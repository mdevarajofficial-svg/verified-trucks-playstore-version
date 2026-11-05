package com.example.data.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class CityHub(
  val name: String,
  val stateCode: String,
  val lat: Double,
  val lng: Double,
)

object GeoAndPrivacyUtils {

  const val SIX_HOURS_MILLIS: Long = 6L * 60L * 60L * 1000L
  const val MAX_MATCH_RADIUS_KM: Double = 250.0

  // Includes requested 8ft, 9ft, 10ft alongside full Indian commercial fleet sizes
  val STANDARD_TRUCK_SIZES_FEET: List<Int> = listOf(8, 9, 10, 14, 17, 20, 22, 24, 32, 40)

  val MAJOR_LOGISTICS_HUBS: List<CityHub> =
    listOf(
      CityHub("Mumbai (Bhiwandi / JNPT)", "MH", 19.0760, 72.8777),
      CityHub("Pune (Chakan MIDC)", "MH", 18.5204, 73.8567),
      CityHub("Nashik (Satpur MIDC)", "MH", 19.9975, 73.7898),
      CityHub("Nagpur (MIHAN / Butibori)", "MH", 21.1458, 79.0882),
      CityHub("Aurangabad (Waluj MIDC)", "MH", 19.8762, 75.3433),
      CityHub("Surat (Hazira)", "GJ", 21.1702, 72.8311),
      CityHub("Ahmedabad (Sanand / Changodar)", "GJ", 23.0225, 72.5714),
      CityHub("Vadodara (Halol)", "GJ", 22.3072, 73.1812),
      CityHub("Rajkot", "GJ", 22.3039, 70.8022),
      CityHub("Mundra Port / Gandhidham", "GJ", 22.8394, 69.7213),
      CityHub("Vapi / Silvassa", "GJ", 20.3893, 72.9106),
      CityHub("Bengaluru (Peenya / Nelamangala)", "KA", 12.9716, 77.5946),
      CityHub("Mysuru", "KA", 12.2958, 76.6394),
      CityHub("Hubballi - Dharwad", "KA", 15.3647, 75.1240),
      CityHub("Mangaluru Port", "KA", 12.9141, 74.8560),
      CityHub("Hosur Industrial Corridor", "TN", 12.7409, 77.8253),
      CityHub("Chennai (Sriperumbudur / Oragadam)", "TN", 13.0827, 80.2707),
      CityHub("Coimbatore", "TN", 11.0168, 76.9558),
      CityHub("Madurai", "TN", 9.9252, 78.1198),
      CityHub("Salem", "TN", 11.6643, 78.1460),
      CityHub("Tiruppur", "TN", 11.1085, 77.3411),
      CityHub("Hyderabad (Shamshabad / Medchal)", "TS", 17.3850, 78.4867),
      CityHub("Warangal", "TS", 17.9689, 79.5941),
      CityHub("Visakhapatnam Port", "AP", 17.6868, 83.2185),
      CityHub("Vijayawada / Guntur", "AP", 16.5062, 80.6480),
      CityHub("Tirupati / Sri City", "AP", 13.6288, 79.4192),
      CityHub("Kochi (Vallarpadam)", "KL", 9.9312, 76.2673),
      CityHub("Kozhikode", "KL", 11.2588, 75.7804),
      CityHub("Delhi NCR (Gurugram / Manesar)", "DL", 28.6139, 77.2090),
      CityHub("Noida / Greater Noida", "UP", 28.5355, 77.3910),
      CityHub("Faridabad", "HR", 28.4089, 77.3178),
      CityHub("Panipat / Sonipat", "HR", 29.3909, 76.9635),
      CityHub("Ludhiana", "PB", 30.9010, 75.8573),
      CityHub("Amritsar / Jalandhar", "PB", 31.6340, 74.8723),
      CityHub("Chandigarh / Baddi", "HP", 30.7333, 76.7794),
      CityHub("Jaipur (Vishwakarma / Sitapura)", "RJ", 26.9124, 75.7873),
      CityHub("Udaipur", "RJ", 24.5854, 73.7125),
      CityHub("Jodhpur", "RJ", 26.2389, 73.0243),
      CityHub("Bhiwadi / Neemrana", "RJ", 28.2088, 76.8446),
      CityHub("Indore (Pithampur)", "MP", 22.7196, 75.8577),
      CityHub("Bhopal", "MP", 23.2599, 77.4126),
      CityHub("Jabalpur", "MP", 23.1815, 79.9864),
      CityHub("Lucknow / Kanpur", "UP", 26.8467, 80.9462),
      CityHub("Varanasi / Prayagraj", "UP", 25.3176, 82.9739),
      CityHub("Raipur / Bhilai", "CG", 21.2514, 81.6296),
      CityHub("Kolkata (Dankuni / Haldia)", "WB", 22.5726, 88.3639),
      CityHub("Siliguri", "WB", 26.7271, 88.3953),
      CityHub("Bhubaneswar / Cuttack", "OD", 20.2961, 85.8245),
      CityHub("Jamshedpur / Ranchi", "JH", 22.8046, 86.2029),
      CityHub("Patna", "BR", 25.5941, 85.1376),
      CityHub("Guwahati", "AS", 26.1445, 91.7362),
    )

  /**
   * Computes great-circle distance in kilometers between two coordinates using the Haversine formula.
   */
  fun haversineDistanceKm(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
  ): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val radLat1 = Math.toRadians(lat1)
    val radLat2 = Math.toRadians(lat2)

    val a =
      sin(dLat / 2.0).pow(2.0) +
        sin(dLon / 2.0).pow(2.0) * cos(radLat1) * cos(radLat2)
    val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
    return earthRadiusKm * c
  }

  /**
   * Strictly formats any phone number to `XXXXXX1234` (6 X characters + last 4 digits)
   * so neither poster nor driver can infer full phone numbers during bidding.
   */
  fun maskPhoneStrict(rawPhone: String): String {
    val digits = rawPhone.filter { it.isDigit() }
    val lastFour =
      when {
        digits.length >= 4 -> digits.takeLast(4)
        digits.isNotEmpty() -> digits.padStart(4, '0')
        else -> "0000"
      }
    return "XXXXXX$lastFour"
  }

  /**
   * Masks Aadhaar input immediately into `[Aadhaar Redacted - XXXX-XXXX-1234]` so raw 12-digit
   * Aadhaar numbers are never persisted in plaintext.
   */
  fun redactAadhaar(rawInput: String): String {
    if (rawInput.startsWith("[Aadhaar Redacted")) return rawInput.take(60)
    val digits = rawInput.filter { it.isDigit() }
    val lastFour = if (digits.length >= 4) digits.takeLast(4) else "REDACTED"
    return "[Aadhaar Redacted - XXXX-XXXX-$lastFour]"
  }

  /**
   * Masks a vehicle registration plate for blind bidding (e.g. `MH-12-AB-8821` -> `MH12XX8821`).
   */
  fun maskVehicleNumber(rawVehicleNumber: String): String {
    val cleaned = rawVehicleNumber.uppercase().filter { it.isLetterOrDigit() }
    if (cleaned.length <= 4) return "XX${cleaned.padStart(4, '0')}"
    val prefix = cleaned.take(4)
    val suffix = cleaned.takeLast(4)
    return "${prefix}XX$suffix"
  }

  /**
   * Generates a deterministic anonymous fleet alias for competing drivers on the blind leaderboard.
   */
  fun anonymousOperatorAlias(driverUid: String): String {
    val hash = (driverUid.hashCode() and 0x7FFFFFFF) % 9000 + 1000
    return "Verified Fleet #$hash"
  }
}
