package com.example.ui.poster

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.location.MapplsLocationService
import com.example.data.model.LoadPost
import com.example.data.model.UserPrivateKyc
import com.example.data.model.UserRoleProfile
import com.example.data.model.VehicleBodyType
import com.example.data.util.GeoAndPrivacyUtils
import com.example.ui.components.MapplsLocationSearchField
import com.example.ui.theme.VerifiedTrucksBrushes
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay

@Composable
fun PosterDashboardContent(
  profile: UserRoleProfile,
  loadsState: UiState<List<LoadPost>>,
  isBusy: Boolean = false,
  onOpenCreateLoad: () -> Unit,
  onSelectLoad: (LoadPost) -> Unit,
  onMarkLoadStatus: (LoadPost, String) -> Unit = { _, _ -> },
) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        PosterSummaryBanner(
          companyName = profile.displayName,
          loadsState = loadsState,
          onOpenCreateLoad = onOpenCreateLoad,
        )
      }

      when (loadsState) {
        is UiState.Loading -> {
          item {
            Box(
              modifier = Modifier.fillMaxWidth().padding(32.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator()
            }
          }
        }
        is UiState.Error -> {
          item {
            Text(
              text = loadsState.message,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
        is UiState.Success -> {
          if (loadsState.data.isEmpty()) {
            item {
              EmptyPosterLoadsCard(onOpenCreateLoad = onOpenCreateLoad)
            }
          } else {
            items(loadsState.data, key = { it.id }) { load ->
              PosterLoadItemCard(
                load = load,
                isBusy = isBusy,
                onClick = { onSelectLoad(load) },
                onMarkLoadStatus = { status -> onMarkLoadStatus(load, status) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PosterSummaryBanner(
  companyName: String,
  loadsState: UiState<List<LoadPost>>,
  onOpenCreateLoad: () -> Unit,
) {
  val loads = (loadsState as? UiState.Success)?.data ?: emptyList()
  val now = System.currentTimeMillis()
  val activeCount =
    loads.count { it.status in listOf("ACTIVE", "ACCEPTED") && !it.isExpired(now) }
  val bookedCount = loads.count { it.status in listOf("ACCEPTED", "BOOKED") }

  Card(
    shape = RoundedCornerShape(24.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(VerifiedTrucksBrushes.ExecutiveHeroGradient)
          .padding(20.dp)
    ) {
      Column {
        Text(
          text = "🏢 $companyName",
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
          color = Color.White,
        )
        Text(
          text = "🇮🇳 Pan-India Dispatch Console • Book verified trucks fast.",
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.88f),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          MetricPill(
            label = "📡 Live Loads",
            value = activeCount.toString(),
            modifier = Modifier.weight(1f),
          )
          MetricPill(
            label = "✅ Booked / Accepted",
            value = bookedCount.toString(),
            modifier = Modifier.weight(1f),
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onOpenCreateLoad,
          shape = RoundedCornerShape(14.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.secondary,
              contentColor = Color.White,
            ),
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("open_post_load_sheet_button"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("🚛 Post New Load", fontWeight = FontWeight.ExtraBold)
        }
      }
    }
  }
}

@Composable
private fun MetricPill(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = Color.White.copy(alpha = 0.16f),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        color = Color.White,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.9f),
      )
    }
  }
}

@Composable
private fun EmptyPosterLoadsCard(onOpenCreateLoad: () -> Unit) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = Icons.Filled.Inventory2,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(46.dp),
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "📦 No Loads Posted Yet",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Enter your loading and unloading locations to start receiving bids from verified trucks.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(16.dp))
      OutlinedButton(
        onClick = onOpenCreateLoad,
        modifier = Modifier.testTag("empty_state_post_load_button"),
      ) {
        Text("🚛 Find Truck Now", fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun PosterLoadItemCard(
  load: LoadPost,
  isBusy: Boolean,
  onClick: () -> Unit,
  onMarkLoadStatus: (String) -> Unit,
) {
  val isAccepted = load.status == "ACCEPTED"
  val isBooked = load.status == "BOOKED"
  val isCancelled = load.status == "CANCELLED"
  val closed = load.isExpired()
  val acceptedCount = load.acceptedCount()
  val routeKm = load.resolvedRouteDistanceKm()

  Card(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onClick)
        .testTag("poster_load_card_${load.id}"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    border =
      BorderStroke(
        width = 1.5.dp,
        color =
          when {
            isBooked || isAccepted -> MaterialTheme.colorScheme.secondary
            isCancelled -> MaterialTheme.colorScheme.error
            closed -> MaterialTheme.colorScheme.outline
            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
          },
      ),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color =
            when {
              isBooked || isAccepted -> MaterialTheme.colorScheme.secondaryContainer
              isCancelled -> MaterialTheme.colorScheme.errorContainer
              closed -> MaterialTheme.colorScheme.surfaceVariant
              else -> MaterialTheme.colorScheme.primaryContainer
            },
          shape = RoundedCornerShape(50),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector =
                when {
                  isBooked || isAccepted -> Icons.Filled.CheckCircle
                  isCancelled -> Icons.Filled.Cancel
                  else -> Icons.Filled.RadioButtonChecked
                },
              contentDescription = null,
              tint =
                when {
                  isBooked || isAccepted -> MaterialTheme.colorScheme.secondary
                  isCancelled -> MaterialTheme.colorScheme.error
                  else -> MaterialTheme.colorScheme.primary
                },
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text =
                when {
                  isBooked -> "📦 BOOKED"
                  isCancelled -> "❌ CANCELLED"
                  isAccepted -> "✅ $acceptedCount ACCEPTED"
                  closed -> "CLOSED"
                  else -> "⚡ LIVE"
                },
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
              color =
                when {
                  isBooked || isAccepted -> MaterialTheme.colorScheme.onSecondaryContainer
                  isCancelled -> MaterialTheme.colorScheme.onErrorContainer
                  else -> MaterialTheme.colorScheme.primary
                },
            )
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(50),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.Route,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "🛣️ $routeKm km",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      val fromDisplay = GeoAndPrivacyUtils.resolveCityWithState(load.originCity)
      val toDisplay = GeoAndPrivacyUtils.resolveCityWithState(load.destinationCity)
      Text(
        text = "📍 $fromDisplay  →  🏁 $toDisplay",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "🚛 ${load.requiredSizeFeet} ft ${load.requiredBodyType} • ⚖️ ${load.requiredTonnage}T • 📦 ${load.materialType}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.Gavel,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text =
              if (load.bidCount > 0 && load.lowestBidAmount != null) {
                "💰 ${load.bidCount} Bids • Lowest: ₹${load.lowestBidAmount.toInt()}"
              } else {
                "⏳ Awaiting bids"
              },
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
          )
        }

        Text(
          text =
            when {
              isBooked -> "📦 Booked • View →"
              isCancelled -> "❌ Cancelled • View →"
              isAccepted -> "✅ Contacts ($acceptedCount) →"
              else -> "🏆 My Bids →"
            },
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
          color = MaterialTheme.colorScheme.secondary,
        )
      }

      // Quick Booked / Cancelled actions right inside My Loads / My Bids
      if (load.status in listOf("ACTIVE", "ACCEPTED") && !closed) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = { onMarkLoadStatus("BOOKED") },
            enabled = !isBusy,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
              ),
            shape = RoundedCornerShape(10.dp),
            modifier =
              Modifier.weight(1f)
                .height(48.dp)
                .testTag("card_mark_booked_${load.id}"),
          ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("Booked", fontWeight = FontWeight.Bold)
          }
          OutlinedButton(
            onClick = { onMarkLoadStatus("CANCELLED") },
            enabled = !isBusy,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error),
            colors =
              ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
              ),
            shape = RoundedCornerShape(10.dp),
            modifier =
              Modifier.weight(1f)
                .height(48.dp)
                .testTag("card_mark_cancelled_${load.id}"),
          ) {
            Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("Cancelled", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateLoadBottomSheet(
  profile: UserRoleProfile,
  privateKyc: UserPrivateKyc?,
  isBusy: Boolean,
  onSubmitLoad: (
    companyName: String,
    directPhone: String,
    originCity: String,
    originAddress: String,
    originLat: Double,
    originLng: Double,
    destCity: String,
    destAddress: String,
    destLat: Double,
    destLng: Double,
    totalDistanceKm: Double,
    sizeFeet: Int,
    tonnage: Double,
    bodyType: String,
    materialType: String,
  ) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  // Start completely EMPTY — let the customer search on their own
  var originName by remember { mutableStateOf("") }
  var originAddress by remember { mutableStateOf("") }
  var originLat by remember { mutableDoubleStateOf(Double.NaN) }
  var originLng by remember { mutableDoubleStateOf(Double.NaN) }

  var destName by remember { mutableStateOf("") }
  var destAddress by remember { mutableStateOf("") }
  var destLat by remember { mutableDoubleStateOf(Double.NaN) }
  var destLng by remember { mutableDoubleStateOf(Double.NaN) }

  var calculatedKm by remember { mutableDoubleStateOf(0.0) }
  var isCalculatingDistance by remember { mutableStateOf(false) }

  // Resolve coordinates silently if customer typed manually without tapping a dropdown item
  LaunchedEffect(originName) {
    if (originName.trim().length >= 2 && (originLat.isNaN() || originLng.isNaN())) {
      delay(450)
      val resolved = MapplsLocationService.resolveCoordinatesForTypedPlace(context, originName)
      if (resolved != null && originLat.isNaN()) {
        originLat = resolved.lat
        originLng = resolved.lng
        if (originAddress.isBlank()) originAddress = resolved.placeAddress
      }
    }
  }

  LaunchedEffect(destName) {
    if (destName.trim().length >= 2 && (destLat.isNaN() || destLng.isNaN())) {
      delay(450)
      val resolved = MapplsLocationService.resolveCoordinatesForTypedPlace(context, destName)
      if (resolved != null && destLat.isNaN()) {
        destLat = resolved.lat
        destLng = resolved.lng
        if (destAddress.isBlank()) destAddress = resolved.placeAddress
      }
    }
  }

  // Auto-calculate kilometers as soon as both coordinates are available
  LaunchedEffect(originLat, originLng, destLat, destLng) {
    if (!originLat.isNaN() && !originLng.isNaN() && !destLat.isNaN() && !destLng.isNaN()) {
      isCalculatingDistance = true
      val distResult =
        MapplsLocationService.calculateTotalRouteKilometers(
          originLat = originLat,
          originLng = originLng,
          destLat = destLat,
          destLng = destLng,
        )
      calculatedKm = distResult.totalKilometers
      isCalculatingDistance = false
    } else {
      calculatedKm = 0.0
    }
  }

  var directPhone by remember { mutableStateOf(privateKyc?.phoneNumber ?: "") }

  val sizes = GeoAndPrivacyUtils.STANDARD_TRUCK_SIZES_FEET
  var selectedSizeFeet by remember { mutableIntStateOf(32) }
  var tonnageText by remember { mutableStateOf("") }
  var selectedBodyType by remember { mutableStateOf(VehicleBodyType.CONTAINER.name) }
  // Start empty — let customer fill Material Type manually
  var materialType by remember { mutableStateOf("") }

  val parsedTonnage = tonnageText.toDoubleOrNull() ?: 0.0
  val phoneValid = directPhone.count { it.isDigit() } in 10..13
  val formValid =
    originName.trim().length >= 2 &&
      destName.trim().length >= 2 &&
      phoneValid &&
      materialType.trim().length >= 2 &&
      parsedTonnage in 0.5..100.0

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .navigationBarsPadding()
          .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = "🚛 Post Load",
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
        color = MaterialTheme.colorScheme.primary,
      )

      // Loading Location (Empty box, simple heading, zero pre-filled cities)
      MapplsLocationSearchField(
        heading = "📍 Loading Location",
        value = originName,
        testTag = "origin_mappls_search_field",
        onValueChange = { typed ->
          originName = typed
          originAddress = typed
          originLat = Double.NaN
          originLng = Double.NaN
        },
        onLocationSelected = { place ->
          originName = place.placeName
          originAddress = place.placeAddress
          originLat = place.lat
          originLng = place.lng
        },
      )

      // Unloading Location (Empty box, simple heading, zero pre-filled cities)
      MapplsLocationSearchField(
        heading = "🏁 Unloading Location",
        value = destName,
        testTag = "dest_mappls_search_field",
        onValueChange = { typed ->
          destName = typed
          destAddress = typed
          destLat = Double.NaN
          destLng = Double.NaN
        },
        onLocationSelected = { place ->
          destName = place.placeName
          destAddress = place.placeAddress
          destLat = place.lat
          destLng = place.lng
        },
      )

      // Distance Section: JUST Kilometer value, zero explanation text
      if (calculatedKm > 0.0 || isCalculatingDistance) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.secondaryContainer,
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary),
          modifier = Modifier.fillMaxWidth().testTag("auto_calculated_km_banner"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (isCalculatingDistance) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.secondary,
                strokeWidth = 2.dp,
              )
            } else {
              Icon(
                imageVector = Icons.Filled.Route,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "🛣️ ${calculatedKm.toInt()} km",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }
        }
      }

      // Vehicle Size (Includes 8 ft, 9 ft, 10 ft + commercial sizes)
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.LocalShipping,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "🚛 Truck Size (Feet)",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
      }
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        sizes.forEach { ft ->
          FilterChip(
            selected = selectedSizeFeet == ft,
            onClick = { selectedSizeFeet = ft },
            label = { Text("$ft ft", fontWeight = FontWeight.Bold) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = Color.White,
              ),
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        VehicleBodyType.entries.forEach { body ->
          FilterChip(
            selected = selectedBodyType == body.name,
            onClick = { selectedBodyType = body.name },
            label = {
              Text(
                if (body == VehicleBodyType.OPEN) "🛻 ${body.label}" else "🚛 ${body.label}",
                fontWeight = FontWeight.Bold,
              )
            },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                selectedLabelColor = Color.White,
              ),
            modifier = Modifier.weight(1f),
          )
        }
      }

      OutlinedTextField(
        value = tonnageText,
        onValueChange = { tonnageText = it.take(6) },
        label = { Text("⚖️ Weight (Tons)") },
        leadingIcon = {
          Icon(Icons.Filled.Scale, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("load_tonnage_input"),
      )

      OutlinedTextField(
        value = materialType,
        onValueChange = { materialType = it.take(80) },
        label = { Text("📦 Material Type") },
        leadingIcon = {
          Icon(Icons.Filled.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("load_material_input"),
      )

      OutlinedTextField(
        value = directPhone,
        onValueChange = { directPhone = it.take(15) },
        label = { Text("📞 Contact Number") },
        leadingIcon = {
          Icon(Icons.Filled.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("load_phone_input"),
      )

      Button(
        onClick = {
          val safeOriginLat = if (originLat.isNaN()) 19.0760 else originLat
          val safeOriginLng = if (originLng.isNaN()) 72.8777 else originLng
          val safeDestLat = if (destLat.isNaN()) 18.5204 else destLat
          val safeDestLng = if (destLng.isNaN()) 73.8567 else destLng
          val safeKm = if (calculatedKm > 0.0) calculatedKm else 100.0

          onSubmitLoad(
            profile.displayName,
            directPhone,
            originName.trim(),
            originAddress.trim().ifBlank { originName.trim() },
            safeOriginLat,
            safeOriginLng,
            destName.trim(),
            destAddress.trim().ifBlank { destName.trim() },
            safeDestLat,
            safeDestLng,
            safeKm,
            selectedSizeFeet,
            parsedTonnage,
            selectedBodyType,
            materialType.trim(),
          )
        },
        enabled = formValid && !isBusy,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
          ),
        modifier =
          Modifier.fillMaxWidth()
            .height(56.dp)
            .testTag("publish_load_submit_button"),
        shape = RoundedCornerShape(16.dp),
      ) {
        if (isBusy) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Color.White,
            strokeWidth = 2.dp,
          )
        } else {
          Text(
            text = "Find Truck Now",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
