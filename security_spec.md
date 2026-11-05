# Security Specification: Verified Trucks Marketplace

## 1. Data Invariants
1. **Strict PII Split-Collection Isolation & Conditional Contact Unlocking**:
   - Public `/loads/{loadId}` and `/loads/{loadId}/bids/{bidId}` documents NEVER store raw phone numbers; `maskedPosterPhone` and `maskedDriverPhone` MUST match `^X{6}[0-9]{4}$`.
   - Before bid acceptance, personal phone numbers are strictly isolated:
     - `/loads/{loadId}/vault/contact` stores `posterDirectPhone` and `exactPickupAddress`, plus `authorizedDriverIds`. Readable ONLY by `posterId` OR an accepted driver whose UID is in `authorizedDriverIds` (or legacy `authorizedDriverId`).
     - `/loads/{loadId}/bid_contacts/{bidId}` stores `driverDirectPhone` (entered while placing the bid). Readable ONLY by `driverId` OR by `posterId` ONLY AFTER `unlockedForPoster == true` (set atomically when `posterId` accepts that specific bid).
   - Admin audit records `/bid_phone_records/{bidId}` and `/accepted_deals/{dealId}` store unmasked numbers for console visibility and forbid cross-user reads and all `list` queries.
2. **Multi-Bid Acceptance & Ranked Lowest-First Leaderboard**:
   - Leaderboard sorts bids by `bidAmount` ascending (lowest L1 on top).
   - `posterId` can accept however many bids they want (`acceptedBidIds` and `acceptedDriverIds` lists) while the load status is `'ACTIVE'` or `'ACCEPTED'`.
   - Accepting any bid atomically updates `/loads/{loadId}`, `/loads/{loadId}/bids/{bidId}` (`status == 'ACCEPTED'`), `/loads/{loadId}/bid_contacts/{bidId}` (`unlockedForPoster == true`), `/loads/{loadId}/vault/contact` (adds `driverId` to `authorizedDriverIds`), writes `/accepted_deals/{loadId}_{bidId}`, updates the driver's `/driver_trips/{driverId}_{loadId}` (`bidStatus == 'ACCEPTED'`, `unlockedPosterPhone`), and sends `BID_ACCEPTED` notifications to both parties.
   - All accepted bidders receive the Load Poster's direct contact number (`posterDirectPhone`), and the Load Poster receives direct contact numbers for all accepted bids.
3. **Booked / Cancelled Load Status Lifecycle & Real-Time Driver Sync**:
   - Load Poster can mark their load in My Bids / My Loads as `'BOOKED'` or `'CANCELLED'` (`status in ['BOOKED', 'CANCELLED']`).
   - Once marked `'BOOKED'` or `'CANCELLED'`, terminal state locking prevents further bid placements, further bid acceptances, or status reversals.
   - Every driver who placed a bid has a `/driver_trips/{driverId}_{loadId}` document for their **My Trips** tab. When the Load Poster marks the load `'BOOKED'` or `'CANCELLED'`, all bidder `/driver_trips` documents for that load update `loadStatus` to `'BOOKED'` or `'CANCELLED'`, and both the Load Poster and each bidding Vehicle Owner / Driver receive a `LOAD_STATUS_CHANGED` notification reflecting the chosen status.
4. **Fleet Vehicles & 250km Matched Load Routing**:
   - Vehicle owners/drivers can register their vehicle/fleet (`vehicleSizeFeet`, `vehicleBodyType` [Container or Open], `vehicleCity` / location) under `/users/{userId}/fleet/{vehicleId}` and their user profile.
   - When matching loads are found within a 250km radius from the vehicle's location, the driver receives clean, scannable `MATCHING_LOAD` alerts.
5. **Notification Management & Deletion**:
   - Recipients can clear individual or all notifications (`allow delete: if existing().recipientUid == request.auth.uid`).
6. **Query Enforcement (No Blanket List)**:
   - `/loads` list queries MUST filter by `status == 'ACTIVE'`, `posterId == request.auth.uid`, `winningDriverId == request.auth.uid`, or `acceptedDriverIds array-contains request.auth.uid`.
   - `/driver_trips` list queries MUST filter by `driverId == request.auth.uid` or `posterId == request.auth.uid`.
   - `/notifications` list queries MUST filter by `recipientUid == request.auth.uid`.

## 2. Dirty Dozen Adversarial Payloads
1. **Unauthenticated Load Scrape**: `unauth.collection('loads').where('status', '==', 'ACTIVE').get()` -> REJECTED (`request.auth != null`).
2. **Blanket Unfiltered Load Scrape**: `alice.collection('loads').get()` -> REJECTED (rules are not filters).
3. **Raw Phone Leak in Public Load**: Creating a load with `maskedPosterPhone: "9876543210"` -> REJECTED (`matches('^X{6}[0-9]{4}$')` fails).
4. **Raw Phone Leak in Public Bid**: Creating a bid with `maskedDriverPhone: "+919876543210"` -> REJECTED (`matches('^X{6}[0-9]{4}$')` fails).
5. **Poster Reading Unaccepted Driver Phone**: Poster attempts `get('/loads/L1/bid_contacts/B_unaccepted')` where `unlockedForPoster == false` -> REJECTED.
6. **Unaccepted Driver Reading Poster Vault**: Non-accepted bidder attempts `get('/loads/L1/vault/contact')` -> REJECTED (`request.auth.uid in existing().authorizedDriverIds` fails).
7. **Foreign Notification Clear/Deletion**: User Bob attempts to delete Alice's notification -> REJECTED (`existing().recipientUid == request.auth.uid` fails).
8. **Cross-User KYC Theft**: Bob attempts `get('/users/alice/private_kyc/info')` -> REJECTED (`request.auth.uid == userId`).
9. **Shadow Ghost Field Injection**: Driver updates profile with `{ isVerifiedAdmin: true }` -> REJECTED (`hasOnly` and `affectedKeys().hasOnly()`).
10. **Driver Self-Accepting Own Bid or Unlocking Poster Phone**: Driver updates `/loads/L1/bids/B1` or `/driver_trips/{id}` to inject `bidStatus: 'ACCEPTED'` -> REJECTED (only `posterId` can accept/update status).
11. **Orphaned Acceptance Without Contact Unlock**: Poster updates `/loads/L1/bids/B1` to `'ACCEPTED'` without unlocking `/bid_contacts/B1` or updating `/loads/L1` -> REJECTED (`getAfter` atomicity gate).
12. **Terminal State Re-Write (Post-Booked / Post-Cancelled)**: Attempting to place a bid, accept another bid, or flip `'BOOKED'` to `'CANCELLED'` after terminal transition -> REJECTED (`existing().status in ['ACTIVE', 'ACCEPTED']` gate).
