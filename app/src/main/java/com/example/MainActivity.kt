package com.example

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.UserRole
import com.example.data.repository.LogisticsRepository
import com.example.ui.auth.AuthSplashScreen
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.bidding.BiddingAndDealBottomSheet
import com.example.ui.components.VerifiedTrucksBrandLockup
import com.example.ui.driver.DriverLiveFeedContent
import com.example.ui.driver.DriverWonDealsContent
import com.example.ui.driver.ProfileAndKycSummaryScreen
import com.example.ui.notifications.RealTimeNotificationsScreen
import com.example.ui.notifications.SystemNotificationHelper
import com.example.ui.onboarding.RoleSelectionAndOnboardingFlow
import com.example.ui.poster.CreateLoadBottomSheet
import com.example.ui.poster.PosterDashboardContent
import com.example.ui.theme.VerifiedTrucksTheme
import com.example.ui.viewmodel.LogisticsViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

import com.example.ui.localization.AppLocalizationProvider
import com.example.ui.localization.LocalAppLocalization

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle =
        SystemBarStyle.light(
          android.graphics.Color.TRANSPARENT,
          android.graphics.Color.TRANSPARENT,
        ),
      navigationBarStyle =
        SystemBarStyle.light(
          android.graphics.Color.TRANSPARENT,
          android.graphics.Color.TRANSPARENT,
        ),
    )
    setContent {
      AppLocalizationProvider {
        VerifiedTrucksTheme {
          VerifiedTrucksRootNavigation()
        }
      }
    }
  }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun VerifiedTrucksRootNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  // Strict Auth Gate: Never instantiate LogisticsViewModel or query Firestore while unauthenticated
  if (user == null) {
    AuthSplashScreen(onAuthSuccess = {})
  } else {
    AuthenticatedWorkspace(
      currentUserId = user.uid,
      userEmailOrName = user.displayName ?: user.email ?: "Verified Member",
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedWorkspace(
  currentUserId: String,
  userEmailOrName: String,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  // Keyed by currentUserId and injected with custom R.string.firestore_database_id
  val viewModel: LogisticsViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY]) as Application
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            LogisticsViewModel(LogisticsRepository(db), currentUserId)
          }
        },
    )

  val roleProfileState by viewModel.roleProfileState.collectAsStateWithLifecycle()
  val privateKyc by viewModel.privateKyc.collectAsStateWithLifecycle()
  val feedFilter by viewModel.feedFilter.collectAsStateWithLifecycle()
  val matchedLoadsState by viewModel.matchedDriverLoadsState.collectAsStateWithLifecycle()
  val strictAlertLoads by viewModel.strictAlertMatchedLoadsState.collectAsStateWithLifecycle()
  val posterLoadsState by viewModel.posterLoadsState.collectAsStateWithLifecycle()
  val driverTripsState by viewModel.driverTripsState.collectAsStateWithLifecycle()
  val driverFleetState by viewModel.driverFleetState.collectAsStateWithLifecycle()
  val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
  val selectedLoad by viewModel.selectedLoad.collectAsStateWithLifecycle()
  val selectedLoadBidsState by viewModel.selectedLoadBidsState.collectAsStateWithLifecycle()
  val unlockedContactsMap by viewModel.unlockedContactsMap.collectAsStateWithLifecycle()
  val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

  var isEditingRoleOrKyc by rememberSaveable { mutableStateOf(false) }
  var showCreateLoadSheet by rememberSaveable { mutableStateOf(false) }
  var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
  var announcedNotificationIds by remember(currentUserId) { mutableStateOf(setOf<String>()) }
  var announcedDriverLoadAlertIds by remember(currentUserId) { mutableStateOf(setOf<String>()) }

  // Request POST_NOTIFICATIONS on Android 13+ once signed in for real-time status & load alerts
  val notifPermLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }
  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
      ) {
        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  // Trigger real-time system notification banner for new unread bid/acceptance updates
  LaunchedEffect(notificationsState) {
    val list = (notificationsState as? UiState.Success)?.data ?: return@LaunchedEffect
    val freshUnread = list.filter { !it.isRead && !announcedNotificationIds.contains(it.id) }
    if (freshUnread.isNotEmpty()) {
      val newest = freshUnread.first()
      SystemNotificationHelper.postLocalStatusNotification(
        context = context,
        notificationId = newest.id.hashCode(),
        title = newest.title,
        body = newest.message,
      )
      announcedNotificationIds = announcedNotificationIds + freshUnread.map { it.id }
    }
  }

  // Real-time Live Load Alerts for Truck Owners / Drivers
  LaunchedEffect(strictAlertLoads) {
    val newMatchedItems =
      strictAlertLoads.filter { !announcedDriverLoadAlertIds.contains(it.load.id) }
    if (newMatchedItems.isNotEmpty()) {
      val topMatch = newMatchedItems.first()
      val load = topMatch.load
      val routeKm = load.resolvedRouteDistanceKm()
      SystemNotificationHelper.postLocalStatusNotification(
        context = context,
        notificationId = ("match_${load.id}").hashCode(),
        title = "🚛 New ${load.requiredSizeFeet}ft ${load.requiredBodyType} Load",
        body = "📍 ${load.originCity} → 🏁 ${load.destinationCity} • 🛣️ $routeKm km",
      )
      announcedDriverLoadAlertIds =
        announcedDriverLoadAlertIds + newMatchedItems.map { it.load.id }
      snackbarHostState.showSnackbar(
        "🔔 New Load: ${load.originCity} → ${load.destinationCity} (${load.requiredSizeFeet}ft)"
      )
    }
  }

  LaunchedEffect(actionMessage) {
    val msg = actionMessage ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(msg)
    viewModel.clearMessage()
  }

  when (val profileRes = roleProfileState) {
    is UiState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }
    is UiState.Error -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = profileRes.message, color = MaterialTheme.colorScheme.error)
      }
    }
    is UiState.Success -> {
      val profile = profileRes.data
      if (profile == null || !profile.kycVerified || isEditingRoleOrKyc) {
        if (isEditingRoleOrKyc) {
          BackHandler { isEditingRoleOrKyc = false }
        }
        RoleSelectionAndOnboardingFlow(
          initialEmailOrName = userEmailOrName,
          existingProfile = profile,
          existingKyc = privateKyc,
          isBusy = isBusy,
          onSubmitPoster = { company, phone ->
            viewModel.submitPosterOnboarding(company, phone) {
              isEditingRoleOrKyc = false
              selectedTabIndex = 0
            }
          },
          onSubmitDriver = { fullName, phone, dl ->
            viewModel.submitDriverKycAndVehicle(
              fullName = fullName,
              phoneNumber = phone,
              drivingLicenseNumber = dl,
            ) {
              isEditingRoleOrKyc = false
              selectedTabIndex = 0
            }
          },
          onSignOut = {
            GoogleAuthHelper.signOut(context, scope) {}
          },
        )
      } else {
        // Main Role-Specific Marketplace Shell (Adaptive Bottom Nav + Wide Screen NavigationRail)
        if (selectedTabIndex != 0) {
          BackHandler { selectedTabIndex = 0 }
        }

        val isPoster = profile.role == UserRole.POSTER.name
        val unreadCount =
          ((notificationsState as? UiState.Success)?.data?.count { !it.isRead }) ?: 0

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          val isExpandedScreen = maxWidth >= 720.dp

          Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
              TopAppBar(
                title = {
                  VerifiedTrucksBrandLockup(
                    logoSize = 34.dp,
                    wordmarkHeight = 22.dp,
                  )
                },
                actions = {
                  Surface(
                    color =
                      if (isPoster) {
                        MaterialTheme.colorScheme.primaryContainer
                      } else {
                        MaterialTheme.colorScheme.secondaryContainer
                      },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(end = 12.dp),
                  ) {
                    Text(
                      text = if (isPoster) "🏢 SHIPPER" else "🚛 PARTNER",
                      style =
                        MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                      color =
                        if (isPoster) {
                          MaterialTheme.colorScheme.primary
                        } else {
                          MaterialTheme.colorScheme.onSecondaryContainer
                        },
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                  }
                },
                colors =
                  TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                  ),
              )
            },
            floatingActionButton = {
              if (isPoster && selectedTabIndex == 0) {
                ExtendedFloatingActionButton(
                  onClick = { showCreateLoadSheet = true },
                  containerColor = MaterialTheme.colorScheme.secondary,
                  contentColor = Color.White,
                  icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                  text = { Text("🚛 Post Load", fontWeight = FontWeight.ExtraBold) },
                  modifier = Modifier.testTag("fab_post_load"),
                )
              }
            },
            bottomBar = {
              if (!isExpandedScreen) {
                MarketplaceBottomBar(
                  isPoster = isPoster,
                  selectedTabIndex = selectedTabIndex,
                  unreadCount = unreadCount,
                  onSelectTab = { selectedTabIndex = it },
                )
              }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
          ) { innerPadding ->
            Row(
              modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) {
              if (isExpandedScreen) {
                MarketplaceSideRail(
                  isPoster = isPoster,
                  selectedTabIndex = selectedTabIndex,
                  unreadCount = unreadCount,
                  onSelectTab = { selectedTabIndex = it },
                )
              }

              Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                if (isPoster) {
                  when (selectedTabIndex) {
                    0 ->
                      PosterDashboardContent(
                        profile = profile,
                        loadsState = posterLoadsState,
                        isBusy = isBusy,
                        onOpenCreateLoad = { showCreateLoadSheet = true },
                        onSelectLoad = { viewModel.selectLoadForBidding(it) },
                        onMarkLoadStatus = { load, status ->
                          viewModel.markLoadStatus(load, status)
                        },
                      )
                    1 ->
                      RealTimeNotificationsScreen(
                        notificationsState = notificationsState,
                        onMarkRead = { viewModel.markNotificationRead(it) },
                        onClearAll = { viewModel.clearAllNotifications() },
                        onDeleteNotification = { viewModel.deleteNotification(it) },
                      )
                    else ->
                      ProfileAndKycSummaryScreen(
                        profile = profile,
                        privateKyc = privateKyc,
                        isBusy = isBusy,
                        onEditVehicleOrKyc = { isEditingRoleOrKyc = true },
                        onSignOut = { GoogleAuthHelper.signOut(context, scope) {} },
                      )
                  }
                } else {
                  when (selectedTabIndex) {
                    0 ->
                      DriverLiveFeedContent(
                        profile = profile,
                        filter = feedFilter,
                        matchedLoadsState = matchedLoadsState,
                        onUpdateFilter = { viewModel.updateFeedFilter(it) },
                        onToggleAvailability = { avail ->
                          viewModel.updateDriverAvailability(isAvailable = avail)
                        },
                        onSelectLoad = { viewModel.selectLoadForBidding(it) },
                        onOpenCreateLoad = { showCreateLoadSheet = true },
                      )
                    1 ->
                      DriverWonDealsContent(
                        tripsState = driverTripsState,
                        onSelectTrip = { trip -> viewModel.selectLoadForBidding(trip.toLoadPost()) },
                      )
                    2 ->
                      RealTimeNotificationsScreen(
                        notificationsState = notificationsState,
                        onMarkRead = { viewModel.markNotificationRead(it) },
                        onClearAll = { viewModel.clearAllNotifications() },
                        onDeleteNotification = { viewModel.deleteNotification(it) },
                      )
                    else ->
                      ProfileAndKycSummaryScreen(
                        profile = profile,
                        privateKyc = privateKyc,
                        isBusy = isBusy,
                        fleetVehiclesState = driverFleetState,
                        onSaveFleetVehicle = { ft, body, loc ->
                          viewModel.saveDriverFleetVehicle(ft, body, loc)
                        },
                        onEditVehicleOrKyc = { isEditingRoleOrKyc = true },
                        onSignOut = { GoogleAuthHelper.signOut(context, scope) {} },
                      )
                  }
                }
              }
            }
          }
        }

        if (showCreateLoadSheet) {
          CreateLoadBottomSheet(
            profile = profile,
            privateKyc = privateKyc,
            isBusy = isBusy,
            onSubmitLoad = {
              company,
              phone,
              origCity,
              origAddr,
              origLat,
              origLng,
              dstCity,
              dstAddr,
              dstLat,
              dstLng,
              totalKm,
              sizeFt,
              tons,
              body,
              material,
              ->
              viewModel.createNewLoadPost(
                posterCompanyName = company,
                posterDirectPhone = phone,
                originCity = origCity,
                originAddress = origAddr,
                originLat = origLat,
                originLng = origLng,
                destinationCity = dstCity,
                destinationAddress = dstAddr,
                destLat = dstLat,
                destLng = dstLng,
                totalDistanceKm = totalKm,
                requiredSizeFeet = sizeFt,
                requiredTonnage = tons,
                requiredBodyType = body,
                materialType = material,
              ) {
                showCreateLoadSheet = false
              }
            },
            onDismiss = { showCreateLoadSheet = false },
          )
        }

        val activeSelectedLoad = selectedLoad
        if (activeSelectedLoad != null) {
          BiddingAndDealBottomSheet(
            load = activeSelectedLoad,
            currentUserId = currentUserId,
            isPosterRole = isPoster,
            bidsState = selectedLoadBidsState,
            unlockedContacts = unlockedContactsMap[activeSelectedLoad.id],
            isBusy = isBusy,
            onPlaceBid = { amount, contactPhone ->
              viewModel.placeDriverBid(activeSelectedLoad, amount, contactPhone)
            },
            onAcceptBid = { winningBid ->
              viewModel.acceptWinningBid(activeSelectedLoad, winningBid)
            },
            onMarkLoadStatus = { status ->
              viewModel.markLoadStatus(activeSelectedLoad, status)
            },
            onRequestUnlockFetch = {
              viewModel.loadUnlockedDealContacts(activeSelectedLoad, forceRefresh = true)
            },
            onDismiss = { viewModel.selectLoadForBidding(null) },
          )
        }
      }
    }
  }
}

