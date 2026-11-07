package com.example.ui.bidding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.unit.sp
import com.example.data.model.Bid
import com.example.data.model.LoadPost
import com.example.data.model.UnlockedDealContacts
import com.example.data.repository.LogisticsRepository
import com.example.data.util.GeoAndPrivacyUtils
import com.example.ui.localization.LocalAppLocalization
import com.example.ui.theme.EmeraldGreenAccent
import com.example.ui.theme.EmeraldGreenContainer
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.OnEmeraldGreenContainer
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.RoyalBlueSoftBackground
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiddingAndDealBottomSheet(
  load: LoadPost,
  currentUserId: String,
  isPosterRole: Boolean,
  bidsState: UiState<List<Bid>>,
  unlockedContacts: UnlockedDealContacts?,
  isBusy: Boolean,
  onPlaceBid: (amount: Double, contactPhone: String) -> Unit,
  onAcceptBid: (Bid) -> Unit,
  onMarkLoadStatus: (String) -> Unit,
  onRequestUnlockFetch: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val isPosterOfThisLoad = load.posterId == currentUserId
  val isAcceptedDriver = load.isDriverAccepted(currentUserId)
  val acceptedCount = load.acceptedCount()
  val isTerminalLoad = load.status in listOf("BOOKED", "CANCELLED", "EXPIRED")
  val routeKm = load.resolvedRouteDistanceKm()

  LaunchedEffect(load.id, load.status, acceptedCount) {
    if (acceptedCount > 0 && (isPosterOfThisLoad || isAcceptedDriver)) {
      onRequestUnlockFetch()
    }
  }

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
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      val appStrings = LocalAppLocalization.current

      // Properly aligned Futuristic Load Details Card on top (No state initials)
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = RoyalBlueSoftBackground,
        border = BorderStroke(1.2.dp, RoyalBluePrimary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              Text(
                text = GeoAndPrivacyUtils.cleanCityNameOnly(load.originCity),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = RoyalBluePrimary,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = GeoAndPrivacyUtils.cleanCityNameOnly(load.destinationCity),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            StatusPillBadge(status = load.status, acceptedCount = acceptedCount)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "${load.requiredSizeFeet} ft ${load.requiredBodyType} • ${load.requiredTonnage}T • ${load.materialType}",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
              color = EmeraldGreenContainer,
              shape = RoundedCornerShape(50),
            ) {
              Text(
                text = "$routeKm km",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                color = OnEmeraldGreenContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
        }
      }

      // Load Poster Controls: Mark Load as BOOKED or CANCELLED
      if (isPosterOfThisLoad && !isTerminalLoad) {
        PosterLoadStatusControlsCard(
          isBusy = isBusy,
          onMarkBooked = { onMarkLoadStatus("BOOKED") },
          onMarkCancelled = { onMarkLoadStatus("CANCELLED") },
        )
      } else if (load.status in listOf("BOOKED", "CANCELLED")) {
        LoadFinalStatusBanner(status = load.status)
      }

      // Mutual Direct Contact Card once bid(s) accepted (visible ONLY to Poster & Accepted Drivers)
      if (acceptedCount > 0 && (isPosterOfThisLoad || isAcceptedDriver)) {
        UnlockedContactConfirmationCard(
          isPosterOfThisLoad = isPosterOfThisLoad,
          acceptedCount = acceptedCount,
          fallbackCompanyName = load.posterCompanyName,
          unlockedContacts = unlockedContacts,
        )
      }

      // Driver Competitive Bidding Form (while Load is ACTIVE/ACCEPTED with <3 accepted bids)
      if (
        !isPosterRole &&
          !isPosterOfThisLoad &&
          !isTerminalLoad &&
          acceptedCount < LogisticsRepository.MAX_ACCEPTED_BIDS
      ) {
        DriverBlindBidForm(
          isBusy = isBusy,
          onPlaceBid = onPlaceBid,
        )
      }

      HorizontalDivider()

      // Bidding Leaderboard Header: Exactly 1 trophy icon on left, minimal title "Live Bids"
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = RoyalBluePrimary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = appStrings.liveBids,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
          )
        }
        Text(
          text = appStrings.lowestFirst,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = EmeraldGreenDark,
        )
      }

      // Leaderboard List (Sorted Lowest → Highest)
      when (bidsState) {
        is UiState.Loading -> {
          Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(color = RoyalBluePrimary)
          }
        }
        is UiState.Error -> {
          Text(
            text = bidsState.message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
        is UiState.Success -> {
          if (bidsState.data.isEmpty()) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Icon(
                  imageVector = Icons.Filled.Gavel,
                  contentDescription = null,
                  tint = RoyalBluePrimary,
                  modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = appStrings.noBidsYet,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
              }
            }
          } else {
            val unlockedByBidId =
              unlockedContacts?.acceptedBidders?.associateBy { it.bidId }.orEmpty()
            bidsState.data.forEachIndexed { index, bid ->
              LeaderboardBidCard(
                rank = index + 1,
                bid = bid,
                currentUserId = currentUserId,
                isPosterOfThisLoad = isPosterOfThisLoad,
                isTerminalLoad = isTerminalLoad,
                acceptedCount = acceptedCount,
                unlockedDriverPhone = unlockedByBidId[bid.id]?.driverDirectPhone,
                isBusy = isBusy,
                onAcceptBid = { onAcceptBid(bid) },
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
private fun PosterLoadStatusControlsCard(
  isBusy: Boolean,
  onMarkBooked: () -> Unit,
  onMarkCancelled: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth().testTag("poster_load_status_controls_card"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "📋 Update Load Status",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "Mark this load as Booked or Cancelled. Bidders will see the updated status in My Trips and receive a notification.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = onMarkBooked,
          enabled = !isBusy,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.secondary,
              contentColor = Color.White,
            ),
          shape = RoundedCornerShape(12.dp),
          modifier =
            Modifier.weight(1f)
              .height(48.dp)
              .testTag("mark_load_booked_button"),
        ) {
          Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Booked", fontWeight = FontWeight.ExtraBold)
        }

        OutlinedButton(
          onClick = onMarkCancelled,
          enabled = !isBusy,
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
          colors =
            ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.error,
            ),
          shape = RoundedCornerShape(12.dp),
          modifier =
            Modifier.weight(1f)
              .height(48.dp)
              .testTag("mark_load_cancelled_button"),
        ) {
          Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Cancelled", fontWeight = FontWeight.ExtraBold)
        }
      }
    }
  }
}

