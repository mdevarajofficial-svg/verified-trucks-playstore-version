package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Test

class LogisticsRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun unauthenticatedCaller_observeActiveMarketplaceLoads_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = LogisticsRepository(firestore)

    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeActiveMarketplaceLoads().first()
      }
    } catch (e: Exception) {
      caught = e
    }

    assertThat(caught).isNotNull()
    val fsEx = checkNotNull(caught!!.unwrapFirestoreException()) {
      "Expected FirebaseFirestoreException inside $caught"
    }
    assertThat(fsEx.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
  }

  @Test
  fun topThreeLowestLeaderboard_mutualDirectContactExchange_andBookedStatusSyncWithNotifications() =
    runBlocking {
      // 1. Load Poster signs in & posts load with direct phone stored in vault
      val posterUid = signInTestUser("poster_top3@verifiedtrucks.test")
      val repo = LogisticsRepository(firestore)

      withTimeout(DEFAULT_TIMEOUT_MS) {
        val posterSave = repo.savePosterOnboarding("Alpha Transports", "9876543210")
        assertThat(posterSave.isSuccess).isTrue()
      }

      val loadId =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo
            .createLoadPost(
              posterCompanyName = "Alpha Transports",
              posterDirectPhone = "9876543210",
              originCity = "Mumbai",
              originAddress = "Mumbai MIDC Gate 4",
              originLat = 19.0760,
              originLng = 72.8777,
              destinationCity = "Pune",
              destinationAddress = "Pune Chakan Phase 2",
              destLat = 18.5204,
              destLng = 73.8567,
              totalDistanceKm = 148.0,
              requiredSizeFeet = 10,
              requiredTonnage = 2.5,
              requiredBodyType = "OPEN",
              materialType = "Industrial Boxes",
            )
            .getOrThrow()
        }

      // 2. Four Drivers sign in and place bids in unsorted order:
      //    Driver 4 = ₹16,500 (Rank L4)
      //    Driver 1 = ₹12,000 (Rank L1 - Lowest on top)
      //    Driver 3 = ₹14,500 (Rank L3)
      //    Driver 2 = ₹13,200 (Rank L2)
      data class TestBidder(
        val email: String,
        val name: String,
        val phone: String,
        val amount: Double,
      )

      val bidders =
        listOf(
          TestBidder("driver4_top3@verifiedtrucks.test", "Suresh Yadav", "9444444444", 16500.0),
          TestBidder("driver1_top3@verifiedtrucks.test", "Vikram Rathore", "9111111111", 12000.0),
          TestBidder("driver3_top3@verifiedtrucks.test", "Manoj Sharma", "9333333333", 14500.0),
          TestBidder("driver2_top3@verifiedtrucks.test", "Rajesh Patel", "9222222222", 13200.0),
        )

      val uidByPhone = mutableMapOf<String, String>()
      val bidIdByPhone = mutableMapOf<String, String>()

      for (b in bidders) {
        val dUid = signInTestUser(b.email)
        uidByPhone[b.phone] = dUid
        withTimeout(DEFAULT_TIMEOUT_MS) {
          assertThat(
              repo
                .saveDriverKycAndVehicleProfile(
                  fullName = b.name,
                  phoneNumber = b.phone,
                  drivingLicenseNumber = "MH1220200099881",
                )
                .isSuccess
            )
            .isTrue()
        }

        val activeLoads =
          withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeActiveMarketplaceLoads().first { list -> list.any { it.id == loadId } }
          }
        val targetLoad = activeLoads.first { it.id == loadId }

        // Driver CANNOT read Poster's unmasked vault phone before bid acceptance
        var prematureVaultErr: Exception? = null
        try {
          withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore
              .collection("loads")
              .document(loadId)
              .collection("vault")
              .document("contact")
              .get()
              .await()
          }
        } catch (e: Exception) {
          prematureVaultErr = e
        }
        assertThat(prematureVaultErr?.unwrapFirestoreException()?.code)
          .isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)

        val placedBidId =
          withTimeout(DEFAULT_TIMEOUT_MS) {
            repo
              .placeBlindBid(
                load = targetLoad,
                driverDisplayName = b.name,
                driverDirectPhone = b.phone,
                driverRating = 4.8,
                vehicleDetails = "10ft OPEN",
                fullVehicleNumber = "VERIFIED-TRUCK",
                bidAmount = b.amount,
              )
              .getOrThrow()
          }
        bidIdByPhone[b.phone] = placedBidId
      }

      // 3. Poster signs in & verifies leaderboard has LOWEST amount on top (12000, 13200, 14500, 16500)
      val posterBackUid = signInTestUser("poster_top3@verifiedtrucks.test")
      assertThat(posterBackUid).isEqualTo(posterUid)

      val rankedBids =
        withTimeout(FLOW_TIMEOUT_MS) {
          repo.observeBidsForPoster(loadId, posterUid).first { it.size == 4 }
        }
      assertThat(rankedBids.map { it.bidAmount })
        .containsExactly(12000.0, 13200.0, 14500.0, 16500.0)
        .inOrder()

      // Poster CANNOT read any bidder's private contact BEFORE accepting their bid
      var prematurePosterReadErr: Exception? = null
      try {
        withTimeout(DEFAULT_TIMEOUT_MS) {
          firestore
            .collection("loads")
            .document(loadId)
            .collection("bid_contacts")
            .document(rankedBids[0].id)
            .get()
            .await()
        }
      } catch (e: Exception) {
        prematurePosterReadErr = e
      }
      assertThat(prematurePosterReadErr?.unwrapFirestoreException()?.code)
        .isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)

      // Attempting to accept Rank 4 bid (16500) MUST fail (Top 3 only)
      val rank4Attempt =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo.acceptBidTransaction(loadId, rankedBids[3])
        }
      assertThat(rank4Attempt.isFailure).isTrue()

      // 4. Poster accepts Top 3 bids (L1=12000, L2=13200, L3=14500) and gets all 3 bidder phone numbers
      withTimeout(DEFAULT_TIMEOUT_MS) {
        val accept1 = repo.acceptBidTransaction(loadId, rankedBids[0])
        if (accept1.isFailure) {
          throw AssertionError("accept1 failed: ${accept1.exceptionOrNull()}", accept1.exceptionOrNull())
        }
        val accept2 = repo.acceptBidTransaction(loadId, rankedBids[1])
        if (accept2.isFailure) {
          throw AssertionError("accept2 failed: ${accept2.exceptionOrNull()}", accept2.exceptionOrNull())
        }
      }
      val unlockedAfterThree =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo.acceptBidTransaction(loadId, rankedBids[2]).getOrThrow()
        }

      assertThat(unlockedAfterThree.acceptedBidders.map { it.driverDirectPhone })
        .containsExactly("9111111111", "9222222222", "9333333333")
        .inOrder()

      // Poster STILL cannot read unaccepted 4th bidder's private phone
      var unacceptedReadErr: Exception? = null
      try {
        withTimeout(DEFAULT_TIMEOUT_MS) {
          firestore
            .collection("loads")
            .document(loadId)
            .collection("bid_contacts")
            .document(rankedBids[3].id)
            .get()
            .await()
        }
      } catch (e: Exception) {
        unacceptedReadErr = e
      }
      assertThat(unacceptedReadErr?.unwrapFirestoreException()?.code)
        .isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)

      // 5. Accepted Driver (L1) signs in & gets Load Poster's direct phone ("9876543210") in My Trips, Vault, and Notification
      val driver1Uid = signInTestUser("driver1_top3@verifiedtrucks.test")
      assertThat(driver1Uid).isEqualTo(uidByPhone["9111111111"])

      val driver1Trips =
        withTimeout(FLOW_TIMEOUT_MS) {
          repo.observeDriverTrips(driver1Uid).first { list -> list.any { it.loadId == loadId } }
        }
      val d1Trip = driver1Trips.first { it.loadId == loadId }
      assertThat(d1Trip.bidStatus).isEqualTo("ACCEPTED")
      assertThat(d1Trip.unlockedPosterPhone).isEqualTo("9876543210")

      val driver1Unlocked =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo.fetchUnlockedContactsForAcceptedLoad(d1Trip.toLoadPost()).getOrThrow()
        }
      assertThat(driver1Unlocked.posterDirectPhone).isEqualTo("9876543210")
      assertThat(driver1Unlocked.exactPickupAddress).isEqualTo("Mumbai MIDC Gate 4")

      val driver1Notifs =
        withTimeout(FLOW_TIMEOUT_MS) {
          repo
            .observeUserNotifications(driver1Uid)
            .first { list -> list.any { it.type == "BID_ACCEPTED" && it.loadId == loadId } }
        }
      assertThat(driver1Notifs.first { it.type == "BID_ACCEPTED" }.unlockedPhone)
        .isEqualTo("9876543210")

      // Unaccepted 4th Driver signs in: sees bid in My Trips as PENDING without Poster's phone, and cannot read vault
      val driver4Uid = signInTestUser("driver4_top3@verifiedtrucks.test")
      val driver4TripsBeforeBook =
        withTimeout(FLOW_TIMEOUT_MS) {
          repo.observeDriverTrips(driver4Uid).first { list -> list.any { it.loadId == loadId } }
        }
      val d4TripBefore = driver4TripsBeforeBook.first { it.loadId == loadId }
      assertThat(d4TripBefore.bidStatus).isEqualTo("PENDING")
      assertThat(d4TripBefore.unlockedPosterPhone).isNull()

      var d4VaultErr: Exception? = null
      try {
        withTimeout(DEFAULT_TIMEOUT_MS) {
          firestore
            .collection("loads")
            .document(loadId)
            .collection("vault")
            .document("contact")
            .get()
            .await()
        }
      } catch (e: Exception) {
        d4VaultErr = e
      }
      assertThat(d4VaultErr?.unwrapFirestoreException()?.code)
        .isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)

      // 6. Load Poster marks load as BOOKED -> syncs to My Trips for ALL bidders & sends LOAD_STATUS_CHANGED notifications to both parties
      signInTestUser("poster_top3@verifiedtrucks.test")
      val currentPosterLoad = checkNotNull(repo.getLoadById(loadId))
      withTimeout(DEFAULT_TIMEOUT_MS) {
        assertThat(repo.updateLoadFinalStatus(currentPosterLoad, "BOOKED").isSuccess).isTrue()
      }

      val posterStatusNotifs =
        withTimeout(FLOW_TIMEOUT_MS) {
          repo
            .observeUserNotifications(posterUid)
            .first { list -> list.any { it.type == "LOAD_STATUS_CHANGED" && it.loadId == loadId } }
        }
      assertThat(posterStatusNotifs.first { it.type == "LOAD_STATUS_CHANGED" }.message)
        .contains("BOOKED")

      // Both accepted Driver 1 and unaccepted Driver 4 see loadStatus == "BOOKED" in My Trips and get LOAD_STATUS_CHANGED notifications
      signInTestUser("driver4_top3@verifiedtrucks.test")
      val driver4TripsAfterBook =
        withTimeout(FLOW_TIMEOUT_MS) {
          repo
            .observeDriverTrips(driver4Uid)
            .first { list -> list.any { it.loadId == loadId && it.loadStatus == "BOOKED" } }
        }
      assertThat(driver4TripsAfterBook.first { it.loadId == loadId }.loadStatus).isEqualTo("BOOKED")

      val driver4StatusNotifs =
        withTimeout(FLOW_TIMEOUT_MS) {
          repo
            .observeUserNotifications(driver4Uid)
            .first { list -> list.any { it.type == "LOAD_STATUS_CHANGED" && it.loadId == loadId } }
        }
      assertThat(driver4StatusNotifs.first { it.type == "LOAD_STATUS_CHANGED" }.message)
        .contains("BOOKED")
    }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 8000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