@Composable
private fun MarketplaceBottomBar(
  isPoster: Boolean,
  selectedTabIndex: Int,
  unreadCount: Int,
  onSelectTab: (Int) -> Unit,
) {
  val appStrings = LocalAppLocalization.current
  NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
    if (isPoster) {
      NavigationBarItem(
        selected = selectedTabIndex == 0,
        onClick = { onSelectTab(0) },
        icon = {
          Icon(
            if (selectedTabIndex == 0) Icons.Filled.Gavel else Icons.Outlined.Gavel,
            contentDescription = appStrings.myBids,
          )
        },
        label = { Text(appStrings.myBids, fontWeight = FontWeight.Bold) },
        modifier = Modifier.testTag("nav_poster_loads"),
      )
      NavigationBarItem(
        selected = selectedTabIndex == 1,
        onClick = { onSelectTab(1) },
        icon = {
          NotificationBadgedIcon(
            selected = selectedTabIndex == 1,
            unreadCount = unreadCount,
          )
        },
        label = { Text(appStrings.alerts, fontWeight = FontWeight.Bold) },
        modifier = Modifier.testTag("nav_notifications"),
      )
      NavigationBarItem(
        selected = selectedTabIndex == 2,
        onClick = { onSelectTab(2) },
        icon = {
          Icon(
            if (selectedTabIndex == 2) Icons.Filled.Person else Icons.Outlined.Person,
            contentDescription = appStrings.profile,
          )
        },
        label = { Text(appStrings.profile, fontWeight = FontWeight.Bold) },
        modifier = Modifier.testTag("nav_profile"),
      )
    } else {
      NavigationBarItem(
        selected = selectedTabIndex == 0,
        onClick = { onSelectTab(0) },
        icon = {
          Icon(
            if (selectedTabIndex == 0) Icons.Filled.Radar else Icons.Outlined.Radar,
            contentDescription = appStrings.liveLoads,
          )
        },
        label = { Text(appStrings.liveLoads, fontWeight = FontWeight.Bold) },
        modifier = Modifier.testTag("nav_driver_feed"),
      )
      NavigationBarItem(
        selected = selectedTabIndex == 1,
        onClick = { onSelectTab(1) },
        icon = {
          Icon(
            if (selectedTabIndex == 1) Icons.Filled.Route else Icons.Outlined.Route,
            contentDescription = appStrings.myTrips,
          )
        },
        label = { Text(appStrings.myTrips, fontWeight = FontWeight.Bold) },
        modifier = Modifier.testTag("nav_driver_won"),
      )
      NavigationBarItem(
        selected = selectedTabIndex == 2,
        onClick = { onSelectTab(2) },
        icon = {
          NotificationBadgedIcon(
            selected = selectedTabIndex == 2,
            unreadCount = unreadCount,
          )
        },
        label = { Text(appStrings.alerts, fontWeight = FontWeight.Bold) },
        modifier = Modifier.testTag("nav_notifications"),
      )
      NavigationBarItem(
        selected = selectedTabIndex == 3,
        onClick = { onSelectTab(3) },
        icon = {
          Icon(
            if (selectedTabIndex == 3) Icons.Filled.Person else Icons.Outlined.Person,
            contentDescription = appStrings.profile,
          )
        },
        label = { Text(appStrings.profile, fontWeight = FontWeight.Bold) },
        modifier = Modifier.testTag("nav_profile"),
      )
    }
  }
}

