package com.example.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.UserPrivateKyc
import com.example.data.model.UserRole
import com.example.data.model.UserRoleProfile
import com.example.ui.components.VerifiedTrucksBrandLockup
import com.example.ui.theme.VerifiedTrucksBrushes

@Composable
fun RoleSelectionAndOnboardingFlow(
  initialEmailOrName: String,
  existingProfile: UserRoleProfile?,
  existingKyc: UserPrivateKyc?,
  isBusy: Boolean,
  onSubmitPoster: (companyName: String, phone: String) -> Unit,
  onSubmitDriver: (
    fullName: String,
    phone: String,
    dlNumber: String,
  ) -> Unit,
  onSignOut: () -> Unit,
) {
  var selectedRole by remember { mutableStateOf<UserRole?>(null) }

  if (selectedRole == null) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(Color.White)
          .windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier =
          Modifier.widthIn(max = 560.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          VerifiedTrucksBrandLockup(logoSize = 36.dp, wordmarkHeight = 24.dp)
          TextButton(
            onClick = onSignOut,
            modifier = Modifier.testTag("sign_out_role_screen_button"),
          ) {
            Text("🚪 Sign Out", fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(
          shape = RoundedCornerShape(22.dp),
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(VerifiedTrucksBrushes.ExecutiveHeroGradient)
                .padding(20.dp)
          ) {
            Column {
              Text(
                text = "👋 Choose Your Role",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Signed in as $initialEmailOrName",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.88f),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        RoleOptionCard(
          testTag = "select_role_poster_card",
          icon = Icons.Filled.Apartment,
          accentColor = MaterialTheme.colorScheme.primary,
          badge = "📦 SHIPPER / TRANSPORTER",
          title = stringResource(R.string.role_poster_title),
          description = stringResource(R.string.role_poster_subtitle),
          onClick = { selectedRole = UserRole.POSTER },
        )

        Spacer(modifier = Modifier.height(16.dp))

        RoleOptionCard(
          testTag = "select_role_driver_card",
          icon = Icons.Filled.LocalShipping,
          accentColor = MaterialTheme.colorScheme.secondary,
          badge = "🚛 VEHICLE OWNER / DRIVER",
          title = stringResource(R.string.role_driver_title),
          description = stringResource(R.string.role_driver_subtitle),
          onClick = { selectedRole = UserRole.DRIVER },
        )
      }
    }
  } else if (selectedRole == UserRole.POSTER) {
    BackHandler { selectedRole = null }
    PosterOnboardingScreen(
      defaultCompanyName = existingProfile?.displayName ?: initialEmailOrName.substringBefore("@"),
      defaultPhone = existingKyc?.phoneNumber ?: "",
      isBusy = isBusy,
      onBack = { selectedRole = null },
      onSubmit = onSubmitPoster,
    )
  } else {
    BackHandler { selectedRole = null }
    DriverKycOnboardingScreen(
      existingProfile = existingProfile,
      existingKyc = existingKyc,
      defaultName = initialEmailOrName.substringBefore("@"),
      isBusy = isBusy,
      onBack = { selectedRole = null },
      onSubmit = onSubmitDriver,
    )
  }
}

@Composable
private fun RoleOptionCard(
  testTag: String,
  icon: ImageVector,
  accentColor: Color,
  badge: String,
  title: String,
  description: String,
  onClick: () -> Unit,
) {
  Card(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .clickable(onClick = onClick)
        .testTag(testTag),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(2.dp, accentColor.copy(alpha = 0.45f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = accentColor.copy(alpha = 0.14f),
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = accentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          )
        }
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = "Select $title",
          tint = accentColor,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier =
            Modifier.size(50.dp)
              .clip(CircleShape)
              .background(accentColor),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(26.dp),
          )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun PosterOnboardingScreen(
  defaultCompanyName: String,
  defaultPhone: String,
  isBusy: Boolean,
  onBack: () -> Unit,
  onSubmit: (String, String) -> Unit,
) {
  var companyName by remember { mutableStateOf(defaultCompanyName) }
  var phone by remember { mutableStateOf(defaultPhone) }
  val digitsCount = phone.count { it.isDigit() }
  val isValid = companyName.trim().length >= 2 && digitsCount in 10..13

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 560.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("poster_onboarding_back")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
          text = "🏢 Transporter / Company Profile",
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          OutlinedTextField(
            value = companyName,
            onValueChange = { companyName = it },
            label = { Text("🏢 Company / Transporter Name") },
            leadingIcon = {
              Icon(Icons.Filled.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("poster_company_input"),
          )

          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.take(15) },
            label = { Text("📞 Contact Number") },
            leadingIcon = {
              Icon(Icons.Filled.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("poster_phone_input"),
          )

          Button(
            onClick = { onSubmit(companyName, phone) },
            enabled = isValid && !isBusy,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
              ),
            modifier =
              Modifier.fillMaxWidth()
                .height(54.dp)
                .testTag("submit_poster_onboarding_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            if (isBusy) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp,
              )
            } else {
              Icon(Icons.Filled.CheckCircle, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Save & Continue", fontWeight = FontWeight.ExtraBold)
            }
          }
        }
      }
    }
  }
}

@Composable
fun DriverKycOnboardingScreen(
  existingProfile: UserRoleProfile?,
  existingKyc: UserPrivateKyc?,
  defaultName: String,
  isBusy: Boolean,
  onBack: () -> Unit,
  onSubmit: (
    fullName: String,
    phone: String,
    dlNumber: String,
  ) -> Unit,
) {
  var fullName by remember {
    mutableStateOf(existingKyc?.fullName ?: existingProfile?.displayName ?: defaultName)
  }
  var phone by remember { mutableStateOf(existingKyc?.phoneNumber ?: "") }
  var dlNumber by remember { mutableStateOf(existingKyc?.drivingLicenseNumber ?: "") }

  val phoneValid = phone.count { it.isDigit() } in 10..13
  val canSubmit = fullName.trim().length >= 2 && phoneValid && !isBusy

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 560.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("driver_kyc_back_button")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Column {
          Text(
            text = "🚛 Vehicle Owner / Driver Profile",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "View all live loads in the market and place competitive bids instantly.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Badge,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "👤 Profile Details",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
          }

          OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("👤 Full Name") },
            leadingIcon = {
              Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("driver_fullname_input"),
          )

          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.take(15) },
            label = { Text("📞 Contact Number") },
            leadingIcon = {
              Icon(Icons.Filled.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("driver_phone_input"),
          )

          OutlinedTextField(
            value = dlNumber,
            onValueChange = { dlNumber = it.uppercase().take(20) },
            label = { Text("🪪 Driving License No. (Optional)") },
            leadingIcon = {
              Icon(Icons.Filled.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("driver_dl_input"),
          )
        }
      }

      Button(
        onClick = {
          onSubmit(fullName, phone, dlNumber)
        },
        enabled = canSubmit,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = Color.White,
          ),
        modifier =
          Modifier.fillMaxWidth()
            .height(56.dp)
            .testTag("submit_driver_kyc_profile_button"),
        shape = RoundedCornerShape(16.dp),
      ) {
        if (isBusy) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Color.White,
            strokeWidth = 2.dp,
          )
        } else {
          Icon(Icons.Filled.CheckCircle, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("🚛 View Live Loads", fontWeight = FontWeight.ExtraBold)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
