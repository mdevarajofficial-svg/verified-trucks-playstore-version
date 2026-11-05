package com.example.data.model

import com.example.data.util.GeoAndPrivacyUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.roundToInt

enum class UserRole {
  POSTER,
  DRIVER,
}

enum class VehicleBodyType(val label: String) {
  OPEN("Open Body"),
  CONTAINER("Closed Container"),
}

@IgnoreExtraProperties
data class UserRoleProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val role: String = UserRole.DRIVER.name,
  val displayName: String = "",
  val kycVerified: Boolean = false,
  val rating: Double = 4.8,
  val vehicleNumberMasked: String? = null,
  val vehicleSizeFeet: Int? = null,
  val vehicleCapacityTons: Double? = null,
  val vehicleBodyType: String? = null,
  val isAvailable: Boolean? = true,
  val currentCity: String? = null,
  val currentLat: Double? = null,
  val currentLng: Double? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class UserPrivateKyc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val fullName: String = "",
  val phoneNumber: String = "",
  val maskedPhone: String = "XXXXXX0000",
  val drivingLicenseNumber: String? = null,
  val panNumber: String? = null,
  val aadhaarRedacted: String? = null,
  val vehicleNumberFull: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class LoadPost(
  @DocumentId val id: String = "",
  val posterId: String = "",
  val posterCompanyName: String = "",
  val maskedPosterPhone: String = "XXXXXX0000",
  val originCity: String = "",
  val originAddress: String? = null,
  val originLat: Double = 19.0760,
  val originLng: Double = 72.8777,
  val destinationCity: String = "",
  val destinationAddress: String? = null,
  val destLat: Double = 18.5204,
  val destLng: Double = 73.8567,
  val totalDistanceKm: Double? = null,
  val loadingWaypoints: List<String> = emptyList(),
  val requiredSizeFeet: Int = 32,
  val requiredTonnage: Double = 15.0,
  val requiredBodyType: String = VehicleBodyType.CONTAINER.name,
  val materialType: String = "",
  val loadingDate: String = "",
  val loadingTime: String = "",
  val expectedBudgetInr: Double? = null,
  val status: String = "ACTIVE",
  val acceptedBidId: String? = null,
  val winningDriverId: String? = null,
  val acceptedBidIds: List<String> = emptyList(),
  val acceptedDriverIds: List<String> = emptyList(),
  val bidCount: Int = 0,
  val lowestBidAmount: Double? = null,
  val expiresAtEpochMillis: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun isExpired(nowMillis: Long = System.currentTimeMillis()): Boolean {
    return (status == "ACTIVE" && expiresAtEpochMillis in 1..nowMillis) || status == "EXPIRED"
  }

  fun isDriverAccepted(driverUid: String): Boolean {
    return winningDriverId == driverUid || acceptedDriverIds.contains(driverUid)
  }

  fun acceptedCount(): Int {
    return when {
      acceptedBidIds.isNotEmpty() -> acceptedBidIds.size
      !acceptedBidId.isNullOrBlank() -> 1
      else -> 0
    }
  }

  fun resolvedRouteDistanceKm(): Int {
    val stored = totalDistanceKm
    if (stored != null && stored > 0.0) {
      return stored.roundToInt().coerceAtLeast(1)
    }
    val straight = GeoAndPrivacyUtils.haversineDistanceKm(originLat, originLng, destLat, destLng)
    return (straight * 1.21).roundToInt().coerceAtLeast(1)
  }
}

