package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.Bid
import com.example.data.model.DriverTripEntry
import com.example.data.model.FleetVehicle
import com.example.data.model.LoadPost
import com.example.data.model.MatchedLoadItem
import com.example.data.model.UnlockedDealContacts
import com.example.data.model.UserPrivateKyc
import com.example.data.model.UserRole
import com.example.data.model.UserRoleProfile
import com.example.data.repository.LogisticsRepository
import com.example.data.util.GeoAndPrivacyUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

/**
 * Default feed filter shows ALL live loads posted in the market by others (`sizeFeetFilter = null`,
 * `bodyTypeFilter = null`), allowing truckers/vehicle owners to optionally apply filters later.
 */
data class LoadFeedFilter(
  val searchQuery: String = "",
  val sizeFeetFilter: Int? = null,
  val bodyTypeFilter: String? = null, // null = All, "OPEN", "CONTAINER"
)

class LogisticsViewModel(
  private val repository: LogisticsRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val _notifiedMatchLoadIds = mutableSetOf<String>()

  val roleProfileState: StateFlow<UiState<UserRoleProfile?>> =
    repository
      .observeUserRoleProfile(currentUserId)
      .map<UserRoleProfile?, UiState<UserRoleProfile?>> { UiState.Success(it) }
      .catch { e ->
        Log.w(TAG, "Error observing user role profile", e)
        emit(UiState.Error(e.localizedMessage ?: "Failed to load profile"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
      )

  private val _privateKyc = MutableStateFlow<UserPrivateKyc?>(null)
  val privateKyc: StateFlow<UserPrivateKyc?> = _privateKyc.asStateFlow()

  private val _feedFilter = MutableStateFlow(LoadFeedFilter())
  val feedFilter: StateFlow<LoadFeedFilter> = _feedFilter.asStateFlow()

  private val _selectedLoad = MutableStateFlow<LoadPost?>(null)
  val selectedLoad: StateFlow<LoadPost?> = _selectedLoad.asStateFlow()

  private val _unlockedContactsMap = MutableStateFlow<Map<String, UnlockedDealContacts>>(emptyMap())
  val unlockedContactsMap: StateFlow<Map<String, UnlockedDealContacts>> =
    _unlockedContactsMap.asStateFlow()

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  // Live Active Marketplace Loads Stream
  private val rawActiveLoadsFlow =
    repository
      .observeActiveMarketplaceLoads()
      .map<List<LoadPost>, UiState<List<LoadPost>>> { UiState.Success(it) }
      .catch { e ->
        Log.w(TAG, "Error observing active loads", e)
        emit(UiState.Error(e.localizedMessage ?: "Failed to load active marketplace"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
      )

  /**
   * Real-Time Driver Alerts Stream for newly posted live loads from other shippers.
   */
  val strictAlertMatchedLoadsState: StateFlow<List<MatchedLoadItem>> =
    combine(rawActiveLoadsFlow, roleProfileState) { loadsState, profileState ->
      val loads = (loadsState as? UiState.Success)?.data ?: return@combine emptyList()
      val profile = (profileState as? UiState.Success)?.data ?: return@combine emptyList()
      if (profile.role != UserRole.DRIVER.name || profile.isAvailable == false) {
        return@combine emptyList()
      }
      val now = System.currentTimeMillis()
      val driverLat = profile.currentLat
      val driverLng = profile.currentLng
      val driverSize = profile.vehicleSizeFeet
      val driverBody = profile.vehicleBodyType

      loads
        .filter { load ->
          !load.isExpired(now) &&
            load.status in listOf("ACTIVE", "ACCEPTED") &&
            load.posterId != currentUserId
        }
        .map { load ->
          val distKm =
            if (driverLat != null && driverLng != null) {
              GeoAndPrivacyUtils.haversineDistanceKm(driverLat, driverLng, load.originLat, load.originLng)
            } else {
              load.resolvedRouteDistanceKm().toDouble()
            }
          MatchedLoadItem(load = load, distanceKm = distKm, isFleetMatch = true)
        }
        .filter { item ->
          val sizeOk = driverSize == null || item.load.requiredSizeFeet == driverSize
          val bodyOk = driverBody == null || item.load.requiredBodyType.equals(driverBody, ignoreCase = true)
          val radiusOk =
            driverLat == null || driverLng == null || item.distanceKm <= GeoAndPrivacyUtils.MAX_MATCH_RADIUS_KM
          sizeOk && bodyOk && radiusOk
        }
        .sortedByDescending { it.load.createdAt?.seconds ?: 0L }
    }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = emptyList(),
      )

  /**
   * Truckers / Vehicle Owners Live Loads Feed:
   * Shows ALL live loads posted in the market by others by default (`load.posterId != currentUserId`),
   * and lets truckers optionally apply vehicle size, body type, or search filters later.
   */
  val matchedDriverLoadsState: StateFlow<UiState<List<MatchedLoadItem>>> =
    combine(rawActiveLoadsFlow, roleProfileState, _feedFilter) { loadsState, profileState, filter ->
      when (loadsState) {
        is UiState.Loading -> UiState.Loading
        is UiState.Error -> UiState.Error(loadsState.message)
        is UiState.Success -> {
          val profile = (profileState as? UiState.Success)?.data
          val driverLat = profile?.currentLat
          val driverLng = profile?.currentLng
          val driverSize = profile?.vehicleSizeFeet
          val driverBody = profile?.vehicleBodyType
          val now = System.currentTimeMillis()

          val matched =
            loadsState.data
              .filter { load ->
                !load.isExpired(now) &&
                  load.status in listOf("ACTIVE", "ACCEPTED") &&
                  load.posterId != currentUserId
              }
              .map { load ->
                val distKm =
                  if (driverLat != null && driverLng != null) {
                    GeoAndPrivacyUtils.haversineDistanceKm(
                      driverLat,
                      driverLng,
                      load.originLat,
                      load.originLng,
                    )
                  } else {
                    load.resolvedRouteDistanceKm().toDouble()
                  }

                val isFeetMatch = driverSize != null && load.requiredSizeFeet == driverSize
                val isBodyMatch =
                  driverBody != null && load.requiredBodyType.equals(driverBody, ignoreCase = true)
                val isRadiusMatch = distKm <= GeoAndPrivacyUtils.MAX_MATCH_RADIUS_KM
                val isFleetMatch = isFeetMatch && isBodyMatch && isRadiusMatch

                if (isFleetMatch && _notifiedMatchLoadIds.add(load.id)) {
                  viewModelScope.launch {
                    repository.dispatchMatchingLoadNotification(
                      recipientUid = currentUserId,
                      load = load,
                      vehicleFeet = driverSize ?: load.requiredSizeFeet,
                      vehicleBodyType = driverBody ?: load.requiredBodyType,
                      vehicleCity = profile?.currentCity ?: "your location",
                      distanceKm = distKm,
                    )
                  }
                }

                MatchedLoadItem(load = load, distanceKm = distKm, isFleetMatch = isFleetMatch)
              }
              .filter { item ->
                val query = filter.searchQuery.trim().lowercase()
                val matchesSearch =
                  query.isEmpty() ||
                    item.load.originCity.lowercase().contains(query) ||
                    item.load.destinationCity.lowercase().contains(query) ||
                    item.load.materialType.lowercase().contains(query) ||
                    (item.load.originAddress?.lowercase()?.contains(query) == true) ||
                    (item.load.destinationAddress?.lowercase()?.contains(query) == true)

                val matchesFilterSize =
                  filter.sizeFeetFilter == null || item.load.requiredSizeFeet == filter.sizeFeetFilter

                val matchesFilterBody =
                  filter.bodyTypeFilter == null ||
                    item.load.requiredBodyType.equals(filter.bodyTypeFilter, ignoreCase = true)

                matchesSearch && matchesFilterSize && matchesFilterBody
              }
              .sortedWith(
                compareByDescending<MatchedLoadItem> { it.isFleetMatch }
                  .thenByDescending { it.load.createdAt?.seconds ?: 0L }
              )

          UiState.Success(matched)
        }
      }
    }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
      )

  val posterLoadsState: StateFlow<UiState<List<LoadPost>>> =
    repository
      .observePosterLoads(currentUserId)
      .map<List<LoadPost>, UiState<List<LoadPost>>> { UiState.Success(it) }
      .catch { e ->
        Log.w(TAG, "Error observing poster loads", e)
        emit(UiState.Error(e.localizedMessage ?: "Failed to load your shipments"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
      )

  val driverTripsState: StateFlow<UiState<List<DriverTripEntry>>> =
    repository
      .observeDriverTrips(currentUserId)
      .map<List<DriverTripEntry>, UiState<List<DriverTripEntry>>> { UiState.Success(it) }
      .catch { e ->
        Log.w(TAG, "Error observing my trips", e)
        emit(UiState.Error(e.localizedMessage ?: "Failed to load My Trips"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
      )

  val notificationsState: StateFlow<UiState<List<AppNotification>>> =
    repository
      .observeUserNotifications(currentUserId)
      .map<List<AppNotification>, UiState<List<AppNotification>>> { UiState.Success(it) }
      .catch { e ->
        Log.w(TAG, "Error observing notifications", e)
        emit(UiState.Error(e.localizedMessage ?: "Failed to load notifications"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
      )

  @OptIn(ExperimentalCoroutinesApi::class)
  val selectedLoadBidsState: StateFlow<UiState<List<Bid>>> =
    _selectedLoad
      .flatMapLatest { load ->
        if (load == null) {
          flowOf(UiState.Success(emptyList()))
        } else {
          val stream =
            if (load.posterId == currentUserId) {
              repository.observeBidsForPoster(load.id, currentUserId)
            } else {
              repository.observeLeaderboardForDrivers(load.id)
            }
          stream
            .map<List<Bid>, UiState<List<Bid>>> { UiState.Success(it) }
            .catch { e ->
              Log.w(TAG, "Error observing bids for load ${load.id}", e)
              emit(UiState.Error(e.localizedMessage ?: "Unable to load bidding leaderboard"))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Success(emptyList()),
      )

  init {
    refreshPrivateKyc()
  }

  fun refreshPrivateKyc() {
    viewModelScope.launch {
      try {
        _privateKyc.value = repository.getPrivateKyc(currentUserId)
      } catch (e: Exception) {
        Log.w(TAG, "Private KYC not yet initialized", e)
      }
    }
  }

  fun updateFeedFilter(transform: (LoadFeedFilter) -> LoadFeedFilter) {
    _feedFilter.update(transform)
  }

  fun selectLoadForBidding(load: LoadPost?) {
    _selectedLoad.value = load
    if (load != null) {
      viewModelScope.launch {
        val fresh = repository.getLoadById(load.id) ?: load
        if (_selectedLoad.value?.id == load.id) {
          _selectedLoad.value = fresh
        }
        if (
          fresh.acceptedCount() > 0 &&
            (fresh.posterId == currentUserId || fresh.isDriverAccepted(currentUserId))
        ) {
          loadUnlockedDealContacts(fresh, forceRefresh = true)
        }
      }
    }
  }

  fun clearMessage() {
    _actionMessage.value = null
  }

  fun submitPosterOnboarding(
    companyName: String,
    phoneNumber: String,
    onSuccess: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.savePosterOnboarding(companyName, phoneNumber)
      _isBusy.value = false
      res
        .onSuccess {
          refreshPrivateKyc()
          _actionMessage.value = "✅ Profile saved! Ready to post loads."
          onSuccess()
        }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Failed to save profile."
        }
    }
  }

  fun submitDriverKycAndVehicle(
    fullName: String,
    phoneNumber: String,
    drivingLicenseNumber: String,
    onSuccess: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res =
        repository.saveDriverKycAndVehicleProfile(
          fullName = fullName,
          phoneNumber = phoneNumber,
          drivingLicenseNumber = drivingLicenseNumber,
          isAvailable = true,
        )
      _isBusy.value = false
      res
        .onSuccess {
          refreshPrivateKyc()
          _actionMessage.value = "✅ Profile activated! Viewing all live loads."
          onSuccess()
        }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Failed to save profile."
        }
    }
  }

  fun updateDriverAvailability(isAvailable: Boolean) {
    viewModelScope.launch {
      repository
        .updateDriverAvailabilityAndLocation(isAvailable = isAvailable)
        .onSuccess {
          _actionMessage.value =
            if (isAvailable) "🔔 Live load alerts active" else "🔕 Alerts paused"
        }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Failed to update status"
        }
    }
  }

  fun createNewLoadPost(
    posterCompanyName: String,
    posterDirectPhone: String,
    originCity: String,
    originAddress: String,
    originLat: Double,
    originLng: Double,
    destinationCity: String,
    destinationAddress: String,
    destLat: Double,
    destLng: Double,
    totalDistanceKm: Double,
    requiredSizeFeet: Int,
    requiredTonnage: Double,
    requiredBodyType: String,
    materialType: String,
    onSuccess: () -> Unit,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res =
        repository.createLoadPost(
          posterCompanyName = posterCompanyName,
          posterDirectPhone = posterDirectPhone,
          originCity = originCity,
          originAddress = originAddress,
          originLat = originLat,
          originLng = originLng,
          destinationCity = destinationCity,
          destinationAddress = destinationAddress,
          destLat = destLat,
          destLng = destLng,
          totalDistanceKm = totalDistanceKm,
          requiredSizeFeet = requiredSizeFeet,
          requiredTonnage = requiredTonnage,
          requiredBodyType = requiredBodyType,
          materialType = materialType,
        )
      _isBusy.value = false
      res
        .onSuccess {
          _actionMessage.value = "🚛 Load posted! Finding ${requiredSizeFeet}ft trucks now."
          onSuccess()
        }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Failed to post load."
        }
    }
  }

  fun placeDriverBid(
    load: LoadPost,
    bidAmount: Double,
    driverContactPhone: String,
    onSuccess: () -> Unit = {},
  ) {
    viewModelScope.launch {
      val profile = (roleProfileState.value as? UiState.Success)?.data
      val kyc = _privateKyc.value ?: runCatching { repository.getPrivateKyc(currentUserId) }.getOrNull()
      if (profile == null) {
        _actionMessage.value = "Please complete your Profile before bidding."
        return@launch
      }
      val cleanPhone = driverContactPhone.trim()
      val digitCount = cleanPhone.count { it.isDigit() }
      if (digitCount !in 10..13 || cleanPhone.length !in 10..16) {
        _actionMessage.value = "Please enter a valid 10-digit contact number to place your bid."
        return@launch
      }
      _isBusy.value = true
      val vehicleSummary =
        if (profile.vehicleSizeFeet != null) {
          "${profile.vehicleSizeFeet}ft ${profile.vehicleBodyType ?: "TRUCK"}"
        } else {
          "${load.requiredSizeFeet}ft ${load.requiredBodyType}"
        }
      val resolvedPlate =
        kyc?.vehicleNumberFull?.takeIf { it.length >= 4 }
          ?: kyc?.drivingLicenseNumber?.takeIf { it.length >= 4 }
          ?: "VERIFIED-TRUCK"

      val res =
        repository.placeBlindBid(
          load = load,
          driverDisplayName = profile.displayName,
          driverDirectPhone = cleanPhone,
          driverRating = profile.rating,
          vehicleDetails = vehicleSummary,
          fullVehicleNumber = resolvedPlate,
          bidAmount = bidAmount,
          availabilityTime = "Ready Now",
        )
      _isBusy.value = false
      res
        .onSuccess {
          _actionMessage.value =
            "✅ Bid of ₹${bidAmount.toInt()} placed! You will be notified once accepted."
          onSuccess()
        }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Failed to place bid."
        }
    }
  }

  fun acceptWinningBid(
    load: LoadPost,
    winningBid: Bid,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.acceptBidTransaction(load.id, winningBid)
      _isBusy.value = false
      res
        .onSuccess { unlocked ->
          _unlockedContactsMap.update { it + (load.id to unlocked) }
          val refreshed =
            repository.getLoadById(load.id)
              ?: load.copy(
                status = "ACCEPTED",
                acceptedBidId = winningBid.id,
                winningDriverId = winningBid.driverId,
                acceptedBidIds = (load.acceptedBidIds + winningBid.id).distinct(),
                acceptedDriverIds = (load.acceptedDriverIds + winningBid.driverId).distinct(),
              )
          _selectedLoad.value = refreshed
          _actionMessage.value =
            "✅ Bid Accepted (${refreshed.acceptedCount()} accepted)! Direct contact details unlocked."
        }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Failed to accept bid."
        }
    }
  }

  fun markLoadStatus(
    load: LoadPost,
    targetStatus: String,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.updateLoadFinalStatus(load, targetStatus)
      _isBusy.value = false
      res
        .onSuccess {
          _selectedLoad.update { cur ->
            if (cur?.id == load.id) cur.copy(status = targetStatus) else cur
          }
          _actionMessage.value =
            if (targetStatus == "BOOKED") {
              "✅ Load marked as BOOKED! Status updated & bidders notified."
            } else {
              "❌ Load marked as CANCELLED! Status updated & bidders notified."
            }
        }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Failed to update load status."
        }
    }
  }

  fun loadUnlockedDealContacts(
    load: LoadPost,
    forceRefresh: Boolean = false,
  ) {
    if (!forceRefresh && _unlockedContactsMap.value.containsKey(load.id)) return
    viewModelScope.launch {
      repository
        .fetchUnlockedContactsForAcceptedLoad(load)
        .onSuccess { unlocked ->
          _unlockedContactsMap.update { it + (load.id to unlocked) }
        }
    }
  }

  fun markNotificationRead(notificationId: String) {
    viewModelScope.launch {
      repository.markNotificationRead(notificationId)
    }
  }

  fun clearAllNotifications() {
    viewModelScope.launch {
      _isBusy.value = true
      val res = repository.clearAllNotifications()
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "🗑️ All notifications cleared."
      }.onFailure { err ->
        _actionMessage.value = err.localizedMessage ?: "Failed to clear notifications."
      }
    }
  }

  fun deleteNotification(notificationId: String) {
    viewModelScope.launch {
      repository.deleteNotification(notificationId)
    }
  }

  val driverFleetState: StateFlow<List<FleetVehicle>> =
    if (currentUserId.isNotBlank()) {
      repository
        .observeFleetVehicles(currentUserId)
        .catch { e ->
          Log.w(TAG, "Error observing driver fleet", e)
          emit(emptyList())
        }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
          initialValue = emptyList(),
        )
    } else {
      MutableStateFlow(emptyList())
    }

  fun saveDriverFleetVehicle(
    vehicleSizeFeet: Int,
    vehicleBodyType: String,
    vehicleCity: String,
    onSuccess: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val res =
        repository.saveFleetVehicle(
          vehicleSizeFeet = vehicleSizeFeet,
          vehicleBodyType = vehicleBodyType,
          vehicleCity = vehicleCity,
        )
      _isBusy.value = false
      res.onSuccess {
        _actionMessage.value = "✅ Fleet vehicle saved: ${vehicleSizeFeet}ft $vehicleBodyType in $vehicleCity"
        onSuccess()
      }.onFailure { err ->
        _actionMessage.value = err.localizedMessage ?: "Failed to save vehicle details."
      }
    }
  }

  private companion object {
    const val TAG = "LogisticsVM"
    const val STOP_TIMEOUT_MS = 5000L
  }
}
