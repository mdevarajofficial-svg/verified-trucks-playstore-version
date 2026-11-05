package com.example.ui.driver

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DriverTripEntry
import com.example.data.model.FleetVehicle
import com.example.data.model.LoadPost
import com.example.data.model.MatchedLoadItem
import com.example.data.model.UserPrivateKyc
import com.example.data.model.UserRoleProfile
import com.example.data.model.VehicleBodyType
import com.example.data.util.GeoAndPrivacyUtils
import com.example.ui.localization.LocalAppLocalization
import com.example.ui.localization.LocalLocalizationManager
import com.example.ui.localization.SupportedLanguage
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.RoyalBlueSoftBackground
import com.example.ui.viewmodel.LoadFeedFilter
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatRelativeTime(timestamp: Long): String {
  val diffMillis = System.currentTimeMillis() - timestamp
  val diffMinutes = (diffMillis / (1000 * 60)).coerceAtLeast(1)
  return when {
    diffMinutes < 60 -> "$diffMinutes min ago"
    diffMinutes < 1440 -> "${diffMinutes / 60}h ago"
    else -> "${diffMinutes / 1440}d ago"
  }
}

private fun formatPickupDate(timestamp: Long): String {
  return try {
    val sdf = SimpleDateFormat("dd MMM", Locale.ENGLISH)
    sdf.format(Date(timestamp))
  } catch (_: Exception) {
    "Today"
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DriverLiveFeedContent(
  profile: UserRoleProfile,
  filter: LoadFeedFilter,
  matchedLoadsState: UiState<List<MatchedLoadItem>>,
  onUpdateFilter: ((LoadFeedFilter) -> LoadFeedFilter) -> Unit,
  onToggleAvailability: (Boolean) -> Unit,
  onSelectLoad: (LoadPost) -> Unit,
  onOpenCreateLoad: () -> Unit = {},
) {
  val appStrings = LocalAppLocalization.current
  val totalVisibleCount = (matchedLoadsState as? UiState.Success)?.data?.size ?: 0
  val formattedCount = String.format(Locale.getDefault(), "%02d", totalVisibleCount)
  var showFilterModal by remember { mutableStateOf(false) }

  val currentTimeStr = remember {
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
  }

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // 1. Dispatch Board Header (Exact Screenshot Design)
      item {
        Column(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "DISPATCH BOARD  /  $currentTimeStr",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp,
            ),
            color = RoyalBluePrimary,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Move freight\nwith certainty.",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.Black,
                  lineHeight = 32.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = formattedCount,
                style = MaterialTheme.typography.displaySmall.copy(
                  fontWeight = FontWeight.Black,
                ),
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "LIVE LOADS",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = 0.8.sp,
                ),
                color = RoyalBluePrimary,
              )
            }
          }
        }
      }

      // 2. Search & Minimal Filter Button Row (Exact Pill Shape)
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = filter.searchQuery,
            onValueChange = { q -> onUpdateFilter { it.copy(searchQuery = q) } },
            placeholder = {
              Text(
                "Search city, truck or shipper",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              )
            },
            leadingIcon = {
              Icon(
                Icons.Filled.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
            },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface,
              focusedBorderColor = RoyalBluePrimary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            ),
            modifier = Modifier.weight(1f).testTag("driver_feed_search_input"),
          )

          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.size(54.dp).clickable { showFilterModal = true },
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = "Filter",
                tint = RoyalBluePrimary,
                modifier = Modifier.size(22.dp),
              )
            }
          }
        }
      }

      // 3. Section Subheader ("Recommended for your lanes" + "+ Post load")
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "Recommended for your lanes",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Verified shippers · Freshest first",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          TextButton(
            onClick = onOpenCreateLoad,
            colors = ButtonDefaults.textButtonColors(contentColor = RoyalBluePrimary),
            modifier = Modifier.testTag("post_load_header_button"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("+ Post load", fontWeight = FontWeight.ExtraBold)
          }
        }
      }

      // 4. Live Loads List
      when (matchedLoadsState) {
        is UiState.Loading -> {
          item {
            Box(
              modifier = Modifier.fillMaxWidth().padding(40.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(color = RoyalBluePrimary)
            }
          }
        }
        is UiState.Error -> {
          item {
            Card(
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = matchedLoadsState.message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        }
        is UiState.Success -> {
          if (matchedLoadsState.data.isEmpty()) {
            item {
              Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(32.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Icon(
                    imageVector = Icons.Filled.LocalShipping,
                    contentDescription = null,
                    tint = RoyalBluePrimary,
                    modifier = Modifier.size(48.dp),
                  )
                  Spacer(modifier = Modifier.height(12.dp))
                  Text(
                    text = "No Loads Found",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "Loads matching your search or filters will show up automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          } else {
            items(matchedLoadsState.data, key = { it.load.id }) { matchedItem ->
              DriverMatchedLoadCard(
                matchedItem = matchedItem,
                onClick = { onSelectLoad(matchedItem.load) },
                onContactClick = { onSelectLoad(matchedItem.load) },
                onBidClick = { onSelectLoad(matchedItem.load) },
              )
            }
          }
        }
      }
    }
  }

  // Filter Sheet
  if (showFilterModal) {
    ModalBottomSheet(
      onDismissRequest = { showFilterModal = false },
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Filter Loads",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
          )
          TextButton(
            onClick = {
              onUpdateFilter { LoadFeedFilter() }
              showFilterModal = false
            }
          ) {
            Text("Reset All", fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
          }
        }

        Text(
          text = "Body Type",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          VehicleBodyType.entries.forEach { body ->
            FilterChip(
              selected = filter.bodyTypeFilter == body.name,
              onClick = {
                onUpdateFilter {
                  it.copy(bodyTypeFilter = if (it.bodyTypeFilter == body.name) null else body.name)
                }
              },
              label = { Text(if (body == VehicleBodyType.OPEN) "Open" else "Container") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = RoyalBluePrimary,
                selectedLabelColor = Color.White,
              ),
            )
          }
        }

        Text(
          text = "Truck Size (Feet)",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          GeoAndPrivacyUtils.STANDARD_TRUCK_SIZES_FEET.forEach { ft ->
            FilterChip(
              selected = filter.sizeFeetFilter == ft,
              onClick = {
                onUpdateFilter {
                  it.copy(sizeFeetFilter = if (it.sizeFeetFilter == ft) null else ft)
                }
              },
              label = { Text("$ft ft") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = RoyalBluePrimary,
                selectedLabelColor = Color.White,
              ),
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = { showFilterModal = false },
          colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
          shape = RoundedCornerShape(50),
          modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
          Text("Apply Filters", fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }
}

/**
 * Live Load Card (Strict Outside Display & Privacy Rules)
 * - Cities & States: Main City + State Code in parentheses (e.g. Bengaluru (KA) → Mumbai (MH))
 * - DO NOT show poster/shipper name anywhere on the card.
 * - DO NOT show street address on the outer card.
 * - Distance: Simple total distance (e.g. 1,020 km)
 * - Specs: PICKUP (date), VEHICLE (feet), LOAD (tons)
 * - Action buttons: Outlined "Contact" & Bold solid Royal Blue pill "Place a bid ⚡"
 */
@Composable
private fun DriverMatchedLoadCard(
  matchedItem: MatchedLoadItem,
  onClick: () -> Unit,
  onContactClick: () -> Unit,
  onBidClick: () -> Unit,
) {
  val load = matchedItem.load
  val totalKm = load.resolvedRouteDistanceKm()
  val fromDisplay = GeoAndPrivacyUtils.resolveCityWithState(load.originCity)
  val loadTimeMillis = load.createdAt?.toDate()?.time ?: System.currentTimeMillis()
  val relativeTime = formatRelativeTime(loadTimeMillis)
  val pickupDate = formatPickupDate(loadTimeMillis)

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .clickable(onClick = onClick)
      .testTag("driver_matched_load_${load.id}"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Top Row: ● VERIFIED LOAD pill badge + Relative Timestamp
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = RoyalBlueSoftBackground,
          shape = RoundedCornerShape(50),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(RoyalBluePrimary)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "VERIFIED LOAD",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp,
              ),
              color = RoyalBluePrimary,
            )
          }
        }

        Text(
          text = relativeTime,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Cities & States Row: FROM Bengaluru (KA) ➔ TO Mumbai (MH) ...
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          Column {
            Text(
              text = "📍 FROM",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = fromDisplay,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          Spacer(modifier = Modifier.width(12.dp))
          Icon(
            imageVector = Icons.Filled.ArrowForward,
            contentDescription = "to",
            tint = RoyalBluePrimary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = "↕ TO",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = toDisplay,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        Icon(
          imageVector = Icons.Filled.MoreVert,
          contentDescription = "More details",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp),
        )
      }

      // Specs Row: PICKUP, VEHICLE, LOAD
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.CalendarMonth,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "PICKUP",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = pickupDate,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
          )
        }

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.LocalShipping,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "VEHICLE",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${load.requiredSizeFeet} ft ${load.requiredBodyType}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
          )
        }

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Inventory2,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "LOAD",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${load.requiredTonnage} T",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
          )
        }
      }

      // Budget & Distance Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
      ) {
        Column {
          Text(
            text = "BUDGET",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          val displayRate = (load.lowestBidAmount ?: load.expectedBudgetInr ?: 42000.0)
          Text(
            text = "₹${displayRate.toInt()}",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Black,
              fontSize = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "DISTANCE",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "$totalKm km",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      // Buttons Row: Outlined "Contact" & Bold Solid Royal Blue pill "Place a bid ⚡"
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = onContactClick,
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
          ),
          modifier = Modifier.weight(0.42f).height(48.dp).testTag("load_contact_btn_${load.id}"),
        ) {
          Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Contact", fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = onBidClick,
          shape = RoundedCornerShape(50),
          colors = ButtonDefaults.buttonColors(
            containerColor = RoyalBluePrimary,
            contentColor = Color.White,
          ),
          modifier = Modifier.weight(0.58f).height(48.dp).testTag("load_place_bid_btn_${load.id}"),
        ) {
          Text("Place a bid ⚡", fontWeight = FontWeight.Black)
        }
      }
    }
  }
}

