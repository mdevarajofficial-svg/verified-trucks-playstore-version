package com.example.ui.driver

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocalAppLocalization
import com.example.ui.localization.LocalLocalizationManager
import com.example.ui.localization.SupportedLanguage
import com.example.ui.theme.EmeraldGreenAccent
import com.example.ui.theme.EmeraldGreenContainer
import com.example.ui.theme.RoyalBlueContainer
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.RoyalBlueSoftBackground
import com.example.ui.viewmodel.LoadFeedFilter
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 10+ Major Indian Transport States requested for the filter dropdown
data class StateOption(val code: String?, val name: String)

val TOP_STATES_FILTER = listOf(
  StateOption(null, "All States"),
  StateOption("KA", "Karnataka"),
  StateOption("TN", "Tamil Nadu"),
  StateOption("TS", "Telangana"),
  StateOption("AP", "Andhra Pradesh"),
  StateOption("KL", "Kerala"),
  StateOption("MH", "Maharashtra"),
  StateOption("MP", "Madhya Pradesh"),
  StateOption("OD", "Odisha"),
  StateOption("RJ", "Rajasthan"),
  StateOption("UP", "Uttar Pradesh"),
  StateOption("WB", "West Bengal"),
  StateOption("GJ", "Gujarat"),
  StateOption("PB", "Punjab"),
  StateOption("HR", "Haryana"),
  StateOption("DL", "Delhi NCR"),
  StateOption("BR", "Bihar"),
  StateOption("JH", "Jharkhand"),
  StateOption("CG", "Chhattisgarh"),
  StateOption("AS", "Assam"),
  StateOption("UK", "Uttarakhand"),
  StateOption("HP", "Himachal Pradesh"),
  StateOption("GA", "Goa"),
)

val FEET_FILTER_OPTIONS = listOf(null, 7, 8, 9, 10, 14, 17, 19, 20, 22, 24, 32, 40)

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

/**
 * Real working market style green pulsing object
 */