@Composable
private fun LoadFinalStatusBanner(status: String) {
  val isBooked = status == "BOOKED"
  Surface(
    shape = RoundedCornerShape(16.dp),
    color =
      if (isBooked) {
        MaterialTheme.colorScheme.secondaryContainer
      } else {
        MaterialTheme.colorScheme.errorContainer
      },
    border =
      BorderStroke(
        1.5.dp,
        if (isBooked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
      ),
    modifier = Modifier.fillMaxWidth().testTag("load_final_status_banner"),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = if (isBooked) Icons.Filled.Inventory2 else Icons.Filled.Cancel,
        contentDescription = null,
        tint = if (isBooked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text =
          if (isBooked) {
            "📦 Load Status: BOOKED"
          } else {
            "❌ Load Status: CANCELLED"
          },
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
        color =
          if (isBooked) {
            MaterialTheme.colorScheme.onSecondaryContainer
          } else {
            MaterialTheme.colorScheme.onErrorContainer
          },
      )
    }
  }
}

@Composable
private fun StatusPillBadge(
  status: String,
  acceptedCount: Int,
) {
  val containerColor =
    when (status) {
      "BOOKED", "ACCEPTED" -> MaterialTheme.colorScheme.secondaryContainer
      "CANCELLED" -> MaterialTheme.colorScheme.errorContainer
      else -> MaterialTheme.colorScheme.primaryContainer
    }
  val contentColor =
    when (status) {
      "BOOKED", "ACCEPTED" -> MaterialTheme.colorScheme.onSecondaryContainer
      "CANCELLED" -> MaterialTheme.colorScheme.onErrorContainer
      else -> MaterialTheme.colorScheme.primary
    }
  val label =
    when (status) {
      "BOOKED" -> "📦 BOOKED"
      "CANCELLED" -> "❌ CANCELLED"
      "ACCEPTED" -> "✅ $acceptedCount ACCEPTED"
      "EXPIRED" -> "⏳ EXPIRED"
      else -> "⚡ LIVE"
    }

  Surface(
    color = containerColor,
    shape = RoundedCornerShape(50),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector =
          when (status) {
            "BOOKED", "ACCEPTED" -> Icons.Filled.CheckCircle
            "CANCELLED" -> Icons.Filled.Cancel
            else -> Icons.Filled.RadioButtonChecked
          },
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(15.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
        color = contentColor,
      )
    }
  }
}

