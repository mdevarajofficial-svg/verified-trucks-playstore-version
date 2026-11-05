package com.example.data.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.example.BuildConfig
import com.example.data.util.GeoAndPrivacyUtils
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class PlaceLocationResult(
  val placeName: String,
  val placeAddress: String,
  val lat: Double,
  val lng: Double,
  val mapplsPin: String? = null,
  val source: String = "Mappls",
)

data class RouteDistanceResult(
  val totalKilometers: Double,
  val durationMinutes: Int?,
  val isExactRoadRoute: Boolean,
)

object MapplsLocationService {

  private const val PLACEHOLDER_KEY = "YOUR_MAPPLS_REST_API_KEY"

  private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(8, TimeUnit.SECONDS)
      .readTimeout(8, TimeUnit.SECONDS)
      .build()
  }

  fun isMapplsKeyConfigured(): Boolean {
    val key = BuildConfig.MAPPLS_API_KEY.trim()
    return key.isNotEmpty() && key != PLACEHOLDER_KEY
  }

  private fun activeApiKey(): String? {
    val key = BuildConfig.MAPPLS_API_KEY.trim()
    return if (key.isNotEmpty() && key != PLACEHOLDER_KEY) key else null
  }

  /**
   * Searches real locations ONLY when the customer actively types (>= 2 characters).
   * Never returns pre-populated default city suggestions when the box is empty.
   */
  suspend fun searchLocations(
    context: Context,
    rawQuery: String,
  ): List<PlaceLocationResult> = withContext(Dispatchers.IO) {
    val query = rawQuery.trim()
    if (query.length < 2) {
      return@withContext emptyList()
    }

    val results = mutableListOf<PlaceLocationResult>()
    val apiKey = activeApiKey()

    // 1. Live Mappls REST Geocode & Autosuggest Search
    if (apiKey != null) {
      results.addAll(queryMapplsRestSearch(context, apiKey, query))
    }

    // 2. Supplement with Android OS Geocoder when user types a locality, PIN code, or street
    if (results.size < 6) {
      val geocoded = queryAndroidGeocoder(context, "$query, India")
      geocoded.forEach { item ->
        if (
          results.none { existing ->
            GeoAndPrivacyUtils.haversineDistanceKm(existing.lat, existing.lng, item.lat, item.lng) < 1.5
          }
        ) {
          results.add(item)
        }
      }
    }

    results.take(6)
  }

  /**
   * Silently resolves coordinates for a manually typed place name if the customer did not tap a
   * dropdown result, enabling automatic kilometer calculation without showing city suggestion lists.
   */
  suspend fun resolveCoordinatesForTypedPlace(
    context: Context,
    typedText: String,
  ): PlaceLocationResult? = withContext(Dispatchers.IO) {
    val q = typedText.trim()
    if (q.length < 2) return@withContext null

    val live = searchLocations(context, q).firstOrNull()
    if (live != null) return@withContext live

    // Silent offline coordinate lookup for distance math only (never shown as UI city suggestions)
    val matchedHub =
      GeoAndPrivacyUtils.MAJOR_LOGISTICS_HUBS.firstOrNull { hub ->
        val baseCity = hub.name.substringBefore("(").trim()
        q.contains(baseCity, ignoreCase = true) || baseCity.contains(q, ignoreCase = true)
      }
    if (matchedHub != null) {
      return@withContext PlaceLocationResult(
        placeName = q,
        placeAddress = q,
        lat = matchedHub.lat,
        lng = matchedHub.lng,
        source = "Map",
      )
    }
    null
  }

  private suspend fun queryMapplsRestSearch(
    context: Context,
    apiKey: String,
    query: String,
  ): List<PlaceLocationResult> {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val endpoints =
      listOf(
        "https://search.mappls.com/search/places/autosuggest/json?query=$encoded&region=IND&access_token=$apiKey",
        "https://atlas.mappls.com/api/places/search/json?query=$encoded&region=IND&access_token=$apiKey",
        "https://search.mappls.com/search/address/geocode?address=$encoded&region=IND&access_token=$apiKey",
        "https://apis.mappls.com/advancedmaps/v1/$apiKey/geo_code?addr=$encoded",
      )

    for (url in endpoints) {
      try {
        val req =
          Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()
        httpClient.newCall(req).execute().use { resp ->
          if (resp.isSuccessful) {
            val body = resp.body?.string().orEmpty()
            val parsed = parseMapplsPlacesJson(context, body)
            if (parsed.isNotEmpty()) {
              return parsed
            }
          }
        }
      } catch (_: Exception) {
        // Try next Mappls endpoint cleanly
      }
    }
    return emptyList()
  }

  private suspend fun parseMapplsPlacesJson(
    context: Context,
    jsonStr: String,
  ): List<PlaceLocationResult> {
    if (jsonStr.isBlank()) return emptyList()
    val out = mutableListOf<PlaceLocationResult>()
    val root = JSONObject(jsonStr)

    val candidateArrays = mutableListOf<JSONArray>()
    root.optJSONArray("suggestedLocations")?.let { candidateArrays.add(it) }
    root.optJSONArray("results")?.let { candidateArrays.add(it) }
    val cop = root.opt("copResults")
    when (cop) {
      is JSONArray -> candidateArrays.add(cop)
      is JSONObject -> candidateArrays.add(JSONArray().put(cop))
    }

    for (arr in candidateArrays) {
      for (i in 0 until minOf(arr.length(), 6)) {
        val obj = arr.optJSONObject(i) ?: continue
        val name =
          obj.optString("placeName")
            .ifBlank { obj.optString("poi") }
            .ifBlank { obj.optString("locality") }
            .ifBlank { obj.optString("city") }
            .ifBlank { obj.optString("formattedAddress").substringBefore(",") }
            .trim()
        val address =
          obj.optString("placeAddress")
            .ifBlank { obj.optString("formattedAddress") }
            .ifBlank { name }
            .trim()
        val pin = obj.optString("eLoc").ifBlank { obj.optString("mapplsPin") }.ifBlank { null }

        var lat = obj.optDouble("latitude", Double.NaN)
        if (lat.isNaN()) lat = obj.optDouble("lat", Double.NaN)
        if (lat.isNaN()) lat = obj.optDouble("entryLatitude", Double.NaN)

        var lng = obj.optDouble("longitude", Double.NaN)
        if (lng.isNaN()) lng = obj.optDouble("lng", Double.NaN)
        if (lng.isNaN()) lng = obj.optDouble("entryLongitude", Double.NaN)

        if ((lat.isNaN() || lng.isNaN()) && address.isNotBlank()) {
          val resolved = queryAndroidGeocoder(context, "$name, $address").firstOrNull()
          if (resolved != null) {
            lat = resolved.lat
            lng = resolved.lng
          }
        }

        if (!lat.isNaN() && !lng.isNaN() && name.isNotBlank()) {
          out.add(
            PlaceLocationResult(
              placeName = name,
              placeAddress = address,
              lat = lat,
              lng = lng,
              mapplsPin = pin,
              source = "Mappls",
            )
          )
        }
      }
      if (out.isNotEmpty()) break
    }
    return out
  }

  private suspend fun queryAndroidGeocoder(
    context: Context,
    query: String,
  ): List<PlaceLocationResult> {
    if (!Geocoder.isPresent()) return emptyList()
    val geocoder = Geocoder(context, Locale.forLanguageTag("en-IN"))
    return try {
      val addresses =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          suspendCancellableCoroutine { cont ->
            geocoder.getFromLocationName(query, 5) { list ->
              if (cont.isActive) cont.resume(list)
            }
          }
        } else {
          @Suppress("DEPRECATION")
          geocoder.getFromLocationName(query, 5).orEmpty()
        }

      addresses.mapNotNull { addr ->
        val lat = addr.latitude
        val lng = addr.longitude
        val locality =
          addr.subLocality
            ?: addr.locality
            ?: addr.subAdminArea
            ?: addr.featureName
            ?: query
        val state = addr.adminArea ?: ""
        val title =
          if (state.isNotBlank() && !locality.contains(state, ignoreCase = true)) {
            "$locality, $state"
          } else {
            locality
          }
        val fullLine =
          (0..addr.maxAddressLineIndex)
            .mapNotNull { idx -> addr.getAddressLine(idx) }
            .joinToString(", ")
            .ifBlank { title }
        PlaceLocationResult(
          placeName = title.take(90),
          placeAddress = fullLine.take(240),
          lat = lat,
          lng = lng,
          source = "Map",
        )
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  /**
   * Automatically calculates total road driving kilometers between Origin and Destination
   * using Mappls Advanced Maps Driving Route API (`route_adv/driving`), falling back to
   * highway-calibrated coordinate distance if offline or before secret injection.
   */
  suspend fun calculateTotalRouteKilometers(
    originLat: Double,
    originLng: Double,
    destLat: Double,
    destLng: Double,
  ): RouteDistanceResult = withContext(Dispatchers.IO) {
    val straightKm = GeoAndPrivacyUtils.haversineDistanceKm(originLat, originLng, destLat, destLng)
    if (straightKm < 0.2) {
      return@withContext RouteDistanceResult(
        totalKilometers = 1.0,
        durationMinutes = 5,
        isExactRoadRoute = false,
      )
    }

    val apiKey = activeApiKey()
    if (apiKey != null) {
      try {
        val routeUrl =
          "https://apis.mappls.com/advancedmaps/v1/$apiKey/route_adv/driving/$originLng,$originLat;$destLng,$destLat?geometries=polyline&overview=simplified"
        val req =
          Request.Builder()
            .url(routeUrl)
            .header("Accept", "application/json")
            .build()
        httpClient.newCall(req).execute().use { resp ->
          if (resp.isSuccessful) {
            val body = resp.body?.string().orEmpty()
            val root = JSONObject(body)
            val routes = root.optJSONArray("routes")
            val firstRoute = routes?.optJSONObject(0)
            val distanceMeters = firstRoute?.optDouble("distance", Double.NaN) ?: Double.NaN
            val durationSeconds = firstRoute?.optDouble("duration", Double.NaN) ?: Double.NaN
            if (!distanceMeters.isNaN() && distanceMeters > 0.0) {
              val km = ((distanceMeters / 1000.0) * 10.0).roundToInt() / 10.0
              val mins =
                if (!durationSeconds.isNaN() && durationSeconds > 0) {
                  (durationSeconds / 60.0).roundToInt()
                } else {
                  null
                }
              return@withContext RouteDistanceResult(
                totalKilometers = km.coerceAtLeast(1.0),
                durationMinutes = mins,
                isExactRoadRoute = true,
              )
            }
          }
        }
      } catch (_: Exception) {
        // Fall through to coordinate road distance calculation
      }
    }

    val estimatedRoadKm = ((straightKm * 1.21) * 10.0).roundToInt() / 10.0
    val estimatedMins = ((estimatedRoadKm / 48.0) * 60.0).roundToInt()
    RouteDistanceResult(
      totalKilometers = estimatedRoadKm.coerceAtLeast(1.0),
      durationMinutes = estimatedMins,
      isExactRoadRoute = false,
    )
  }
}
