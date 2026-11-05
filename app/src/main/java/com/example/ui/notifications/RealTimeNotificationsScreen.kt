package com.example.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.model.AppNotification
import com.example.ui.viewmodel.UiState

object SystemNotificationHelper {
  private const val CHANNEL_ID = "verified_trucks_live_updates"

  fun postLocalStatusNotification(
    context: Context,
    notificationId: Int,
    title: String,
    body: String,
  ) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          "Verified Trucks Deal Updates",
          NotificationManager.IMPORTANCE_HIGH,
        )
      manager.createNotificationChannel(channel)
    }

    val builder =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    try {
      manager.notify(notificationId, builder.build())
    } catch (_: SecurityException) {
      // Gracefully ignore if POST_NOTIFICATIONS runtime permission was declined; in-app feed remains live.
    }
  }
}

@Composable
fun RealTimeNotificationsScreen(
  notificationsState: UiState<List<AppNotification>>,
  onMarkRead: (String) -> Unit,
  onClearAll: () -> Unit = {},
  onDeleteNotification: (String) -> Unit = {},
) {
  val context = LocalContext.current
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 700.dp).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        val hasNotifications =
          (notificationsState as? UiState.Success)?.data?.isNotEmpty() == true
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "🔔 Live Alerts & Updates",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary,
          )
          if (hasNotifications) {
            ButtonDefaults.outlinedButtonColors()
            androidx.compose.material3.TextButton(
              onClick = onClearAll,
              modifier = Modifier.testTag("clear_all_notifications_button"),
            ) {
              Text(
                text = "🗑️ Clear All",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error,
              )
            }
          }
        }
      }

      when (notificationsState) {
        is UiState.Loading -> {
          item {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
        }
        is UiState.Error -> {
          item {
            Text(text = notificationsState.message, color = MaterialTheme.colorScheme.error)
          }
        }
        is UiState.Success -> {
          if (notificationsState.data.isEmpty()) {
            item {
              Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Icon(
                    imageVector = Icons.Filled.NotificationsNone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp),
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  Text(
                    "🔔 No Alerts Yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Instant alerts for incoming bids and confirmed trips appear here live.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          } else {
            items(notificationsState.data, key = { it.id }) { notif ->
              val isAcceptedAlert = notif.type == "BID_ACCEPTED"
              val isStatusChangeAlert = notif.type == "LOAD_STATUS_CHANGED"
              Card(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                      if (!notif.isRead) onMarkRead(notif.id)
                    }
                    .testTag("notification_card_${notif.id}"),
                shape = RoundedCornerShape(18.dp),
                colors =
                  CardDefaults.cardColors(
                    containerColor =
                      when {
                        isAcceptedAlert -> MaterialTheme.colorScheme.secondaryContainer
                        isStatusChangeAlert -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        !notif.isRead -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.surface
                      }
                  ),
                border =
                  BorderStroke(
                    width = if (isAcceptedAlert || isStatusChangeAlert || !notif.isRead) 1.5.dp else 1.dp,
                    color =
                      when {
                        isAcceptedAlert -> MaterialTheme.colorScheme.secondary
                        isStatusChangeAlert -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                      },
                  ),
              ) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector =
                          when (notif.type) {
                            "BID_ACCEPTED" -> Icons.Filled.CheckCircle
                            "NEW_BID" -> Icons.Filled.Gavel
                            "MATCHING_LOAD" -> Icons.Filled.LocalShipping
                            else -> Icons.Filled.NotificationsActive
                          },
                        contentDescription = null,
                        tint =
                          if (isAcceptedAlert || notif.type == "MATCHING_LOAD") {
                            MaterialTheme.colorScheme.secondary
                          } else {
                            MaterialTheme.colorScheme.primary
                          },
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = notif.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                      if (!notif.isRead) {
                        Surface(
                          color = MaterialTheme.colorScheme.secondary,
                          shape = RoundedCornerShape(50),
                        ) {
                          Text(
                            text = "NEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                          )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                      }
                      androidx.compose.material3.IconButton(
                        onClick = { onDeleteNotification(notif.id) },
                        modifier = Modifier.size(28.dp).testTag("delete_notification_${notif.id}"),
                      ) {
                        Icon(
                          imageVector = Icons.Filled.Delete,
                          contentDescription = "Delete notification",
                          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                          modifier = Modifier.size(16.dp),
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(6.dp))

                  Text(
                    text = notif.message,
                    style = MaterialTheme.typography.bodyMedium,
                  )

                  if (!notif.unlockedPickupAddress.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = "📍 Loading Location: ${notif.unlockedPickupAddress}",
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                  }

                  if (!notif.unlockedPhone.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                      onClick = {
                        if (!notif.isRead) onMarkRead(notif.id)
                        context.startActivity(
                          Intent(Intent.ACTION_DIAL, Uri.parse("tel:${notif.unlockedPhone}"))
                        )
                      },
                      colors =
                        ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.secondary,
                          contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    ) {
                      Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("📞 Call Now: ${notif.unlockedPhone}")
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