@Composable
fun UnlockedContactConfirmationCard(
  isPosterOfThisLoad: Boolean,
  acceptedCount: Int,
  fallbackCompanyName: String,
  unlockedContacts: UnlockedDealContacts?,
) {
  val context = LocalContext.current

  Card(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer
      ),
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary),
    modifier = Modifier.fillMaxWidth().testTag("unlocked_contact_card"),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.CheckCircle,
          contentDescription = "Bid Accepted",
          tint = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text =
            if (isPosterOfThisLoad) {
              "✅ Accepted Bidder Contacts ($acceptedCount)"
            } else {
              "✅ Bid Accepted! Load Poster Contact Unlocked"
            },
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }

      if (isPosterOfThisLoad) {
        val bidders = unlockedContacts?.acceptedBidders.orEmpty()
        if (bidders.isEmpty()) {
          Text(
            text = "Loading unlocked bidder contact numbers...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
          )
        } else {
          bidders.forEachIndexed { idx, bidder ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "🚛 #${idx + 1} ${bidder.driverFullName}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  )
                  Text(
                    text = "₹${bidder.bidAmount.toInt()}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                Text(
                  text = "📞 Bidder Phone: ${bidder.driverDirectPhone}",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Button(
                  onClick = {
                    context.startActivity(
                      Intent(Intent.ACTION_DIAL, Uri.parse("tel:${bidder.driverDirectPhone}"))
                    )
                  },
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.secondary,
                      contentColor = Color.White,
                    ),
                  modifier =
                    Modifier.fillMaxWidth()
                      .height(48.dp)
                      .testTag("call_accepted_bidder_${idx + 1}"),
                  shape = RoundedCornerShape(10.dp),
                ) {
                  Icon(Icons.Filled.Call, contentDescription = null)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    bidder.driverDirectPhone,
                    fontWeight = FontWeight.ExtraBold,
                  )
                }
              }
            }
          }
        }
      } else {
        val posterPhone = unlockedContacts?.posterDirectPhone.orEmpty()
        val companyName = unlockedContacts?.posterCompanyName ?: fallbackCompanyName
        Text(
          text = "🏢 Shipper: $companyName",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        if (!unlockedContacts?.exactPickupAddress.isNullOrBlank()) {
          Text(
            text = "📍 Loading Location: ${unlockedContacts?.exactPickupAddress}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
          )
        }
        if (posterPhone.isNotBlank()) {
          Text(
            text = "📞 Load Poster Phone: $posterPhone",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
          )
          Button(
            onClick = {
              context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$posterPhone")))
            },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
              ),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("call_poster_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Filled.Call, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(posterPhone, fontWeight = FontWeight.ExtraBold)
          }
        } else {
          Text(
            text = "Loading Load Poster contact number...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
          )
        }
      }
    }
  }
}

@Composable
private fun DriverBlindBidForm(
  isBusy: Boolean,
  onPlaceBid: (Double, String) -> Unit,
) {
  val appStrings = LocalAppLocalization.current
  var bidAmountText by remember { mutableStateOf("") }
  var contactPhoneText by remember { mutableStateOf("") }
  val parsedBid = bidAmountText.toDoubleOrNull() ?: 0.0
  val phoneDigitCount = contactPhoneText.count { it.isDigit() }
  val isFormValid =
    parsedBid > 0 && phoneDigitCount in 10..13 && contactPhoneText.trim().length in 10..16

  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.5.dp, RoyalBluePrimary.copy(alpha = 0.35f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = appStrings.placeBidNow,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
        color = RoyalBluePrimary,
      )

      OutlinedTextField(
        value = bidAmountText,
        onValueChange = { bidAmountText = it.filter { ch -> ch.isDigit() }.take(8) },
        label = { Text(appStrings.amount) },
        leadingIcon = {
          Icon(
            Icons.Filled.CurrencyRupee,
            contentDescription = null,
            tint = RoyalBluePrimary,
          )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("bid_amount_input"),
      )

      OutlinedTextField(
        value = contactPhoneText,
        onValueChange = { raw ->
          contactPhoneText = raw.filter { ch -> ch.isDigit() || ch == '+' }.take(15)
        },
        label = { Text(appStrings.phone) },
        leadingIcon = {
          Icon(
            Icons.Filled.Call,
            contentDescription = null,
            tint = RoyalBluePrimary,
          )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("bid_contact_phone_input"),
      )

      Button(
        onClick = {
          val submittedBid = parsedBid
          val submittedPhone = contactPhoneText.trim()
          bidAmountText = ""
          contactPhoneText = ""
          onPlaceBid(submittedBid, submittedPhone)
        },
        enabled = isFormValid && !isBusy,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = RoyalBluePrimary,
            contentColor = Color.White,
          ),
        modifier =
          Modifier.fillMaxWidth()
            .height(50.dp)
            .testTag("submit_blind_bid_button"),
        shape = RoundedCornerShape(50),
      ) {
        if (isBusy) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = Color.White,
            strokeWidth = 2.dp,
          )
        } else {
          Icon(Icons.Filled.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = appStrings.placeBidNow,
            fontWeight = FontWeight.Black,
          )
        }
      }
    }
  }
}

