package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.location.MapplsLocationService
import com.example.data.location.PlaceLocationResult
import kotlinx.coroutines.delay

/**
 * Renders the custom V logo horizontally aligned next to the official "Verified Trucks" wordmark.
 * Supports `elevatedPill = true` to display on a crisp white pill surface over Royal Blue headers.
 */
@Composable
fun VerifiedTrucksBrandLockup(
  modifier: Modifier = Modifier,
  logoSize: Dp = 36.dp,
  wordmarkHeight: Dp = 24.dp,
  elevatedPill: Boolean = false,
) {
  val content: @Composable () -> Unit = {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier =
        if (elevatedPill) {
          Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        } else {
          Modifier
        },
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_brand_v_logo),
        contentDescription = "Verified Trucks V Logo",
        modifier = Modifier.height(logoSize),
        contentScale = ContentScale.FillHeight,
      )
      Spacer(modifier = Modifier.width(10.dp))
      Image(
        painter = painterResource(id = R.drawable.img_brand_wordmark),
        contentDescription = "Verified Trucks",
        modifier = Modifier.height(wordmarkHeight),
        contentScale = ContentScale.FillHeight,
      )
    }
  }

  if (elevatedPill) {
    Surface(
      color = Color.White,
      shape = RoundedCornerShape(20.dp),
      shadowElevation = 8.dp,
      modifier = modifier,
    ) {
      content()
    }
  } else {
    Box(modifier = modifier) {
      content()
    }
  }
}

/**
 * Clean, empty-by-default location input with simple heading text above the box.
 * Never displays pre-populated city suggestions; live Mappls/Geocoder results appear ONLY
 * when the customer actively types on their own.
 */
@Composable
fun MapplsLocationSearchField(
  heading: String,
  value: String,
  testTag: String,
  onValueChange: (String) -> Unit,
  onLocationSelected: (PlaceLocationResult) -> Unit,
) {
  val context = LocalContext.current
  var showDropdown by remember { mutableStateOf(false) }
  var loadingSuggestions by remember { mutableStateOf(false) }
  var suggestions by remember { mutableStateOf<List<PlaceLocationResult>>(emptyList()) }

  LaunchedEffect(value, showDropdown) {
    if (!showDropdown || value.trim().length < 2) {
      suggestions = emptyList()
      loadingSuggestions = false
      return@LaunchedEffect
    }
    loadingSuggestions = true
    delay(260)
    suggestions = MapplsLocationService.searchLocations(context, value)
    loadingSuggestions = false
  }

  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = heading,
      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(bottom = 6.dp),
    )

    OutlinedTextField(
      value = value,
      onValueChange = { typed ->
        onValueChange(typed.take(120))
        showDropdown = typed.trim().length >= 2
      },
      leadingIcon = {
        Icon(
          imageVector = Icons.Filled.LocationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
      },
      trailingIcon = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (loadingSuggestions) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(6.dp))
          }
          if (value.isNotEmpty()) {
            IconButton(
              onClick = {
                onValueChange("")
                suggestions = emptyList()
                showDropdown = false
              }
            ) {
              Icon(Icons.Filled.Close, contentDescription = "Clear location")
            }
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
        ),
      modifier = Modifier.fillMaxWidth().testTag(testTag),
    )

    if (showDropdown && suggestions.isNotEmpty()) {
      Spacer(modifier = Modifier.height(6.dp))
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).padding(6.dp)) {
          suggestions.forEachIndexed { idx, item ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable {
                    showDropdown = false
                    suggestions = emptyList()
                    onLocationSelected(item)
                  }
                  .padding(vertical = 10.dp, horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.placeName,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = item.placeAddress,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            if (idx < suggestions.lastIndex) {
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            }
          }
        }
      }
    }
  }
}