@Composable
private fun LiveMarketRadarBeacon(modifier: Modifier = Modifier) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.35f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "radar_scale",
  )
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.9f,
    targetValue = 0.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "radar_alpha",
  )

  Box(contentAlignment = Alignment.Center, modifier = modifier.size(24.dp)) {
    Box(
      modifier = Modifier
        .size(22.dp)
        .scale(pulseScale)
        .clip(CircleShape)
        .background(EmeraldGreenAccent.copy(alpha = pulseAlpha))
    )
    Box(
      modifier = Modifier
        .size(10.dp)
        .clip(CircleShape)
        .background(EmeraldGreenAccent)
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
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

  var stateMenuExpanded by remember { mutableStateOf(false) }
  var feetMenuExpanded by remember { mutableStateOf(false) }

  val currentStateName = TOP_STATES_FILTER.find { it.code == filter.stateCodeFilter }?.name ?: appStrings.allStates
  val currentFeetName = if (filter.sizeFeetFilter != null) "${filter.sizeFeetFilter} ft" else appStrings.allFeet

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1. Futuristic Top Section: "Live Loads" with real working market green beacon
      item {
        Column(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              LiveMarketRadarBeacon()
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = appStrings.liveLoads,
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = (-0.5).sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Surface(
                color = EmeraldGreenContainer,
                shape = RoundedCornerShape(50),
              ) {
                Text(
                  text = "$formattedCount LIVE",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                  color = EmeraldGreenAccent,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }

            Button(
              onClick = onOpenCreateLoad,
              colors = ButtonDefaults.buttonColors(
                containerColor = RoyalBluePrimary,
                contentColor = Color.White,
              ),
              shape = RoundedCornerShape(50),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
              modifier = Modifier.testTag("post_load_header_button"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(appStrings.postLoad, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        }
      }

      // 2. Compact Futuristic Filter Bar: "Location" search + State Dropdown + Feet Dropdown
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            // Search Bar: Limited to simple "Search Location"
            OutlinedTextField(
              value = filter.searchQuery,
              onValueChange = { q -> onUpdateFilter { it.copy(searchQuery = q) } },
              placeholder = {
                Text(
                  appStrings.searchLocation,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
              },
              leadingIcon = {
                Icon(
                  Icons.Filled.LocationOn,
                  contentDescription = "Location",
                  tint = RoyalBluePrimary,
                  modifier = Modifier.size(20.dp),
                )
              },
              singleLine = true,
              shape = RoundedCornerShape(50),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = RoyalBluePrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
              ),
              modifier = Modifier.fillMaxWidth().testTag("driver_feed_search_input"),
            )

            // Compact Dropdown Filters Row: States (10 top states) and Truck Feet
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              // State Dropdown
              Box(modifier = Modifier.weight(1f)) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = if (filter.stateCodeFilter != null) RoyalBlueSoftBackground else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  border = BorderStroke(
                    1.dp,
                    if (filter.stateCodeFilter != null) RoyalBluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { stateMenuExpanded = true }
                    .padding(vertical = 2.dp)
                    .testTag("state_dropdown_trigger"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = currentStateName,
                      style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (filter.stateCodeFilter != null) FontWeight.ExtraBold else FontWeight.Medium,
                      ),
                      color = if (filter.stateCodeFilter != null) RoyalBluePrimary else MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                    )
                    Icon(
                      imageVector = Icons.Filled.ArrowDropDown,
                      contentDescription = "Select State",
                      tint = if (filter.stateCodeFilter != null) RoyalBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }

                DropdownMenu(
                  expanded = stateMenuExpanded,
                  onDismissRequest = { stateMenuExpanded = false },
                ) {
                  TOP_STATES_FILTER.forEach { stateOpt ->
                    DropdownMenuItem(
                      text = {
                        Text(
                          text = if (stateOpt.code == null) appStrings.allStates else stateOpt.name,
                          fontWeight = if (filter.stateCodeFilter == stateOpt.code) FontWeight.ExtraBold else FontWeight.Normal,
                          color = if (filter.stateCodeFilter == stateOpt.code) RoyalBluePrimary else MaterialTheme.colorScheme.onSurface,
                        )
                      },
                      onClick = {
                        onUpdateFilter { it.copy(stateCodeFilter = stateOpt.code) }
                        stateMenuExpanded = false
                      },
                    )
                  }
                }
              }

              // Feet Dropdown
              Box(modifier = Modifier.weight(0.7f)) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = if (filter.sizeFeetFilter != null) RoyalBlueSoftBackground else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  border = BorderStroke(
                    1.dp,
                    if (filter.sizeFeetFilter != null) RoyalBluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { feetMenuExpanded = true }
                    .padding(vertical = 2.dp)
                    .testTag("feet_dropdown_trigger"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = currentFeetName,
                      style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (filter.sizeFeetFilter != null) FontWeight.ExtraBold else FontWeight.Medium,
                      ),
                      color = if (filter.sizeFeetFilter != null) RoyalBluePrimary else MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                    )
                    Icon(
                      imageVector = Icons.Filled.ArrowDropDown,
                      contentDescription = "Select Feet",
                      tint = if (filter.sizeFeetFilter != null) RoyalBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }

                DropdownMenu(
                  expanded = feetMenuExpanded,
                  onDismissRequest = { feetMenuExpanded = false },
                ) {
                  FEET_FILTER_OPTIONS.forEach { ft ->
                    DropdownMenuItem(
                      text = {
                        Text(
                          text = if (ft != null) "$ft ft" else appStrings.allFeet,
                          fontWeight = if (filter.sizeFeetFilter == ft) FontWeight.ExtraBold else FontWeight.Normal,
                          color = if (filter.sizeFeetFilter == ft) RoyalBluePrimary else MaterialTheme.colorScheme.onSurface,
                        )
                      },
                      onClick = {
                        onUpdateFilter { it.copy(sizeFeetFilter = ft) }
                        feetMenuExpanded = false
                      },
                    )
                  }
                }
              }

              if (filter.stateCodeFilter != null || filter.sizeFeetFilter != null || filter.searchQuery.isNotBlank()) {
                TextButton(
                  onClick = { onUpdateFilter { LoadFeedFilter() } },
                  contentPadding = PaddingValues(horizontal = 6.dp),
                ) {
                  Text("Reset", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        }
      }

      // 3. Futuristic Live Loads Cards
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
                    text = appStrings.noLoadsFound,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = appStrings.noLoadsSub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          } else {
            items(matchedLoadsState.data, key = { it.load.id }) { matchedItem ->
              FuturisticDriverLoadCard(
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
}

/**
 * Futuristic Style Load Post Card:
 * - NO names of who posted the load or who placed the bid.
 * - Cities & States: Main City + State Code in parentheses (e.g. Bengaluru (KA) → Mumbai (MH)).
 * - Distance: Simple total distance (e.g. 1,020 km).
 * - Specs: PICKUP date, VEHICLE (feet), LOAD (tons).
 * - Cyber glowing tag, sharp contrast, futuristic royal blue and emerald accents.
 * - Action buttons: Outlined "Contact" & Bold solid Royal Blue pill "Place a bid ⚡".
 */
@Composable
private fun FuturisticDriverLoadCard(
  matchedItem: MatchedLoadItem,
  onClick: () -> Unit,
  onContactClick: () -> Unit,
  onBidClick: () -> Unit,
) {
  val appStrings = LocalAppLocalization.current
  val load = matchedItem.load
  val totalKm = load.resolvedRouteDistanceKm()
  val fromDisplay = GeoAndPrivacyUtils.cleanCityNameOnly(load.originCity)
  val toDisplay = GeoAndPrivacyUtils.cleanCityNameOnly(load.destinationCity)
  val loadTimeMillis = load.createdAt?.toDate()?.time ?: System.currentTimeMillis()
  val relativeTime = formatRelativeTime(loadTimeMillis)
  val pickupDate = formatPickupDate(loadTimeMillis)

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .clickable(onClick = onClick)
      .testTag("driver_matched_load_${load.id}"),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    border = BorderStroke(1.2.dp, RoyalBluePrimary.copy(alpha = 0.25f)),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Top Row: Futuristic Cyber Pill Badge + Relative Timestamp
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = RoyalBlueSoftBackground,
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.3f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(EmeraldGreenAccent)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = appStrings.verifiedLoad,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
              ),
              color = RoyalBluePrimary,
            )
          }
        }

        Text(
          text = relativeTime,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Cities Route without any state initials (NO poster/shipper name anywhere)
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
              text = appStrings.from,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            Text(
              text = fromDisplay,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          Spacer(modifier = Modifier.width(12.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "to",
            tint = RoyalBluePrimary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = appStrings.to,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            Text(
              text = toDisplay,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
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
              text = appStrings.pickup,
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
              text = appStrings.vehicle,
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
              text = appStrings.load,
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
            text = appStrings.budget,
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
            color = RoyalBluePrimary,
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = appStrings.distance,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = onContactClick,
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
          ),
          modifier = Modifier.weight(0.42f).height(46.dp).testTag("load_contact_btn_${load.id}"),
        ) {
          Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(appStrings.contact, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = onBidClick,
          shape = RoundedCornerShape(50),
          colors = ButtonDefaults.buttonColors(
            containerColor = RoyalBluePrimary,
            contentColor = Color.White,
          ),
          modifier = Modifier.weight(0.58f).height(46.dp).testTag("load_place_bid_btn_${load.id}"),
        ) {
          Text(appStrings.placeABid, fontWeight = FontWeight.Black)
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
              val fromDisplay = GeoAndPrivacyUtils.cleanCityNameOnly(trip.originCity)
              val toDisplay = GeoAndPrivacyUtils.cleanCityNameOnly(trip.destinationCity)

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

                  // Rule: Calling button must contain ONLY the phone number with exactly ONE phone icon/emoji
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
 * Profile Screen with Language Selection Dropdown Component at the Top (Rule #1)
 * Supports English, Kannada, Telugu, Tamil, Hindi, Malayalam, and Marathi with dynamic UI update.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileAndKycSummaryScreen(
  profile: UserRoleProfile,
  privateKyc: UserPrivateKyc?,
  isBusy: Boolean = false,
  fleetVehiclesState: List<FleetVehicle> = emptyList(),
  onSaveFleetVehicle: (feet: Int, bodyType: String, locationCity: String) -> Unit = { _, _, _ -> },
  onEditVehicleOrKyc: () -> Unit,
  onSignOut: () -> Unit,
) {
  val localizationManager = LocalLocalizationManager.current
  val currentLanguage = localizationManager.currentLanguage
  var languageDropdownExpanded by remember { mutableStateOf(false) }

  // Fleet Vehicle Form State
  var selectedFeet by remember { mutableStateOf(32) }
  var selectedBodyType by remember { mutableStateOf(VehicleBodyType.CONTAINER.name) }
  var vehicleCity by remember { mutableStateOf(profile.currentCity?.ifBlank { "Bengaluru" } ?: "Bengaluru") }
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
      // 1. Language Selection Dropdown Component at the Top of Profile Screen
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.2.dp, RoyalBluePrimary.copy(alpha = 0.35f)),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "🌐 Select Language / ಭಾಷೆ ಆರಿಸಿ",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = RoyalBlueSoftBackground,
                border = BorderStroke(1.dp, RoyalBluePrimary.copy(alpha = 0.4f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { languageDropdownExpanded = true }
                  .testTag("language_selector_dropdown_trigger"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Filled.Language,
                      contentDescription = null,
                      tint = RoyalBluePrimary,
                      modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = "${currentLanguage.displayName} (${currentLanguage.nativeName})",
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                      color = RoyalBluePrimary,
                    )
                  }
                  Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "Expand",
                    tint = RoyalBluePrimary,
                  )
                }
              }

              DropdownMenu(
                expanded = languageDropdownExpanded,
                onDismissRequest = { languageDropdownExpanded = false },
                modifier = Modifier.widthIn(min = 260.dp),
              ) {
                SupportedLanguage.entries.forEach { lang ->
                  DropdownMenuItem(
                    text = {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Text(
                          text = lang.nativeName,
                          fontWeight = if (currentLanguage == lang) FontWeight.ExtraBold else FontWeight.Normal,
                          color = if (currentLanguage == lang) RoyalBluePrimary else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                          text = lang.displayName,
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    },
                    onClick = {
                      localizationManager.setLanguage(lang.code)
                      languageDropdownExpanded = false
                    },
                  )
                }
              }
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

      // 3. Vehicle Owner Fleet Management Form
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
                text = "Add your vehicles to automatically discover matching loads within 250 km radius.",
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
                    )
                    fleetSuccessMessage = "✅ Vehicle saved! Loads within 250km will match automatically."
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
                          text = "📍 ${veh.currentCity}",
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
}