@Composable
private fun MarketplaceSideRail(
  isPoster: Boolean,
  selectedTabIndex: Int,
  unreadCount: Int,
  onSelectTab: (Int) -> Unit,
) {
  val appStrings = LocalAppLocalization.current
  NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
    if (isPoster) {
      NavigationRailItem(
        selected = selectedTabIndex == 0,
        onClick = { onSelectTab(0) },
        icon = { Icon(Icons.Filled.Gavel, contentDescription = appStrings.myBids) },
        label = { Text(appStrings.myBids) },
      )
      NavigationRailItem(
        selected = selectedTabIndex == 1,
        onClick = { onSelectTab(1) },
        icon = {
          NotificationBadgedIcon(selected = selectedTabIndex == 1, unreadCount = unreadCount)
        },
        label = { Text(appStrings.alerts) },
      )
      NavigationRailItem(
        selected = selectedTabIndex == 2,
        onClick = { onSelectTab(2) },
        icon = { Icon(Icons.Filled.Person, contentDescription = appStrings.profile) },
        label = { Text(appStrings.profile) },
      )
    } else {
      NavigationRailItem(
        selected = selectedTabIndex == 0,
        onClick = { onSelectTab(0) },
        icon = { Icon(Icons.Filled.Radar, contentDescription = appStrings.liveLoads) },
        label = { Text(appStrings.liveLoads) },
      )
      NavigationRailItem(
        selected = selectedTabIndex == 1,
        onClick = { onSelectTab(1) },
        icon = { Icon(Icons.Filled.Route, contentDescription = appStrings.myTrips) },
        label = { Text(appStrings.myTrips) },
      )
      NavigationRailItem(
        selected = selectedTabIndex == 2,
        onClick = { onSelectTab(2) },
        icon = {
          NotificationBadgedIcon(selected = selectedTabIndex == 2, unreadCount = unreadCount)
        },
        label = { Text(appStrings.alerts) },
      )
      NavigationRailItem(
        selected = selectedTabIndex == 3,
        onClick = { onSelectTab(3) },
        icon = { Icon(Icons.Filled.Person, contentDescription = appStrings.profile) },
        label = { Text(appStrings.profile) },
      )
    }
  }
}

@Composable
private fun NotificationBadgedIcon(
  selected: Boolean,
  unreadCount: Int,
) {
  BadgedBox(
    badge = {
      if (unreadCount > 0) {
        Badge { Text(unreadCount.toString()) }
      }
    }
  ) {
    Icon(
      imageVector = if (selected) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsActive,
      contentDescription = "Notifications",
    )
  }
}
