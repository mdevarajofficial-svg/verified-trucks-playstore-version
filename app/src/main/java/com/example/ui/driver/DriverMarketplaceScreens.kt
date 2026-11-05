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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DriverTripEntry
import com.example.data.model.LoadPost
import com.example.data.model.MatchedLoadItem
import com.example.data.model.UserPrivateKyc
import com.example.data.model.UserRoleProfile
import com.example.data.model.VehicleBodyType
import com.example.data.util.GeoAndPrivacyUtils
import com.example.ui.theme.VerifiedTrucksBrushes
import com.example.ui.viewmodel.LoadFeedFilter
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DriverLiveFeedContent(
  profile: UserRoleProfile,
  filter: LoadFeedFilter,
  matchedLoadsState: UiState<List<MatchedLoadItem>>,
  onUpdateFilter: ((LoadFeedFilter) -> LoadFeedFilter) -> Unit,
  onToggleAvailability: (Boolean) -> Unit,
  onSelectLoad: (LoadPost) -> Unit,
) {
  val totalVisibleCount = (matchedLoadsState as? UiState.Success)?.data?.size ?: 0
  val hasActiveFilter =
    filter.sizeFeetFilter != null ||
      filter.bodyTypeFilter != null ||
      filter.searchQuery.isNotBlank()

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Live Market Loads Header
      item {
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "🚛 Live Market Loads",
                    style =
                      MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "👋 ${profile.displayName} • Showing all live loads posted across India",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.92f),
                  )
                }

                Switch(
                  checked = profile.isAvailable ?: true,
                  onCheckedChange = onToggleAvailability,
                  modifier = Modifier.testTag("feed_driver_availability_switch"),
                )
              }

              Surface(
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text =
                      if (profile.isAvailable != false) {
                        "🔔 Live Alerts ON • $totalVisibleCount live load(s) available now"
                      } else {
                        "🔕 Alerts paused — turn on switch above for live load notifications"
                      },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                  )
                }
              }
            }
          }
        }
      }

      // Optional Search & Filter Bar (All loads show by default; trucker can filter later)
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedTextField(
              value = filter.searchQuery,
              onValueChange = { q -> onUpdateFilter { it.copy(searchQuery = q) } },
              placeholder = { Text("🔍 Search loading/unloading location or material...") },
              leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search loads") },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("driver_feed_search_input"),
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.FilterList,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Optional Filters (All Loads Shown by Default)",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              FilterChip(
                selected = !hasActiveFilter,
                onClick = { onUpdateFilter { LoadFeedFilter() } },
                label = { Text("🌐 All Live Loads", fontWeight = FontWeight.Bold) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = Color.White,
                  ),
              )
              VehicleBodyType.entries.forEach { body ->
                FilterChip(
                  selected = filter.bodyTypeFilter == body.name,
                  onClick = {
                    onUpdateFilter {
                      it.copy(
                        bodyTypeFilter = if (it.bodyTypeFilter == body.name) null else body.name
                      )
                    }
                  },
                  label = {
                    Text(
                      if (body == VehicleBodyType.OPEN) "🛻 OPEN" else "🚛 CONTAINER",
                      fontWeight = FontWeight.Bold,
                    )
                  },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.primary,
                      selectedLabelColor = Color.White,
                    ),
                )
              }
              GeoAndPrivacyUtils.STANDARD_TRUCK_SIZES_FEET.forEach { ft ->
                FilterChip(
                  selected = filter.sizeFeetFilter == ft,
                  onClick = {
                    onUpdateFilter {
                      it.copy(sizeFeetFilter = if (it.sizeFeetFilter == ft) null else ft)
                    }
                  },
                  label = { Text("$ft ft", fontWeight = FontWeight.Bold) },
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.primary,
                      selectedLabelColor = Color.White,
                    ),
                )
              }
            }
          }
        }
      }

      // Live Active Loads List
      when (matchedLoadsState) {
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
              text = matchedLoadsState.message,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
        is UiState.Success -> {
          if (matchedLoadsState.data.isEmpty()) {
            item {
              Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Icon(
                    imageVector = Icons.Filled.LocalShipping,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp),
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  Text(
                    text = "📦 No Live Loads Found",
                    style =
                      MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text =
                      if (hasActiveFilter) {
                        "No live loads match your current filter. Tap 'All Live Loads' above to see everything in the market."
                      } else {
                        "New loads posted by transporters across India will appear here automatically in real time."
                      },
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
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DriverMatchedLoadCard(
  matchedItem: MatchedLoadItem,
  onClick: () -> Unit,
) {
  val load = matchedItem.load
  val totalRouteKm = load.resolvedRouteDistanceKm()

  Card(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onClick)
        .testTag("driver_matched_load_${load.id}"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(50),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.RadioButtonChecked,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "⚡ LIVE LOAD",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(50),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.Route,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "🛣️ $totalRouteKm km",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "📍 ${load.originCity}  →  🏁 ${load.destinationCity}",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text =
          "🚛 ${load.requiredSizeFeet} ft ${load.requiredBodyType} • ⚖️ ${load.requiredTonnage} Tons • 📦 ${load.materialType}",
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
            imageVector = Icons.Filled.Apartment,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp),
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = load.posterCompanyName,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Surface(
          color = MaterialTheme.colorScheme.secondary,
          shape = RoundedCornerShape(10.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.Gavel,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text =
                if (load.lowestBidAmount != null) {
                  "💰 L1: ₹${load.lowestBidAmount.toInt()} • Bid"
                } else {
                  "💰 Place Bid →"
                },
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
              color = Color.White,
            )
          }
        }
      }
    }
  }
}

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
          color = MaterialTheme.colorScheme.primary,
        )
      }

      when (tripsState) {
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
            Text(text = tripsState.message, color = MaterialTheme.colorScheme.error)
          }
        }
        is UiState.Success -> {
          if (tripsState.data.isEmpty()) {
            item {
              Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
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
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(42.dp),
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  Text(
                    "🚛 No Trips Yet",
                    style =
                      MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text =
                      "Place a bid on any live load to track its status here. When your bid is accepted, the Load Poster's contact number unlocks automatically.",
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

              Card(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectTrip(trip) }
                    .testTag("driver_trip_card_${trip.loadId}"),
                shape = RoundedCornerShape(20.dp),
                colors =
                  CardDefaults.cardColors(
                    containerColor =
                      when {
                        isBidAccepted -> MaterialTheme.colorScheme.secondaryContainer
                        isCancelled -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        else -> MaterialTheme.colorScheme.surface
                      }
                  ),
                border =
                  BorderStroke(
                    width = 1.5.dp,
                    color =
                      when {
                        isBidAccepted || isBooked -> MaterialTheme.colorScheme.secondary
                        isCancelled -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outline
                      },
                  ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  // Top Status Pills: Bid Status + Load Status (BOOKED / CANCELLED / LIVE)
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Surface(
                      color =
                        if (isBidAccepted) {
                          MaterialTheme.colorScheme.secondary
                        } else {
                          MaterialTheme.colorScheme.primaryContainer
                        },
                      shape = RoundedCornerShape(50),
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Icon(
                          imageVector =
                            if (isBidAccepted) Icons.Filled.LockOpen else Icons.Filled.Gavel,
                          contentDescription = null,
                          tint =
                            if (isBidAccepted) {
                              Color.White
                            } else {
                              MaterialTheme.colorScheme.primary
                            },
                          modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                          text = if (isBidAccepted) "✅ BID ACCEPTED" else "⏳ BID PLACED",
                          style =
                            MaterialTheme.typography.labelSmall.copy(
                              fontWeight = FontWeight.ExtraBold
                            ),
                          color =
                            if (isBidAccepted) {
                              Color.White
                            } else {
                              MaterialTheme.colorScheme.primary
                            },
                        )
                      }
                    }

                    Surface(
                      color =
                        when (trip.loadStatus) {
                          "BOOKED" -> MaterialTheme.colorScheme.secondaryContainer
                          "CANCELLED" -> MaterialTheme.colorScheme.errorContainer
                          else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                      shape = RoundedCornerShape(50),
                    ) {
                      val loadBadgeText =
                        when (trip.loadStatus) {
                          "BOOKED" -> "📦 LOAD BOOKED"
                          "CANCELLED" -> "❌ LOAD CANCELLED"
                          "ACCEPTED" -> "✅ ACCEPTED"
                          else -> "⚡ LIVE"
                        }
                      Text(
                        text = loadBadgeText,
                        style =
                          MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold
                          ),
                        color =
                          when (trip.loadStatus) {
                            "BOOKED" -> MaterialTheme.colorScheme.onSecondaryContainer
                            "CANCELLED" -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                          },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                      )
                    }
                  }

                  Text(
                    text =
                      "📍 ${trip.originCity} → 🏁 ${trip.destinationCity} (🛣️ ${trip.totalDistanceKm.toInt().coerceAtLeast(1)} km)",
                    style =
                      MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                  )

                  Text(
                    text =
                      "💰 Your Bid: ₹${trip.bidAmount.toInt()} • 🏢 ${trip.posterCompanyName} • 🚛 ${trip.requiredSizeFeet}ft ${trip.requiredBodyType}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )

                  if (isBooked || isCancelled) {
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color =
                        if (isBooked) {
                          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                          MaterialTheme.colorScheme.errorContainer
                        },
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Icon(
                          imageVector =
                            if (isBooked) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                          contentDescription = null,
                          tint =
                            if (isBooked) {
                              MaterialTheme.colorScheme.primary
                            } else {
                              MaterialTheme.colorScheme.error
                            },
                          modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text =
                            if (isBooked) {
                              "📢 Load Poster marked this load as BOOKED"
                            } else {
                              "📢 Load Poster marked this load as CANCELLED"
                            },
                          style =
                            MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                          color =
                            if (isBooked) {
                              MaterialTheme.colorScheme.primary
                            } else {
                              MaterialTheme.colorScheme.onErrorContainer
                            },
                        )
                      }
                    }
                  }

                  if (isBidAccepted && !trip.unlockedPosterPhone.isNullOrBlank()) {
                    Text(
                      text = "📞 Load Poster Phone: ${trip.unlockedPosterPhone}",
                      style =
                        MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                      color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Button(
                      onClick = {
                        context.startActivity(
                          Intent(Intent.ACTION_DIAL, Uri.parse("tel:${trip.unlockedPosterPhone}"))
                        )
                      },
                      colors =
                        ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.secondary,
                          contentColor = Color.White,
                        ),
                      shape = RoundedCornerShape(12.dp),
                      modifier =
                        Modifier.fillMaxWidth()
                          .height(48.dp)
                          .testTag("trip_call_poster_${trip.loadId}"),
                    ) {
                      Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        "📞 Call Load Poster (${trip.unlockedPosterPhone})",
                        fontWeight = FontWeight.ExtraBold,
                      )
                    }
                  } else {
                    Text(
                      text = "Tap to view leaderboard & load details →",
                      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.primary,
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

@Composable
fun ProfileAndKycSummaryScreen(
  profile: UserRoleProfile,
  privateKyc: UserPrivateKyc?,
  isBusy: Boolean = false,
  onEditVehicleOrKyc: () -> Unit,
  onSignOut: () -> Unit,
) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
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
                text = "👤 ${profile.displayName}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
              )
            }

            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(50),
            ) {
              Text(
                text =
                  if (profile.role == "POSTER") {
                    "🏢 Load Poster / Transporter"
                  } else {
                    "🚛 Vehicle Owner / Driver"
                  },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }

            if (privateKyc != null) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.Call,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "📞 Contact: ${privateKyc.phoneNumber}",
                  style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                )
              }

              if (!privateKyc.drivingLicenseNumber.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.Badge,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "🪪 License: ${privateKyc.drivingLicenseNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              OutlinedButton(
                onClick = onEditVehicleOrKyc,
                enabled = !isBusy,
                modifier = Modifier.weight(1f).testTag("edit_profile_button"),
              ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit Profile", fontWeight = FontWeight.Bold)
              }
              Button(
                onClick = onSignOut,
                enabled = !isBusy,
                modifier = Modifier.weight(1f).testTag("sign_out_profile_button"),
              ) {
                Text("🚪 Sign Out", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}