/**
 * Driver Won Deals / My Trips
 */
@Composable
fun DriverWonDealsContent(
  tripsState: UiState<List<DriverTripEntry>>,
  onSelectTrip: (DriverTripEntry) -> Unit,
) {
  val context = LocalContext.current

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Text(
          text = "🛣️ My Trips",
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
          color = RoyalBluePrimary,
        )
      }

      when (tripsState) {
        is UiState.Loading -> {
          item {
            Box(
              modifier = Modifier.fillMaxWidth().padding(32.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(color = RoyalBluePrimary)
            }
          }
        }
        is UiState.Error -> {
          item {
            Text(text = tripsState.message, color = MaterialTheme.colorScheme.error)
          }
        }
        is UiState.Success -> {
          if (tripsState.data.isEmpty()) {
            item {
              Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = RoyalBluePrimary,
                    modifier = Modifier.size(42.dp),
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  Text(
                    "No Trips Yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Place a bid on any live load to track its status here. When your bid is accepted, the direct contact number unlocks automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          } else {
            items(tripsState.data, key = { it.id.ifBlank { it.tripId } }) { trip ->
              val isBidAccepted = trip.bidStatus == "ACCEPTED"
              val isBooked = trip.loadStatus == "BOOKED"
              val isCancelled = trip.loadStatus == "CANCELLED"
              val fromDisplay = GeoAndPrivacyUtils.resolveCityWithState(trip.originCity)
              val toDisplay = GeoAndPrivacyUtils.resolveCityWithState(trip.destinationCity)

              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(20.dp))
                  .clickable { onSelectTrip(trip) }
                  .testTag("driver_trip_card_${trip.loadId}"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                  containerColor = when {
                    isBidAccepted -> MaterialTheme.colorScheme.secondaryContainer
                    isCancelled -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                    else -> MaterialTheme.colorScheme.surface
                  }
                ),
                border = BorderStroke(
                  width = 1.5.dp,
                  color = when {
                    isBidAccepted || isBooked -> MaterialTheme.colorScheme.secondary
                    isCancelled -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                  },
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Surface(
                      color = if (isBidAccepted) MaterialTheme.colorScheme.secondary else RoyalBlueSoftBackground,
                      shape = RoundedCornerShape(50),
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Icon(
                          imageVector = if (isBidAccepted) Icons.Filled.LockOpen else Icons.Filled.Verified,
                          contentDescription = null,
                          tint = if (isBidAccepted) Color.White else RoyalBluePrimary,
                          modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                          text = if (isBidAccepted) "BID ACCEPTED" else "BID PLACED",
                          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                          color = if (isBidAccepted) Color.White else RoyalBluePrimary,
                        )
                      }
                    }

                    Surface(
                      color = when (trip.loadStatus) {
                        "BOOKED" -> MaterialTheme.colorScheme.secondaryContainer
                        "CANCELLED" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                      },
                      shape = RoundedCornerShape(50),
                    ) {
                      val loadBadgeText = when (trip.loadStatus) {
                        "BOOKED" -> "BOOKED"
                        "CANCELLED" -> "CANCELLED"
                        "ACCEPTED" -> "ACCEPTED"
                        else -> "LIVE"
                      }
                      Text(
                        text = loadBadgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = when (trip.loadStatus) {
                          "BOOKED" -> MaterialTheme.colorScheme.onSecondaryContainer
                          "CANCELLED" -> MaterialTheme.colorScheme.onErrorContainer
                          else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                      )
                    }
                  }

                  Text(
                    text = "📍 $fromDisplay → 🏁 $toDisplay",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                  )

                  Text(
                    text = "💰 Your Bid: ₹${trip.bidAmount.toInt()} • 🚛 ${trip.requiredSizeFeet}ft ${trip.requiredBodyType}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )

                  // Rule #5: Calling button must contain ONLY the phone number (no names next to it)
                  if (isBidAccepted && !trip.unlockedPosterPhone.isNullOrBlank()) {
                    Button(
                      onClick = {
                        context.startActivity(
                          Intent(Intent.ACTION_DIAL, Uri.parse("tel:${trip.unlockedPosterPhone}"))
                        )
                      },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.White,
                      ),
                      shape = RoundedCornerShape(50),
                      modifier = Modifier.fillMaxWidth().height(48.dp).testTag("trip_call_poster_${trip.loadId}"),
                    ) {
                      Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(trip.unlockedPosterPhone ?: "", fontWeight = FontWeight.ExtraBold)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Profile, Multi-Language Selector & Fleet Vehicle Management Screen
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileAndKycSummaryScreen(
  profile: UserRoleProfile,
  privateKyc: UserPrivateKyc?,
  fleetVehiclesState: List<FleetVehicle> = emptyList(),
  onSaveFleetVehicle: (feet: Int, bodyType: String, locationCity: String, vehiclePlate: String?) -> Unit = { _, _, _, _ -> },
  onEditVehicleOrKyc: () -> Unit,
  onSignOut: () -> Unit,
) {
  val localizationManager = LocalLocalizationManager.current
  val currentLanguage = localizationManager.currentLanguage
  var showLanguageDialog by remember { mutableStateOf(false) }

  // Fleet Vehicle Form State
  var selectedFeet by remember { mutableStateOf(32) }
  var selectedBodyType by remember { mutableStateOf(VehicleBodyType.CONTAINER.name) }
  var vehicleCity by remember { mutableStateOf(profile.currentCity?.ifBlank { "Bengaluru" } ?: "Bengaluru") }
  var truckPlateNumber by remember { mutableStateOf("") }
  var fleetSuccessMessage by remember { mutableStateOf<String?>(null) }

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // 1. Language Selector Card (Rule #7)
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth().clickable { showLanguageDialog = true },
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = RoyalBlueSoftBackground,
                modifier = Modifier.size(40.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = "Language",
                    tint = RoyalBluePrimary,
                    modifier = Modifier.size(22.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "App Language / ಭಾಷೆ / भाषा",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = "${currentLanguage.displayName} (${currentLanguage.nativeName})",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                  color = RoyalBluePrimary,
                )
              }
            }

            Surface(
              color = RoyalBlueSoftBackground,
              shape = RoundedCornerShape(50),
            ) {
              Text(
                text = "Change",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = RoyalBluePrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }
          }
        }
      }

      // 2. Profile Summary Card
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = profile.displayName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
              )
            }

            Surface(
              color = RoyalBlueSoftBackground,
              shape = RoundedCornerShape(50),
            ) {
              Text(
                text = if (profile.role == "POSTER") "🏢 Shipper / Transporter" else "🚛 Vehicle Owner / Driver",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = RoyalBluePrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }

            if (privateKyc != null) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.Call,
                  contentDescription = null,
                  tint = RoyalBluePrimary,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Contact: ${privateKyc.phoneNumber}",
                  style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                )
              }

              if (!privateKyc.drivingLicenseNumber.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.Badge,
                    contentDescription = null,
                    tint = RoyalBluePrimary,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "License: ${privateKyc.drivingLicenseNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              OutlinedButton(
                onClick = onEditVehicleOrKyc,
                enabled = !isBusy,
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1f).testTag("edit_profile_button"),
              ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit Profile", fontWeight = FontWeight.Bold)
              }
              Button(
                onClick = onSignOut,
                enabled = !isBusy,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f).testTag("sign_out_profile_button"),
              ) {
                Text("Sign Out", fontWeight = FontWeight.Bold, color = Color.White)
              }
            }
          }
        }
      }

      // 3. Vehicle Owner Fleet Management Form (Rule #8)
      if (profile.role != "POSTER") {
        item {
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.LocalShipping,
                  contentDescription = null,
                  tint = RoyalBluePrimary,
                  modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "🚛 Add Fleet Vehicle",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                )
              }
              Text(
                text = "Add your vehicles to automatically discover loads within 250 km radius.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              // Select Truck Size (Feet)
              Text(
                text = "Vehicle Size (Feet)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                GeoAndPrivacyUtils.STANDARD_TRUCK_SIZES_FEET.forEach { ft ->
                  FilterChip(
                    selected = selectedFeet == ft,
                    onClick = { selectedFeet = ft },
                    label = { Text("$ft ft", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = RoyalBluePrimary,
                      selectedLabelColor = Color.White,
                    ),
                  )
                }
              }

              // Body Type (Container or Open)
              Text(
                text = "Body Type",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              )
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(VehicleBodyType.CONTAINER.name, VehicleBodyType.OPEN.name).forEach { body ->
                  FilterChip(
                    selected = selectedBodyType == body,
                    onClick = { selectedBodyType = body },
                    label = { Text(if (body == "CONTAINER") "🚛 Container" else "🛻 Open Vehicle", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = RoyalBluePrimary,
                      selectedLabelColor = Color.White,
                    ),
                  )
                }
              }

              // Vehicle Location City
              OutlinedTextField(
                value = vehicleCity,
                onValueChange = { vehicleCity = it },
                label = { Text("Vehicle Current Location (City)") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = RoyalBluePrimary) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("fleet_city_input"),
              )

              // Truck Registration Plate (Optional)
              OutlinedTextField(
                value = truckPlateNumber,
                onValueChange = { truckPlateNumber = it.uppercase() },
                label = { Text("Truck Number (e.g. KA01AB1234 - Optional)") },
                leadingIcon = { Icon(Icons.Filled.DirectionsBus, contentDescription = null, tint = RoyalBluePrimary) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("fleet_plate_input"),
              )

              if (fleetSuccessMessage != null) {
                Surface(
                  color = MaterialTheme.colorScheme.secondaryContainer,
                  shape = RoundedCornerShape(10.dp),
                ) {
                  Text(
                    text = fleetSuccessMessage ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  )
                }
              }

              Button(
                onClick = {
                  if (vehicleCity.isNotBlank()) {
                    onSaveFleetVehicle(
                      selectedFeet,
                      selectedBodyType,
                      vehicleCity.trim(),
                      truckPlateNumber.trim().ifBlank { null },
                    )
                    fleetSuccessMessage = "✅ Vehicle saved! Loads within 250km will match automatically."
                    truckPlateNumber = ""
                  }
                },
                enabled = vehicleCity.isNotBlank() && !isBusy,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_fleet_vehicle_btn"),
              ) {
                Text("Save Vehicle to Fleet", fontWeight = FontWeight.ExtraBold, color = Color.White)
              }

              // Saved Fleet Vehicles List
              val savedVehicles = fleetVehiclesState
              if (savedVehicles.isNotEmpty()) {
                Text(
                  text = "Your Saved Fleet (${savedVehicles.size})",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                )
                savedVehicles.forEachIndexed { idx, veh ->
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Row(
                      modifier = Modifier.padding(12.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Column {
                        Text(
                          text = "🚛 ${veh.sizeFeet} ft ${veh.bodyType}",
                          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                          text = "📍 ${veh.currentCity} ${if (!veh.vehiclePlate.isNullOrBlank()) "• ${veh.vehiclePlate}" else ""}",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                      Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50),
                      ) {
                        Text(
                          text = "250km match ON",
                          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                          color = MaterialTheme.colorScheme.onSecondaryContainer,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Language Selection Dialog
  if (showLanguageDialog) {
    AlertDialog(
      onDismissRequest = { showLanguageDialog = false },
      title = {
        Text(
          text = "Select Language / ಭಾಷೆ ಆರಿಸಿ",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          SupportedLanguage.entries.forEach { lang ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                  localizationManager.setLanguage(lang.code)
                  showLanguageDialog = false
                }
                .padding(vertical = 8.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = currentLanguage == lang,
                onClick = {
                  localizationManager.setLanguage(lang.code)
                  showLanguageDialog = false
                },
                colors = RadioButtonDefaults.colors(selectedColor = RoyalBluePrimary),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = lang.nativeName,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                )
                Text(
                  text = lang.displayName,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showLanguageDialog = false }) {
          Text("Close", fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
        }
      },
    )
  }
}