@Composable
private fun LeaderboardBidCard(
  rank: Int,
  bid: Bid,
  currentUserId: String,
  isPosterOfThisLoad: Boolean,
  isTerminalLoad: Boolean,
  acceptedCount: Int,
  unlockedDriverPhone: String?,
  isBusy: Boolean,
  onAcceptBid: () -> Unit,
) {
  val context = LocalContext.current
  val isLeading = rank == 1
  val isTopThree = rank <= LogisticsRepository.MAX_ACCEPTED_BIDS
  val isOwnDriverBid = bid.driverId == currentUserId
  val isAcceptedBid = bid.status == "ACCEPTED"

  Card(
    modifier = Modifier.fillMaxWidth().testTag("leaderboard_bid_card_$rank"),
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          when {
            isAcceptedBid -> MaterialTheme.colorScheme.secondaryContainer
            isLeading -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else -> MaterialTheme.colorScheme.surface
          }
      ),
    border =
      BorderStroke(
        width = if (isLeading || isAcceptedBid) 1.5.dp else 1.dp,
        color =
          when {
            isAcceptedBid -> MaterialTheme.colorScheme.secondary
            isLeading -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
          },
      ),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color =
              when {
                isLeading -> MaterialTheme.colorScheme.secondary
                isTopThree -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
              },
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = "$rank",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
              color =
                when {
                  isLeading -> Color.White
                  isTopThree -> MaterialTheme.colorScheme.primary
                  else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f, fill = false)) {
            val titleLabel =
              if (isAcceptedBid) {
                if (isPosterOfThisLoad) "🚛 ${bid.driverDisplayName}" else "🚛 Bidder #$rank"
              } else {
                if (isOwnDriverBid) "S.No #$rank (Your Bid)" else "S.No #$rank"
              }
            Text(
              text = titleLabel,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
              text = if (isAcceptedBid) "Bid Accepted" else "Verified Bidder",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "₹${bid.bidAmount.toInt()}",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary,
            softWrap = false,
            maxLines = 1,
          )
        }
      }

      if (isAcceptedBid) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (isPosterOfThisLoad && !unlockedDriverPhone.isNullOrBlank()) {
            OutlinedButton(
              onClick = {
                context.startActivity(
                  Intent(Intent.ACTION_DIAL, Uri.parse("tel:$unlockedDriverPhone"))
                )
              },
              modifier = Modifier.height(48.dp),
            ) {
              Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(unlockedDriverPhone, fontWeight = FontWeight.ExtraBold)
            }
          } else {
            Spacer(modifier = Modifier.width(4.dp))
          }
          Surface(
            color = MaterialTheme.colorScheme.secondary,
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(
              text = "✅ ACCEPTED",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
              color = Color.White,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
          }
        }
      } else if (
        isPosterOfThisLoad &&
          !isTerminalLoad &&
          bid.status == "PENDING"
      ) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          Button(
            onClick = onAcceptBid,
            enabled = !isBusy,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = Color.White,
              ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(48.dp).testTag("accept_bid_button_$rank"),
          ) {
            Text("Accept Bid", fontWeight = FontWeight.ExtraBold)
          }
        }
      }
    }
  }
}
