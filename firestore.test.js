const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const POSTER_UID = "poster_transporter_1";
const DRIVER_1 = "driver_owner_1";
const DRIVER_2 = "driver_owner_2";
const DRIVER_3 = "driver_owner_3";
const DRIVER_4 = "driver_owner_4";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("1. Unauthenticated caller cannot query active loads", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("loads").where("status", "==", "ACTIVE").get());
});

test("2. Blanket unfiltered load query is rejected (rules are not filters)", async () => {
  const driverDb = testEnv.authenticatedContext(DRIVER_1).firestore();
  await assertFails(driverDb.collection("loads").get());
});

test("3. PII Isolation: Cross-user read/delete of private_kyc is rejected, owner read/delete succeeds", async () => {
  const ts = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().doc(`users/${DRIVER_1}`).set({
      userId: DRIVER_1,
      role: "DRIVER",
      displayName: "Rajesh Kumar",
      kycVerified: true,
      rating: 4.9,
      vehicleSizeFeet: 8,
      createdAt: ts,
      updatedAt: ts,
    });
    await ctx.firestore().doc(`users/${DRIVER_1}/private_kyc/info`).set({
      userId: DRIVER_1,
      fullName: "Rajesh Kumar",
      phoneNumber: "9876543210",
      maskedPhone: "XXXXXX3210",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const posterDb = testEnv.authenticatedContext(POSTER_UID).firestore();
  await assertFails(posterDb.doc(`users/${DRIVER_1}/private_kyc/info`).get());
  await assertFails(posterDb.doc(`users/${DRIVER_1}/private_kyc/info`).delete());

  const driverDb = testEnv.authenticatedContext(DRIVER_1).firestore();
  await assertSucceeds(driverDb.doc(`users/${DRIVER_1}/private_kyc/info`).get());
  await assertSucceeds(driverDb.doc(`users/${DRIVER_1}/private_kyc/info`).delete());
  await assertSucceeds(driverDb.doc(`users/${DRIVER_1}`).delete());
});

test("4. Unmasked phone number in public LoadPost is strictly rejected", async () => {
  const posterDb = testEnv.authenticatedContext(POSTER_UID).firestore();
  const ts = new Date(Date.now() - 2000);
  const batch = posterDb.batch();
  batch.set(posterDb.doc("loads/load_bad_phone"), {
    posterId: POSTER_UID,
    posterCompanyName: "Apex Freight",
    maskedPosterPhone: "9876543210",
    originCity: "Mumbai",
    originLat: 19.076,
    originLng: 72.8777,
    destinationCity: "Pune",
    destLat: 18.5204,
    destLng: 73.8567,
    totalDistanceKm: 148.5,
    loadingWaypoints: [],
    requiredSizeFeet: 10,
    requiredTonnage: 2.5,
    requiredBodyType: "OPEN",
    materialType: "Electronics",
    loadingDate: "Immediate",
    loadingTime: "Now",
    status: "ACTIVE",
    bidCount: 0,
    expiresAtEpochMillis: Date.now() + 6 * 3600 * 1000,
    createdAt: ts,
    updatedAt: ts,
  });
  batch.set(posterDb.doc("loads/load_bad_phone/vault/contact"), {
    loadId: "load_bad_phone",
    posterId: POSTER_UID,
    posterDirectPhone: "9876543210",
    exactPickupAddress: "Mumbai, Maharashtra",
    createdAt: ts,
    updatedAt: ts,
  });
  await assertFails(batch.commit());
});

test("5. Top-3 Bid Acceptance, Mutual Direct Contact Exchange, Cap at 3, and BOOKED/CANCELLED My Trips + Notifications", async () => {
  const ts = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().doc("loads/load_303").set({
      posterId: POSTER_UID,
      posterCompanyName: "Bharat Logistics",
      maskedPosterPhone: "XXXXXX1111",
      originCity: "Bengaluru",
      originLat: 12.9716,
      originLng: 77.5946,
      destinationCity: "Chennai",
      destLat: 13.0827,
      destLng: 80.2707,
      totalDistanceKm: 346.0,
      loadingWaypoints: [],
      requiredSizeFeet: 9,
      requiredTonnage: 2.0,
      requiredBodyType: "CONTAINER",
      materialType: "Steel Coils",
      loadingDate: "Immediate",
      loadingTime: "Now",
      status: "ACTIVE",
      bidCount: 0,
      expiresAtEpochMillis: Date.now() + 6 * 3600 * 1000,
      createdAt: ts,
      updatedAt: ts,
    });
    await ctx.firestore().doc("loads/load_303/vault/contact").set({
      loadId: "load_303",
      posterId: POSTER_UID,
      posterDirectPhone: "9888881111",
      exactPickupAddress: "Peenya Industrial Area Phase 2, Bengaluru",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  // Helper to place bid for a driver
  async function placeBidAsDriver(driverUid, bidId, amount, phone, currentCount, lowest) {
    const db = testEnv.authenticatedContext(driverUid).firestore();
    const bTs = new Date(Date.now() - 4000);
    const batch = db.batch();
    batch.set(db.doc(`loads/load_303/bids/${bidId}`), {
      loadId: "load_303",
      posterId: POSTER_UID,
      driverId: driverUid,
      driverAnonymousAlias: `Fleet #${bidId}`,
      driverDisplayName: `Driver ${bidId}`,
      maskedDriverPhone: "XXXXXX9999",
      driverRating: 4.8,
      vehicleDetails: "9ft CONTAINER",
      maskedVehicleNumber: "VERIXXRUCK",
      bidAmount: amount,
      availabilityTime: "Ready Now",
      status: "PENDING",
      createdAt: bTs,
      updatedAt: bTs,
    });
    batch.set(db.doc(`loads/load_303/bid_contacts/${bidId}`), {
      bidId,
      loadId: "load_303",
      driverId: driverUid,
      posterId: POSTER_UID,
      driverDirectPhone: phone,
      fullVehicleNumber: "VERIFIED-TRUCK",
      driverFullName: `Driver ${bidId}`,
      bidAmount: amount,
      unlockedForPoster: false,
      createdAt: bTs,
    });
    batch.set(db.doc(`driver_trips/${driverUid}_load_303`), {
      tripId: `${driverUid}_load_303`,
      loadId: "load_303",
      bidId,
      posterId: POSTER_UID,
      posterCompanyName: "Bharat Logistics",
      driverId: driverUid,
      driverDisplayName: `Driver ${bidId}`,
      originCity: "Bengaluru",
      destinationCity: "Chennai",
      totalDistanceKm: 346.0,
      requiredSizeFeet: 9,
      requiredTonnage: 2.0,
      requiredBodyType: "CONTAINER",
      materialType: "Steel Coils",
      bidAmount: amount,
      bidStatus: "PENDING",
      loadStatus: "ACTIVE",
      createdAt: bTs,
      updatedAt: bTs,
    });
    batch.update(db.doc("loads/load_303"), {
      bidCount: currentCount + 1,
      lowestBidAmount: lowest,
      updatedAt: bTs,
    });
    await assertSucceeds(batch.commit());
  }

  await placeBidAsDriver(DRIVER_1, "bid_1", 16000, "9111110001", 0, 16000);
  await placeBidAsDriver(DRIVER_2, "bid_2", 17000, "9111110002", 1, 16000);
  await placeBidAsDriver(DRIVER_3, "bid_3", 18000, "9111110003", 2, 16000);
  await placeBidAsDriver(DRIVER_4, "bid_4", 19000, "9111110004", 3, 16000);

  const posterDb = testEnv.authenticatedContext(POSTER_UID).firestore();
  const d1Db = testEnv.authenticatedContext(DRIVER_1).firestore();
  const d4Db = testEnv.authenticatedContext(DRIVER_4).firestore();

  // Before acceptance, neither party can read the other's unmasked phone
  await assertFails(posterDb.doc("loads/load_303/bid_contacts/bid_1").get());
  await assertFails(d1Db.doc("loads/load_303/vault/contact").get());

  // Poster accepts Top-3 bids sequentially (1st, 2nd, 3rd)
  async function acceptBidAsPoster(bidId, driverUid, acceptedBidIds, acceptedDriverIds) {
    const aTs = new Date(Date.now() - 1000);
    const batch = posterDb.batch();
    batch.update(posterDb.doc("loads/load_303"), {
      status: "ACCEPTED",
      acceptedBidId: bidId,
      winningDriverId: driverUid,
      acceptedBidIds,
      acceptedDriverIds,
      updatedAt: aTs,
    });
    batch.update(posterDb.doc(`loads/load_303/bids/${bidId}`), {
      status: "ACCEPTED",
      updatedAt: aTs,
    });
    batch.update(posterDb.doc(`loads/load_303/bid_contacts/${bidId}`), {
      unlockedForPoster: true,
      updatedAt: aTs,
    });
    batch.update(posterDb.doc("loads/load_303/vault/contact"), {
      authorizedDriverId: driverUid,
      authorizedDriverIds: acceptedDriverIds,
      updatedAt: aTs,
    });
    batch.update(posterDb.doc(`driver_trips/${driverUid}_load_303`), {
      bidStatus: "ACCEPTED",
      loadStatus: "ACCEPTED",
      unlockedPosterPhone: "9888881111",
      updatedAt: aTs,
    });
    batch.set(posterDb.doc(`notifications/notif_${bidId}`), {
      recipientUid: driverUid,
      senderUid: POSTER_UID,
      title: "Bid Accepted!",
      message: "Your bid was accepted! Call Load Poster directly.",
      type: "BID_ACCEPTED",
      loadId: "load_303",
      bidId,
      unlockedPhone: "9888881111",
      isRead: false,
      createdAt: aTs,
      updatedAt: aTs,
    });
    await assertSucceeds(batch.commit());
  }

  await acceptBidAsPoster("bid_1", DRIVER_1, ["bid_1"], [DRIVER_1]);
  await acceptBidAsPoster("bid_2", DRIVER_2, ["bid_1", "bid_2"], [DRIVER_1, DRIVER_2]);
  await acceptBidAsPoster("bid_3", DRIVER_3, ["bid_1", "bid_2", "bid_3"], [DRIVER_1, DRIVER_2, DRIVER_3]);

  // Poster CAN read accepted drivers' phones (bid_1, bid_2, bid_3)
  await assertSucceeds(posterDb.doc("loads/load_303/bid_contacts/bid_1").get());
  await assertSucceeds(posterDb.doc("loads/load_303/bid_contacts/bid_2").get());
  await assertSucceeds(posterDb.doc("loads/load_303/bid_contacts/bid_3").get());
  // Poster CANNOT read unaccepted 4th driver's phone!
  await assertFails(posterDb.doc("loads/load_303/bid_contacts/bid_4").get());

  // Accepted driver 1 CAN read Poster's direct phone in vault; unaccepted driver 4 CANNOT!
  await assertSucceeds(d1Db.doc("loads/load_303/vault/contact").get());
  await assertFails(d4Db.doc("loads/load_303/vault/contact").get());

  // Attempting to accept a 4th bid MUST FAIL (exceeds max 3 cap)
  const badBatch = posterDb.batch();
  const failTs = new Date(Date.now() - 500);
  badBatch.update(posterDb.doc("loads/load_303"), {
    status: "ACCEPTED",
    acceptedBidId: "bid_4",
    winningDriverId: DRIVER_4,
    acceptedBidIds: ["bid_1", "bid_2", "bid_3", "bid_4"],
    acceptedDriverIds: [DRIVER_1, DRIVER_2, DRIVER_3, DRIVER_4],
    updatedAt: failTs,
  });
  badBatch.update(posterDb.doc("loads/load_303/bids/bid_4"), { status: "ACCEPTED", updatedAt: failTs });
  badBatch.update(posterDb.doc("loads/load_303/bid_contacts/bid_4"), { unlockedForPoster: true, updatedAt: failTs });
  await assertFails(badBatch.commit());

  // Poster marks load as BOOKED -> updates all bidder driver_trips + sends notifications to both Poster & Bidders
  const bookTs = new Date(Date.now() - 200);
  const statusBatch = posterDb.batch();
  statusBatch.update(posterDb.doc("loads/load_303"), {
    status: "BOOKED",
    updatedAt: bookTs,
  });
  for (const dUid of [DRIVER_1, DRIVER_2, DRIVER_3, DRIVER_4]) {
    statusBatch.update(posterDb.doc(`driver_trips/${dUid}_load_303`), {
      loadStatus: "BOOKED",
      updatedAt: bookTs,
    });
    statusBatch.set(posterDb.doc(`notifications/status_${dUid}`), {
      recipientUid: dUid,
      senderUid: POSTER_UID,
      title: "Load Status: BOOKED",
      message: "Load status changed to BOOKED.",
      type: "LOAD_STATUS_CHANGED",
      loadId: "load_303",
      isRead: false,
      createdAt: bookTs,
      updatedAt: bookTs,
    });
  }
  statusBatch.set(posterDb.doc("notifications/status_poster"), {
    recipientUid: POSTER_UID,
    senderUid: POSTER_UID,
    title: "Load Status: BOOKED",
    message: "You marked this load as BOOKED.",
    type: "LOAD_STATUS_CHANGED",
    loadId: "load_303",
    isRead: false,
    createdAt: bookTs,
    updatedAt: bookTs,
  });
  await assertSucceeds(statusBatch.commit());

  // All bidders can query their My Trips and see BOOKED
  const d4Trips = await assertSucceeds(d4Db.collection("driver_trips").where("driverId", "==", DRIVER_4).get());
  if (d4Trips.docs[0].data().loadStatus !== "BOOKED") {
    throw new Error("Expected driver_trips loadStatus to be BOOKED");
  }
});