@IgnoreExtraProperties
data class LoadContactVault(
  @DocumentId val id: String = "",
  val loadId: String = "",
  val posterId: String = "",
  val posterDirectPhone: String = "",
  val exactPickupAddress: String = "",
  val authorizedDriverId: String? = null,
  val authorizedDriverIds: List<String> = emptyList(),
  val winningDriverDirectPhone: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Bid(
  @DocumentId val id: String = "",
  val loadId: String = "",
  val posterId: String = "",
  val driverId: String = "",
  val driverAnonymousAlias: String = "",
  val driverDisplayName: String = "",
  val maskedDriverPhone: String = "XXXXXX0000",
  val driverRating: Double = 4.8,
  val vehicleDetails: String = "",
  val maskedVehicleNumber: String = "",
  val bidAmount: Double = 0.0,
  val availabilityTime: String = "",
  val status: String = "PENDING",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class BidPrivateContact(
  @DocumentId val id: String = "",
  val bidId: String = "",
  val loadId: String = "",
  val driverId: String = "",
  val posterId: String = "",
  val driverDirectPhone: String = "",
  val fullVehicleNumber: String = "",
  val driverFullName: String = "",
  val bidAmount: Double = 0.0,
  val unlockedForPoster: Boolean = false,
  @ServerTimestamp val updatedAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class DriverTripEntry(
  @DocumentId val id: String = "",
  val tripId: String = "",
  val loadId: String = "",
  val bidId: String = "",
  val posterId: String = "",
  val posterCompanyName: String = "",
  val driverId: String = "",
  val driverDisplayName: String = "",
  val originCity: String = "",
  val destinationCity: String = "",
  val totalDistanceKm: Double = 0.0,
  val requiredSizeFeet: Int = 10,
  val requiredTonnage: Double = 2.0,
  val requiredBodyType: String = VehicleBodyType.OPEN.name,
  val materialType: String = "",
  val bidAmount: Double = 0.0,
  val bidStatus: String = "PENDING",
  val loadStatus: String = "ACTIVE",
  val unlockedPosterPhone: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toLoadPost(): LoadPost {
    return LoadPost(
      id = loadId,
      posterId = posterId,
      posterCompanyName = posterCompanyName,
      originCity = originCity,
      destinationCity = destinationCity,
      totalDistanceKm = totalDistanceKm,
      requiredSizeFeet = requiredSizeFeet,
      requiredTonnage = requiredTonnage,
      requiredBodyType = requiredBodyType,
      materialType = materialType,
      status = loadStatus,
      acceptedBidId = if (bidStatus == "ACCEPTED") bidId else null,
      winningDriverId = if (bidStatus == "ACCEPTED") driverId else null,
      acceptedBidIds = if (bidStatus == "ACCEPTED") listOf(bidId) else emptyList(),
      acceptedDriverIds = if (bidStatus == "ACCEPTED") listOf(driverId) else emptyList(),
      createdAt = createdAt,
      updatedAt = updatedAt,
    )
  }
}

@IgnoreExtraProperties
data class AppNotification(
  @DocumentId val id: String = "",
  val recipientUid: String = "",
  val senderUid: String = "",
  val title: String = "",
  val message: String = "",
  val type: String = "SYSTEM",
  val loadId: String = "",
  val bidId: String? = null,
  val unlockedPhone: String? = null,
  val unlockedPickupAddress: String? = null,
  val unlockedVehicleNumber: String? = null,
  val isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class MatchedLoadItem(
  val load: LoadPost,
  val distanceKm: Double,
  val isFleetMatch: Boolean = false,
)

@IgnoreExtraProperties
data class FleetVehicle(
  @DocumentId val id: String = "",
  val userId: String = "",
  val vehicleSizeFeet: Int = 32,
  val vehicleBodyType: String = VehicleBodyType.CONTAINER.name,
  val vehicleCity: String = "",
  val vehicleLat: Double? = null,
  val vehicleLng: Double? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class AcceptedBidderContact(
  val bidId: String,
  val driverId: String,
  val driverFullName: String,
  val driverDirectPhone: String,
  val bidAmount: Double,
  val fullVehicleNumber: String = "",
)

data class UnlockedDealContacts(
  val loadId: String,
  val posterDirectPhone: String? = null,
  val posterCompanyName: String? = null,
  val exactPickupAddress: String? = null,
  val acceptedBidders: List<AcceptedBidderContact> = emptyList(),
  val confirmationBanner: String = "✅ Bid Accepted! Direct contact details unlocked.",
)
