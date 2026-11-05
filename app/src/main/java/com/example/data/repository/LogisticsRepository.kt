package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AcceptedBidderContact
import com.example.data.model.AppNotification
import com.example.data.model.Bid
import com.example.data.model.BidPrivateContact
import com.example.data.model.DriverTripEntry
import com.example.data.model.LoadContactVault
import com.example.data.model.LoadPost
import com.example.data.model.UnlockedDealContacts
import com.example.data.model.UserPrivateKyc
import com.example.data.model.UserRole
import com.example.data.model.UserRoleProfile
import com.example.data.util.GeoAndPrivacyUtils
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LogisticsRepository(
  private val db: FirebaseFirestore,
) {
  constructor(
    context: Context,
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  // --- 1. User Role & Driver Profile ---

  fun observeUserRoleProfile(userId: String): Flow<UserRoleProfile?> {
    val docRef = db.collection("users").document(userId)
    return docRef
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(UserRoleProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
        throw error
      }
  }

  suspend fun getPrivateKyc(userId: String): UserPrivateKyc? {
    val docRef = db.collection("users").document(userId).collection("private_kyc").document("info")
    return try {
      val snap = docRef.get().await()
      if (snap.exists()) {
        snap.toObject(UserPrivateKyc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      } else {
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  suspend fun savePosterOnboarding(
    companyName: String,
    phoneNumber: String,
  ): Result<Unit> {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val kycRef = userRef.collection("private_kyc").document("info")
    val maskedPhone = GeoAndPrivacyUtils.maskPhoneStrict(phoneNumber)

    return try {
      val existingSnap = userRef.get().await()
      val batch = db.batch()
      if (existingSnap.exists()) {
        batch.update(
          userRef,
          mapOf(
            "role" to UserRole.POSTER.name,
            "displayName" to companyName.trim(),
            "kycVerified" to true,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      } else {
        batch.set(
          userRef,
          mapOf(
            "userId" to uid,
            "role" to UserRole.POSTER.name,
            "displayName" to companyName.trim(),
            "kycVerified" to true,
            "rating" to 4.9,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      val existingKyc = kycRef.get().await()
      if (existingKyc.exists()) {
        batch.update(
          kycRef,
          mapOf(
            "fullName" to companyName.trim(),
            "phoneNumber" to phoneNumber.trim(),
            "maskedPhone" to maskedPhone,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      } else {
        batch.set(
          kycRef,
          mapOf(
            "userId" to uid,
            "fullName" to companyName.trim(),
            "phoneNumber" to phoneNumber.trim(),
            "maskedPhone" to maskedPhone,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, userRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveDriverKycAndVehicleProfile(
    fullName: String,
    phoneNumber: String,
    drivingLicenseNumber: String = "",
    panNumber: String? = null,
    rawOrRedactedAadhaar: String? = null,
    vehicleSizeFeet: Int? = null,
    vehicleCapacityTons: Double? = null,
    vehicleBodyType: String? = null,
    vehicleNumberFull: String? = null,
    currentCity: String? = null,
    currentLat: Double? = null,
    currentLng: Double? = null,
    isAvailable: Boolean = true,
  ): Result<Unit> {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val kycRef = userRef.collection("private_kyc").document("info")
    val maskedPhone = GeoAndPrivacyUtils.maskPhoneStrict(phoneNumber)

    return try {
      val existingUser = userRef.get().await()
      val existingKyc = kycRef.get().await()
      val batch = db.batch()

      val publicMap =
        mutableMapOf<String, Any>(
          "role" to UserRole.DRIVER.name,
          "displayName" to fullName.trim(),
          "kycVerified" to true,
          "isAvailable" to isAvailable,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (!vehicleNumberFull.isNullOrBlank()) {
        publicMap["vehicleNumberMasked"] = GeoAndPrivacyUtils.maskVehicleNumber(vehicleNumberFull)
      }
      if (vehicleSizeFeet != null) {
        publicMap["vehicleSizeFeet"] = vehicleSizeFeet
      }
      if (vehicleCapacityTons != null) {
        publicMap["vehicleCapacityTons"] = vehicleCapacityTons
      }
      if (!vehicleBodyType.isNullOrBlank()) {
        publicMap["vehicleBodyType"] = vehicleBodyType
      }
      if (!currentCity.isNullOrBlank()) {
        publicMap["currentCity"] = currentCity.trim().take(120)
      }
      if (currentLat != null) {
        publicMap["currentLat"] = currentLat
      }
      if (currentLng != null) {
        publicMap["currentLng"] = currentLng
      }

      if (existingUser.exists()) {
        batch.update(userRef, publicMap)
      } else {
        publicMap["userId"] = uid
        publicMap["rating"] = 4.8
        publicMap["createdAt"] = FieldValue.serverTimestamp()
        batch.set(userRef, publicMap)
      }

      val privateMap =
        mutableMapOf<String, Any>(
          "fullName" to fullName.trim(),
          "phoneNumber" to phoneNumber.trim(),
          "maskedPhone" to maskedPhone,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (drivingLicenseNumber.isNotBlank()) {
        privateMap["drivingLicenseNumber"] = drivingLicenseNumber.trim().uppercase().take(30)
      }
      if (!panNumber.isNullOrBlank()) {
        privateMap["panNumber"] = panNumber.trim().uppercase().take(20)
      }
      if (!rawOrRedactedAadhaar.isNullOrBlank()) {
        privateMap["aadhaarRedacted"] = GeoAndPrivacyUtils.redactAadhaar(rawOrRedactedAadhaar)
      }
      if (!vehicleNumberFull.isNullOrBlank()) {
        privateMap["vehicleNumberFull"] = vehicleNumberFull.trim().uppercase().take(25)
      }

      if (existingKyc.exists()) {
        batch.update(kycRef, privateMap)
      } else {
        privateMap["userId"] = uid
        privateMap["createdAt"] = FieldValue.serverTimestamp()
        batch.set(kycRef, privateMap)
      }

      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, userRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateDriverAvailabilityAndLocation(
    isAvailable: Boolean,
    city: String? = null,
    lat: Double? = null,
    lng: Double? = null,
  ): Result<Unit> {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    return try {
      val updates =
        mutableMapOf<String, Any>(
          "isAvailable" to isAvailable,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (!city.isNullOrBlank()) updates["currentCity"] = city.take(120)
      if (lat != null) updates["currentLat"] = lat
      if (lng != null) updates["currentLng"] = lng
      userRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteMyAccountAndKycData(): Result<Unit> {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val kycRef = userRef.collection("private_kyc").document("info")
    return try {
      val batch = db.batch()
      batch.delete(kycRef)
      batch.delete(userRef)
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, userRef.path)
      Result.failure(e)
    }
  }

  // --- 2. Load Posting ---

  suspend fun createLoadPost(
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
    loadingDate: String = "Immediate",
    loadingTime: String = "Now",
    expectedBudgetInr: Double? = null,
  ): Result<String> {
    val uid = requireUserId()
    val loadId = "load_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val loadRef = db.collection("loads").document(loadId)
    val vaultRef = loadRef.collection("vault").document("contact")

    val maskedPhone = GeoAndPrivacyUtils.maskPhoneStrict(posterDirectPhone)
    val expiresAtMillis = System.currentTimeMillis() + GeoAndPrivacyUtils.SIX_HOURS_MILLIS
    val cleanOriginCity = originCity.trim().take(140)
    val cleanDestCity = destinationCity.trim().take(140)
    val cleanOriginAddr = originAddress.trim().ifBlank { cleanOriginCity }.take(280)
    val cleanDestAddr = destinationAddress.trim().ifBlank { cleanDestCity }.take(280)
    val safeDate = loadingDate.trim().ifBlank { "Immediate" }.take(30)
    val safeTime = loadingTime.trim().ifBlank { "Now" }.take(30)

    val loadPayload =
      mutableMapOf<String, Any>(
        "posterId" to uid,
        "posterCompanyName" to posterCompanyName.trim(),
        "maskedPosterPhone" to maskedPhone,
        "originCity" to cleanOriginCity,
        "originAddress" to cleanOriginAddr,
        "originLat" to originLat,
        "originLng" to originLng,
        "destinationCity" to cleanDestCity,
        "destinationAddress" to cleanDestAddr,
        "destLat" to destLat,
        "destLng" to destLng,
        "totalDistanceKm" to totalDistanceKm.coerceIn(1.0, 15000.0),
        "loadingWaypoints" to emptyList<String>(),
        "requiredSizeFeet" to requiredSizeFeet,
        "requiredTonnage" to requiredTonnage,
        "requiredBodyType" to requiredBodyType,
        "materialType" to materialType.trim(),
        "loadingDate" to safeDate,
        "loadingTime" to safeTime,
        "status" to "ACTIVE",
        "acceptedBidIds" to emptyList<String>(),
        "acceptedDriverIds" to emptyList<String>(),
        "bidCount" to 0,
        "expiresAtEpochMillis" to expiresAtMillis,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    if (expectedBudgetInr != null && expectedBudgetInr > 0) {
      loadPayload["expectedBudgetInr"] = expectedBudgetInr
    }

    val vaultPayload =
      mapOf(
        "loadId" to loadId,
        "posterId" to uid,
        "posterDirectPhone" to posterDirectPhone.trim(),
        "exactPickupAddress" to cleanOriginAddr,
        "authorizedDriverIds" to emptyList<String>(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )

    return try {
      val batch = db.batch()
      batch.set(loadRef, loadPayload)
      batch.set(vaultRef, vaultPayload)
      batch.commit().await()
      Result.success(loadId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, loadRef.path)
      Result.failure(e)
    }
  }

  /**
   * Observes live marketplace loads (`status in ['ACTIVE', 'ACCEPTED']` with fewer than 3 accepted bids).
   * Once marked `BOOKED` or `CANCELLED`, loads immediately leave the live market feed.
   */
  fun observeActiveMarketplaceLoads(): Flow<List<LoadPost>> {
    val path = "loads"
    return db
      .collection(path)
      .whereIn("status", listOf("ACTIVE", "ACCEPTED"))
      .snapshots()
      .map { snapshot ->
        val now = System.currentTimeMillis()
        snapshot
          .toObjects(LoadPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .filter { !it.isExpired(now) && it.status in listOf("ACTIVE", "ACCEPTED") }
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  /**
   * Observes loads posted by a specific Load Poster (`posterId == userId`), including
   * `ACTIVE`, `ACCEPTED`, `BOOKED`, and `CANCELLED` deals.
   */
  fun observePosterLoads(posterUid: String): Flow<List<LoadPost>> {
    val path = "loads"
    return db
      .collection(path)
      .whereEqualTo("posterId", posterUid)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(LoadPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  /**
   * Observes all trips in "My Trips" where a Driver placed a bid (`driverId == driverUid`),
   * reflecting PENDING / ACCEPTED bid status, unlocked Load Poster contact when accepted,
   * and real-time BOOKED / CANCELLED status chosen by the Load Poster.
   */
  fun observeDriverTrips(driverUid: String): Flow<List<DriverTripEntry>> {
    val path = "driver_trips"
    return db
      .collection(path)
      .whereEqualTo("driverId", driverUid)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(DriverTripEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  /**
   * Fetches the latest LoadPost by ID (for opening full details from a DriverTripEntry).
   */
  suspend fun getLoadById(loadId: String): LoadPost? {
    val docRef = db.collection("loads").document(loadId)
    return try {
      val snap = docRef.get().await()
      if (snap.exists()) {
        snap.toObject(LoadPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      } else {
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      null
    }
  }

  // --- 3. Competitive Bidding (Sorted Lowest First) ---

  suspend fun placeBlindBid(
    load: LoadPost,
    driverDisplayName: String,
    driverDirectPhone: String,
    driverRating: Double,
    vehicleDetails: String,
    fullVehicleNumber: String,
    bidAmount: Double,
    availabilityTime: String = "Ready Now",
  ): Result<String> {
    val driverUid = requireUserId()
    val bidId = "bid_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val tripId = "${driverUid}_${load.id}"
    val loadRef = db.collection("loads").document(load.id)
    val bidRef = loadRef.collection("bids").document(bidId)
    val bidContactRef = loadRef.collection("bid_contacts").document(bidId)
    val bidPhoneRecordRef = db.collection("bid_phone_records").document(bidId)
    val driverTripRef = db.collection("driver_trips").document(tripId)
    val notifRef =
      db.collection("notifications")
        .document("notif_${UUID.randomUUID().toString().replace("-", "").take(16)}")

    val cleanDriverPhone = driverDirectPhone.trim()
    val maskedDriverPhone = GeoAndPrivacyUtils.maskPhoneStrict(cleanDriverPhone)
    val safeFullPlate = fullVehicleNumber.trim().ifBlank { "VERIFIED-TRUCK" }.take(25)
    val maskedPlate = GeoAndPrivacyUtils.maskVehicleNumber(safeFullPlate)
    val alias = GeoAndPrivacyUtils.anonymousOperatorAlias(driverUid)
    val safeVehicleDetails = vehicleDetails.trim().ifBlank { "Verified Partner" }.take(100)
    val safeAvailability = availabilityTime.trim().ifBlank { "Ready Now" }.take(60)

    return try {
      db.runTransaction { transaction ->
        val freshLoadSnap = transaction.get(loadRef)
        val existingTripSnap = transaction.get(driverTripRef)

        if (!freshLoadSnap.exists()) {
          throw IllegalStateException("Load no longer exists.")
        }
        val currentStatus = freshLoadSnap.getString("status") ?: "EXPIRED"
        val expiresAt = freshLoadSnap.getLong("expiresAtEpochMillis") ?: 0L
        if (currentStatus !in listOf("ACTIVE", "ACCEPTED") ||
          (currentStatus == "ACTIVE" && expiresAt in 1..System.currentTimeMillis())
        ) {
          throw IllegalStateException("Bidding closed: Load is $currentStatus.")
        }

        val posterId = freshLoadSnap.getString("posterId") ?: load.posterId
        val posterCompany =
          (freshLoadSnap.getString("posterCompanyName") ?: load.posterCompanyName)
            .trim()
            .ifBlank { "Verified Shipper" }
            .take(100)
        val originCity = (freshLoadSnap.getString("originCity") ?: load.originCity).trim().take(140)
        val destinationCity =
          (freshLoadSnap.getString("destinationCity") ?: load.destinationCity).trim().take(140)
        val totalKm = freshLoadSnap.getDouble("totalDistanceKm") ?: load.resolvedRouteDistanceKm().toDouble()
        val reqFeet = (freshLoadSnap.getLong("requiredSizeFeet") ?: load.requiredSizeFeet.toLong()).toInt()
        val reqTons = freshLoadSnap.getDouble("requiredTonnage") ?: load.requiredTonnage
        val reqBody = freshLoadSnap.getString("requiredBodyType") ?: load.requiredBodyType
        val material = (freshLoadSnap.getString("materialType") ?: load.materialType).trim().take(100)

        val currentBidCount = (freshLoadSnap.getLong("bidCount") ?: 0L).toInt()
        val currentLowest = freshLoadSnap.getDouble("lowestBidAmount")
        val updatedLowest =
          if (currentLowest == null || bidAmount < currentLowest) bidAmount else currentLowest

        transaction.set(
          bidRef,
          mapOf(
            "loadId" to load.id,
            "posterId" to posterId,
            "driverId" to driverUid,
            "driverAnonymousAlias" to alias,
            "driverDisplayName" to driverDisplayName.trim(),
            "maskedDriverPhone" to maskedDriverPhone,
            "driverRating" to driverRating.coerceIn(1.0, 5.0),
            "vehicleDetails" to safeVehicleDetails,
            "maskedVehicleNumber" to maskedPlate,
            "bidAmount" to bidAmount,
            "availabilityTime" to safeAvailability,
            "status" to "PENDING",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        transaction.set(
          bidContactRef,
          mapOf(
            "bidId" to bidId,
            "loadId" to load.id,
            "driverId" to driverUid,
            "posterId" to posterId,
            "driverDirectPhone" to cleanDriverPhone,
            "fullVehicleNumber" to safeFullPlate.uppercase(),
            "driverFullName" to driverDisplayName.trim(),
            "bidAmount" to bidAmount,
            "unlockedForPoster" to false,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )

        transaction.set(
          bidPhoneRecordRef,
          mapOf(
            "bidId" to bidId,
            "loadId" to load.id,
            "posterId" to posterId,
            "driverId" to driverUid,
            "driverFullName" to driverDisplayName.trim(),
            "driverFullPhone" to cleanDriverPhone,
            "bidAmount" to bidAmount,
            "originCity" to originCity,
            "destinationCity" to destinationCity,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )

        if (existingTripSnap.exists()) {
          transaction.update(
            driverTripRef,
            mapOf(
              "bidId" to bidId,
              "bidAmount" to bidAmount,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        } else {
          transaction.set(
            driverTripRef,
            mapOf(
              "tripId" to tripId,
              "loadId" to load.id,
              "bidId" to bidId,
              "posterId" to posterId,
              "posterCompanyName" to posterCompany,
              "driverId" to driverUid,
              "driverDisplayName" to driverDisplayName.trim().take(80),
              "originCity" to originCity,
              "destinationCity" to destinationCity,
              "totalDistanceKm" to totalKm.coerceIn(0.0, 15000.0),
              "requiredSizeFeet" to reqFeet.coerceIn(6, 80),
              "requiredTonnage" to reqTons.coerceIn(0.5, 100.0),
              "requiredBodyType" to reqBody,
              "materialType" to material.ifBlank { "General Cargo" },
              "bidAmount" to bidAmount,
              "bidStatus" to "PENDING",
              "loadStatus" to currentStatus,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }

        transaction.update(
          loadRef,
          mapOf(
            "bidCount" to (currentBidCount + 1),
            "lowestBidAmount" to updatedLowest,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        transaction.set(
          notifRef,
          mapOf(
            "recipientUid" to posterId,
            "senderUid" to driverUid,
            "title" to "💰 New Bid: ₹${bidAmount.toInt()}",
            "message" to "$alias placed a bid of ₹${bidAmount.toInt()} on $originCity → $destinationCity.",
            "type" to "NEW_BID",
            "loadId" to load.id,
            "bidId" to bidId,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()

      Result.success(bidId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, bidRef.path)
      Result.failure(e)
    }
  }

  /**
   * Leaderboard for Load Poster sorted strictly lowest bid amount first (L1 on top).
   */
  fun observeBidsForPoster(loadId: String, posterUid: String): Flow<List<Bid>> {
    val path = "loads/$loadId/bids"
    return db
      .collection("loads")
      .document(loadId)
      .collection("bids")
      .whereEqualTo("posterId", posterUid)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(Bid::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<Bid> { it.bidAmount }.thenBy { it.createdAt?.seconds ?: Long.MAX_VALUE })
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  /**
   * Leaderboard for Drivers sorted strictly lowest bid amount first (L1 on top).
   */
  fun observeLeaderboardForDrivers(loadId: String): Flow<List<Bid>> {
    val path = "loads/$loadId/bids"
    return db
      .collection("loads")
      .document(loadId)
      .collection("bids")
      .whereIn("status", listOf("PENDING", "ACCEPTED"))
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(Bid::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<Bid> { it.bidAmount }.thenBy { it.createdAt?.seconds ?: Long.MAX_VALUE })
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  // --- 4. Top-3 Bid Acceptance & Mutual Direct Contact Unlocking ---

  suspend fun acceptBidTransaction(
    loadId: String,
    winningBid: Bid,
  ): Result<UnlockedDealContacts> {
    val posterUid = requireUserId()
    val loadRef = db.collection("loads").document(loadId)
    val bidRef = loadRef.collection("bids").document(winningBid.id)
    val bidContactRef = loadRef.collection("bid_contacts").document(winningBid.id)
    val vaultRef = loadRef.collection("vault").document("contact")

    return try {
      // Enforce Top-3 Lowest Leaderboard Rank check before committing
      val allBidsSnap =
        loadRef
          .collection("bids")
          .whereEqualTo("posterId", posterUid)
          .get()
          .await()
      val rankedBids =
        allBidsSnap
          .toObjects(Bid::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<Bid> { it.bidAmount }.thenBy { it.createdAt?.seconds ?: Long.MAX_VALUE })

      val rankIndex = rankedBids.indexOfFirst { it.id == winningBid.id }
      if (rankIndex == -1) {
        return Result.failure(IllegalStateException("Bid not found on this load."))
      }

      val dealId = "${loadId}_${winningBid.id}"
      val acceptedDealRef = db.collection("accepted_deals").document(dealId)
      val driverTripRef = db.collection("driver_trips").document("${winningBid.driverId}_$loadId")
      val driverNotifRef =
        db.collection("notifications")
          .document("notif_${UUID.randomUUID().toString().replace("-", "").take(16)}")
      val posterNotifRef =
        db.collection("notifications")
          .document("notif_${UUID.randomUUID().toString().replace("-", "").take(16)}")

      db.runTransaction { transaction ->
        val loadSnap = transaction.get(loadRef)
        val bidSnap = transaction.get(bidRef)
        val vaultSnap = transaction.get(vaultRef)
        val driverTripSnap = transaction.get(driverTripRef)

        if (!loadSnap.exists() || !bidSnap.exists() || !vaultSnap.exists()) {
          throw IllegalStateException("Load, bid, or contact record missing.")
        }

        val ownerId = loadSnap.getString("posterId") ?: ""
        if (ownerId != posterUid) {
          throw IllegalAccessException("Only the Load Poster can accept bids.")
        }

        val currentLoadStatus = loadSnap.getString("status") ?: ""
        if (currentLoadStatus !in listOf("ACTIVE", "ACCEPTED")) {
          throw IllegalStateException("Cannot accept bids when load is $currentLoadStatus.")
        }

        val currentBidStatus = bidSnap.getString("status") ?: ""
        if (currentBidStatus != "PENDING") {
          throw IllegalStateException("This bid is already $currentBidStatus.")
        }

        @Suppress("UNCHECKED_CAST")
        val existingAcceptedBidIds =
          (loadSnap.get("acceptedBidIds") as? List<*>)
            ?.mapNotNull { it as? String }
            ?.toMutableList()
            ?: mutableListOf()
        val legacyBidId = loadSnap.getString("acceptedBidId")
        if (!legacyBidId.isNullOrBlank() && !existingAcceptedBidIds.contains(legacyBidId)) {
          existingAcceptedBidIds.add(legacyBidId)
        }

        @Suppress("UNCHECKED_CAST")
        val existingAcceptedDriverIds =
          (loadSnap.get("acceptedDriverIds") as? List<*>)
            ?.mapNotNull { it as? String }
            ?.toMutableList()
            ?: mutableListOf()
        val legacyDriverId = loadSnap.getString("winningDriverId")
        if (!legacyDriverId.isNullOrBlank() && !existingAcceptedDriverIds.contains(legacyDriverId)) {
          existingAcceptedDriverIds.add(legacyDriverId)
        }

        val winningDriverUid = bidSnap.getString("driverId") ?: winningBid.driverId
        val winningDriverName =
          (bidSnap.getString("driverDisplayName") ?: winningBid.driverDisplayName)
            .trim()
            .ifBlank { "Verified Operator" }
            .take(100)
        val acceptedBidAmount = bidSnap.getDouble("bidAmount") ?: winningBid.bidAmount
        val posterDirectPhone = (vaultSnap.getString("posterDirectPhone") ?: "").trim()
        val exactPickupAddress = (vaultSnap.getString("exactPickupAddress") ?: "").trim()
        val posterCompanyName =
          (loadSnap.getString("posterCompanyName") ?: "Verified Shipper").trim().take(100)
        val originCity = (loadSnap.getString("originCity") ?: "").trim().take(140)
        val destinationCity = (loadSnap.getString("destinationCity") ?: "").trim().take(140)

        val updatedAcceptedBidIds = (existingAcceptedBidIds + winningBid.id).distinct()
        val updatedAcceptedDriverIds = (existingAcceptedDriverIds + winningDriverUid).distinct()

        transaction.update(
          loadRef,
          mapOf(
            "status" to "ACCEPTED",
            "acceptedBidId" to winningBid.id,
            "winningDriverId" to winningDriverUid,
            "acceptedBidIds" to updatedAcceptedBidIds,
            "acceptedDriverIds" to updatedAcceptedDriverIds,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        transaction.update(
          bidRef,
          mapOf(
            "status" to "ACCEPTED",
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        transaction.update(
          bidContactRef,
          mapOf(
            "unlockedForPoster" to true,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        transaction.update(
          vaultRef,
          mapOf(
            "authorizedDriverId" to winningDriverUid,
            "authorizedDriverIds" to updatedAcceptedDriverIds,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        if (driverTripSnap.exists()) {
          transaction.update(
            driverTripRef,
            mapOf(
              "bidStatus" to "ACCEPTED",
              "loadStatus" to "ACCEPTED",
              "unlockedPosterPhone" to posterDirectPhone,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }

        transaction.set(
          acceptedDealRef,
          mapOf(
            "dealId" to dealId,
            "loadId" to loadId,
            "acceptedBidId" to winningBid.id,
            "posterId" to posterUid,
            "posterCompanyName" to posterCompanyName,
            "posterFullPhone" to posterDirectPhone,
            "winningDriverId" to winningDriverUid,
            "winningDriverName" to winningDriverName,
            "driverFullPhone" to null,
            "acceptedBidAmount" to acceptedBidAmount,
            "originCity" to originCity,
            "destinationCity" to destinationCity,
            "customerCarePhone" to VERIFIED_TRUCKS_HELPLINE,
            "verificationStatus" to "ACCEPTED",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        transaction.set(
          driverNotifRef,
          mapOf(
            "recipientUid" to winningDriverUid,
            "senderUid" to posterUid,
            "title" to "✅ Bid Accepted! Load Poster Contact Unlocked",
            "message" to
              "Your bid of ₹${acceptedBidAmount.toInt()} ($originCity → $destinationCity) was accepted by $posterCompanyName! Load Poster Phone: $posterDirectPhone",
            "type" to "BID_ACCEPTED",
            "loadId" to loadId,
            "bidId" to winningBid.id,
            "unlockedPhone" to posterDirectPhone,
            "unlockedPickupAddress" to exactPickupAddress,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        transaction.set(
          posterNotifRef,
          mapOf(
            "recipientUid" to posterUid,
            "senderUid" to posterUid,
            "title" to "✅ Bid Accepted! Driver Contact Unlocked",
            "message" to
              "Accepted $winningDriverName (₹${acceptedBidAmount.toInt()}) for $originCity → $destinationCity. View unlocked contact in deal details.",
            "type" to "BID_ACCEPTED",
            "loadId" to loadId,
            "bidId" to winningBid.id,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()

      // Fetch full unlocked contacts after transaction commits
      val refreshedLoad = getLoadById(loadId)
      if (refreshedLoad != null) {
        fetchUnlockedContactsForAcceptedLoad(refreshedLoad)
      } else {
        Result.failure(IllegalStateException("Unable to refresh load after accepting bid."))
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, loadRef.path)
      Result.failure(e)
    }
  }

  /**
   * Marks a load as BOOKED or CANCELLED by the Load Poster, updates all bidder entries in
   * `/driver_trips` so their My Trips screen immediately reflects BOOKED or CANCELLED,
   * and sends a real-time notification to BOTH the Load Poster and all bidding Vehicle Owners.
   */
  suspend fun updateLoadFinalStatus(
    load: LoadPost,
    targetStatus: String,
  ): Result<Unit> {
    val posterUid = requireUserId()
    require(targetStatus in listOf("BOOKED", "CANCELLED")) {
      "Status must be BOOKED or CANCELLED"
    }
    val loadRef = db.collection("loads").document(load.id)

    return try {
      val driverTripsSnap =
        db.collection("driver_trips")
          .whereEqualTo("posterId", posterUid)
          .get()
          .await()
      val loadTrips =
        driverTripsSnap.documents.filter { doc -> doc.getString("loadId") == load.id }

      val batch = db.batch()
      batch.update(
        loadRef,
        mapOf(
          "status" to targetStatus,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )

      val statusEmoji = if (targetStatus == "BOOKED") "✅" else "❌"
      val notifiedDrivers = mutableSetOf<String>()

      for (tripDoc in loadTrips) {
        batch.update(
          tripDoc.reference,
          mapOf(
            "loadStatus" to targetStatus,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        val bDriverUid = tripDoc.getString("driverId")
        if (!bDriverUid.isNullOrBlank() && notifiedDrivers.add(bDriverUid)) {
          val dNotifRef =
            db.collection("notifications")
              .document("notif_${UUID.randomUUID().toString().replace("-", "").take(16)}")
          batch.set(
            dNotifRef,
            mapOf(
              "recipientUid" to bDriverUid,
              "senderUid" to posterUid,
              "title" to "$statusEmoji Load Status Changed: $targetStatus",
              "message" to
                "Load ${load.originCity} → ${load.destinationCity} status has been updated to $targetStatus by ${load.posterCompanyName}.",
              "type" to "LOAD_STATUS_CHANGED",
              "loadId" to load.id,
              "isRead" to false,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
      }

      val posterNotifRef =
        db.collection("notifications")
          .document("notif_${UUID.randomUUID().toString().replace("-", "").take(16)}")
      batch.set(
        posterNotifRef,
        mapOf(
          "recipientUid" to posterUid,
          "senderUid" to posterUid,
          "title" to "$statusEmoji Load Marked as $targetStatus",
          "message" to
            "Status for ${load.originCity} → ${load.destinationCity} has been changed to $targetStatus.",
          "type" to "LOAD_STATUS_CHANGED",
          "loadId" to load.id,
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )

      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, loadRef.path)
      Result.failure(e)
    }
  }

  /**
   * Fetches mutually unlocked direct contact numbers:
   * - Load Poster receives direct phone numbers of all accepted bidders (up to 3).
   * - Accepted Vehicle Owner / Driver receives Load Poster's direct phone number & pickup address.
   */
  suspend fun fetchUnlockedContactsForAcceptedLoad(load: LoadPost): Result<UnlockedDealContacts> {
    val uid = requireUserId()
    val loadDocRef = db.collection("loads").document(load.id)
    val vaultRef = loadDocRef.collection("vault").document("contact")

    return try {
      val freshLoad = getLoadById(load.id) ?: load
      val isPoster = uid == freshLoad.posterId
      val isAcceptedDriver = freshLoad.isDriverAccepted(uid)

      if (!isPoster && !isAcceptedDriver) {
        return Result.failure(
          IllegalAccessException(
            "Direct contact numbers are shared only with the Load Poster and accepted bidders."
          )
        )
      }

      val vault =
        vaultRef
          .get()
          .await()
          .toObject(LoadContactVault::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

      val acceptedBidders =
        if (isPoster) {
          val unlockedSnap =
            loadDocRef
              .collection("bid_contacts")
              .whereEqualTo("posterId", uid)
              .whereEqualTo("unlockedForPoster", true)
              .get()
              .await()
          unlockedSnap
            .toObjects(
              BidPrivateContact::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedBy { it.bidAmount }
            .map { bc ->
              AcceptedBidderContact(
                bidId = bc.bidId,
                driverId = bc.driverId,
                driverFullName = bc.driverFullName,
                driverDirectPhone = bc.driverDirectPhone,
                bidAmount = bc.bidAmount,
                fullVehicleNumber = bc.fullVehicleNumber,
              )
            }
        } else {
          emptyList()
        }

      Result.success(
        UnlockedDealContacts(
          loadId = freshLoad.id,
          posterDirectPhone = vault?.posterDirectPhone,
          posterCompanyName = freshLoad.posterCompanyName,
          exactPickupAddress = vault?.exactPickupAddress,
          acceptedBidders = acceptedBidders,
          confirmationBanner = CONFIRMATION_MESSAGE,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, vaultRef.path)
      Result.failure(e)
    }
  }

  // --- 5. Real-Time Notifications Stream ---

  fun observeUserNotifications(userId: String): Flow<List<AppNotification>> {
    val path = "notifications"
    return db
      .collection(path)
      .whereEqualTo("recipientUid", userId)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun markNotificationRead(notificationId: String) {
    requireUserId()
    val docRef = db.collection("notifications").document(notificationId)
    try {
      docRef
        .update(
          mapOf(
            "isRead" to true,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  suspend fun deleteNotification(notificationId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("notifications").document(notificationId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun clearAllNotifications(): Result<Unit> {
    val uid = requireUserId()
    return try {
      val snaps =
        db.collection("notifications")
          .whereEqualTo("recipientUid", uid)
          .get()
          .await()
      val batch = db.batch()
      for (doc in snaps.documents) {
        batch.delete(doc.reference)
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "notifications")
      Result.failure(e)
    }
  }

  fun observeFleetVehicles(userId: String): Flow<List<FleetVehicle>> {
    val path = "users/$userId/fleet"
    return db.collection("users").document(userId).collection("fleet")
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(FleetVehicle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun saveFleetVehicle(
    vehicleSizeFeet: Int,
    vehicleBodyType: String,
    vehicleCity: String,
    vehicleLat: Double? = null,
    vehicleLng: Double? = null,
  ): Result<Unit> {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val vehicleId = "veh_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val fleetRef = userRef.collection("fleet").document(vehicleId)

    val resolvedCoords =
      if (vehicleLat != null && vehicleLng != null) {
        Pair(vehicleLat, vehicleLng)
      } else {
        val hub =
          GeoAndPrivacyUtils.MAJOR_LOGISTICS_HUBS.find {
            it.name.contains(vehicleCity.trim(), ignoreCase = true)
          } ?: GeoAndPrivacyUtils.MAJOR_LOGISTICS_HUBS[0]
        Pair(hub.lat, hub.lng)
      }

    return try {
      val batch = db.batch()
      batch.update(
        userRef,
        mapOf(
          "vehicleSizeFeet" to vehicleSizeFeet,
          "vehicleBodyType" to vehicleBodyType,
          "currentCity" to vehicleCity.trim().take(120),
          "currentLat" to resolvedCoords.first,
          "currentLng" to resolvedCoords.second,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        fleetRef,
        mapOf(
          "userId" to uid,
          "vehicleSizeFeet" to vehicleSizeFeet,
          "vehicleBodyType" to vehicleBodyType,
          "vehicleCity" to vehicleCity.trim().take(120),
          "vehicleLat" to resolvedCoords.first,
          "vehicleLng" to resolvedCoords.second,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, fleetRef.path)
      Result.failure(e)
    }
  }

  suspend fun dispatchMatchingLoadNotification(
    recipientUid: String,
    load: LoadPost,
    vehicleFeet: Int,
    vehicleBodyType: String,
    vehicleCity: String,
    distanceKm: Double,
  ): Result<Unit> {
    val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val notifRef = db.collection("notifications").document(notifId)
    val bodyTypeLabel = if (vehicleBodyType == "CONTAINER") "Container" else "Open Truck"
    val cleanDistance = distanceKm.roundToInt().coerceAtLeast(1)

    return try {
      notifRef.set(
        mapOf(
          "recipientUid" to recipientUid,
          "senderUid" to recipientUid,
          "title" to "🎯 Matching Load Found: ${load.originCity} → ${load.destinationCity}",
          "message" to "A shipper posted ${load.originCity} → ${load.destinationCity} (${load.requiredSizeFeet}ft $bodyTypeLabel) near $vehicleCity ($cleanDistance km away). Tap to place your competitive bid!",
          "type" to "MATCHING_LOAD",
          "loadId" to load.id,
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  companion object {
    const val MAX_ACCEPTED_BIDS = 100
    const val VERIFIED_TRUCKS_HELPLINE = "+919113265599"
    const val CONFIRMATION_MESSAGE = "✅ Bid Accepted! Direct contact details unlocked."
  }
}
