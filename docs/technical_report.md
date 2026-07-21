# Technical Report
## Smart Home Monitoring & Control System
### SCS 3311 — Mobile Application Design & Development

---

## 1. Introduction

This document is the technical report for the Smart Home Monitoring & Control System mini-project.
It covers the three areas explicitly required by the specification:

1. **Synchronisation mechanism** — how state changes propagate in real time between all system components
2. **Floor plan representation** — how floor plans and device positions are modelled and stored
3. **Simulator operations** — how the companion web dashboard connects to and interacts with the system

The system consists of four interconnected components:

| Component | Technology | Role |
|-----------|-----------|------|
| Android Mobile App | Kotlin · Jetpack Compose · Firebase SDK | Primary control interface |
| Cloud Database | Firebase Firestore | Single source of truth |
| Cloud Functions | Node.js · Firebase Functions v2 | Server-side safety enforcement |
| Hardware Simulator | HTML · JavaScript · Firebase JS SDK v10 | Companion web dashboard |

---

## 2. Synchronisation Mechanism

### 2.1 Architecture Overview

The system uses **Firebase Cloud Firestore** as the shared state store.
All four components read from and write to the **same Firestore collections**.
There is no separate REST API layer — synchronisation is achieved through
Firestore's native real-time listeners.

```
┌─────────────────────────────────────────────────────────────────┐
│                    Firebase Cloud Firestore                     │
│   floorPlans/{id}/devices/{id}   ·   usageLogs/{id}            │
└────────────┬───────────────────────────────┬────────────────────┘
             │  snapshot listeners            │  batch writes
      ┌──────┴──────┐                  ┌──────┴──────┐
      │ Android App │                  │  Cloud Fns  │
      │  (Kotlin)   │                  │ (Node.js)   │
      └──────┬──────┘                  └─────────────┘
             │  same snapshot listeners
      ┌──────┴──────┐
      │ Web Simulator│
      │  (HTML/JS)  │
      └─────────────┘
```

### 2.2 Real-Time Listeners (Bidirectional Sync)

**Android App — Kotlin:**
The `FirestoreRepository` class wraps each Firestore query in a
`callbackFlow` + `addSnapshotListener`. This converts the callback-based
Firestore API into a Kotlin `Flow<T>` that ViewModels collect.

```kotlin
// FirestoreRepository.kt (simplified)
fun observeDevices(floorPlanId: String): Flow<List<Device>> = callbackFlow {
    val registration = floorPlansRef
        .document(floorPlanId)
        .collection("devices")
        .addSnapshotListener { snapshot, error ->
            val devices = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Device::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(devices)          // emits immediately on any change
        }
    awaitClose { registration.remove() }
}
```

Any write from *any* source (another phone, the web simulator, or a Cloud Function)
triggers the listener, which emits a new list, which the ViewModel exposes via
`StateFlow`, which Compose re-renders automatically.

**Web Simulator — JavaScript:**
The simulator uses the Firebase JS SDK v10 `onSnapshot` function identically:

```javascript
onSnapshot(collection(db, "floorPlans", planId, "devices"), (snap) => {
    devices = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    renderGrid();   // re-draws the 8×8 grid in the browser
});
```

Both clients receive updates in **under 2 seconds** under normal network conditions
(Firestore's real-time push via WebSocket/gRPC).

### 2.3 Write Path

When a user taps a device in the Android app:
1. `FloorPlanGridViewModel.toggleDevice()` calls `FirestoreRepository.toggleDeviceStatus()`
2. The repository writes a partial update (`status: "ON"` / `"OFF"`) to Firestore
3. Firestore immediately delivers the change to **all active listeners** on that document
4. The Android UI (via StateFlow + Compose) and the Web Simulator (via onSnapshot) both re-render within ~1s

When the web simulator user clicks a grid cell:
1. The simulator's `toggleDevice()` calls Firestore `updateDoc()` directly
2. The same Firestore push notifies the Android app's listeners
3. Both UIs update without any polling or manual refresh

### 2.4 Server-Side Safety (Cloud Functions)

Two Cloud Functions run on Cloud Scheduler (every 1 minute):

**ironSafetyCutoff:**
```
every minute:
  query all IRON devices with status = "ON"
  for each:
    elapsed = now - lastTurnedOnAt
    if elapsed ≥ maxOnDurationMinutes:
      write status = "OFF"        → triggers all Firestore listeners
      write usageLogs entry       → event = "CUTOFF"
      send FCM push notification  → Android app shows heads-up alert
```

**lightScheduler:**
```
every minute:
  query all LIGHT devices with turnOnTime / turnOffTime set
  for each:
    if current time is in [turnOnTime, turnOffTime) window AND status = "OFF":
      write status = "ON"         → triggers all listeners
      write usageLogs entry       → event = "SCHEDULE_ON"
    elif current time is outside window AND status = "ON":
      write status = "OFF"        → triggers all listeners
      write usageLogs entry       → event = "SCHEDULE_OFF"
```

The Cloud Functions use the Firebase Admin SDK which bypasses Firestore security rules —
this is the intended pattern for trusted server-side code.

### 2.5 Push Notifications

The Android app includes `SmartHomeMessagingService` (extends `FirebaseMessagingService`).
When an iron cutoff occurs, the Cloud Function sends an FCM data message to the
`smarthome_alerts` topic. The service intercepts this and displays a heads-up system
notification — even when the app is in the background.

---

## 3. Floor Plan Representation

### 3.1 Data Model

Floor plans are stored in Firestore at the path `floorPlans/{floorPlanId}`.
Each floor plan document contains:

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Firestore document ID (auto-generated) |
| `name` | String | Human-readable name, e.g. "Ground Floor" |
| `imageUrl` | String | Optional URL for a background image overlay |
| `createdAt` | Timestamp | Server-generated creation timestamp |

### 3.2 Grid Overlay

Devices are not placed at pixel coordinates. Instead, a simple **8×8 abstract grid**
is overlaid on the floor plan. Each cell is addressed by `(gridX, gridY)` where
both values are integers in the range `[0, 7]`.

This grid-based approach:
- Eliminates the need for floor plan image measurements
- Works on any screen size (cells are dynamically sized)
- Allows simple collision detection (`Set<Pair<Int,Int>>` on the client)
- Makes Firestore queries trivial (integer equality, no geo-queries needed)

### 3.3 Device Document Structure

Devices are stored as sub-documents: `floorPlans/{planId}/devices/{deviceId}`

```
Device {
  id:                  String       // Firestore document ID
  floorPlanId:         String       // Parent plan reference
  name:                String       // Display name
  type:                String       // OUTLET | MULTI_SWITCH | IRON | LIGHT | CAMERA
  gridX:               Int          // Column [0-7]
  gridY:               Int          // Row [0-7]
  status:              String       // ON | OFF | ERROR | DISCONNECTED

  // IRON-specific
  maxOnDurationMinutes: Int         // Cutoff threshold
  lastTurnedOnAt:      Timestamp    // Written when toggled ON

  // LIGHT-specific
  turnOnTime:          String       // "HH:mm" 24h
  turnOffTime:         String       // "HH:mm" 24h

  // CAMERA-specific
  cameraSnapshotUrl:   String       // Mock image URL
  cameraStreamUrl:     String       // Mock RTSP URL

  // MULTI_SWITCH-specific
  switchCount:         Int          // 2, 3, or 5
  switches:            Array<SwitchState>  // [{switchIndex, label, status}]

  createdAt:           Timestamp
}
```

All five device types share the same document schema — unused fields default to
empty strings or `0`, which keeps the Firestore collection homogeneous and allows
simple `whereEqualTo("type", "IRON")` queries in Cloud Functions.

### 3.4 Usage Logs

Event logging is written to a flat top-level collection `usageLogs/{logId}`:

```
UsageLog {
  deviceId:      String
  deviceName:    String
  floorPlanId:   String
  floorPlanName: String
  event:         String   // "ON" | "OFF" | "CUTOFF" | "SCHEDULE_ON" | "SCHEDULE_OFF"
  timestamp:     Timestamp
}
```

The Reporting Screen reads this collection and computes:
- Per-device "minutes ON today" (pairing ON→OFF event timestamps)
- Per-day ON minutes for the last 7 days (shown as a bar chart)

---

## 4. Simulator Operations

### 4.1 What the Simulator Is

The Hardware Simulator is a **standalone single-file web page** (`simulator/index.html`)
that represents the physical home appliances — a stand-in for real IoT hardware.
It connects to the **same Firebase Firestore project** as the Android app and provides
an independent view of all device states.

### 4.2 How to Open the Simulator

No build step is required. Simply open `simulator/index.html` in any modern browser
(Chrome, Firefox, Edge). The page loads the Firebase JS SDK v10 from a CDN and
connects automatically.

```
cd SmartHome/simulator
open index.html      # macOS
# or just drag index.html into a browser window
```

If Firestore's CORS settings block local `file://` access, serve it locally:
```bash
python3 -m http.server 8080
# then visit http://localhost:8080/simulator/
```

### 4.3 Connection & Real-Time Listening

On page load, the simulator:
1. Initialises the Firebase app with the same `projectId` as the Android app
2. Opens a `onSnapshot` listener on `floorPlans` (ordered by `createdAt`)
3. Renders the floor plan list in the left sidebar
4. Auto-selects the first floor plan and opens a listener on its `devices` sub-collection
5. Renders the 8×8 grid, re-rendering on every Firestore update

The simulator also opens a listener on `usageLogs` (latest 50 entries) to:
- Display a live event log panel on the right
- Show an alert banner when an iron `CUTOFF` event is detected

### 4.4 Writing Back to Firestore (Toggle)

Clicking a device cell in the web simulator calls `updateDoc()` to flip the device
status in Firestore. This write is immediately reflected on the Android app (via its
snapshot listener) and in the simulator itself (via its own listener).

The simulator also writes a `usageLogs` entry on every toggle, identical to what the
Android app writes — so the Reporting Screen captures events from both interfaces.

### 4.5 Feature Summary

| Feature | Detail |
|---------|--------|
| Real-time sync | Firestore `onSnapshot` — updates appear < 2s |
| Floor plan switching | Sidebar — click to switch active plan |
| 8×8 device grid | Matching the Android app layout exactly |
| Device types | Emoji icons: 🔌 Outlet · 🔀 Multi-Switch · 🔥 Iron · 💡 Light · 📷 Camera |
| Status colours | Green (ON) · Grey (OFF) · Red (ERROR) · White (DISCONNECTED) |
| Toggle | Click any device cell → writes to Firestore → both apps update |
| Event log | Live right-panel showing last 50 events with timestamps |
| Iron cutoff alert | Red banner at top when CUTOFF event detected in logs |
| No build step | Single `.html` file, Firebase SDK loaded from CDN |

---

## 5. Project File Structure

```
SmartHome/
├── app/src/main/java/com/example/smarthome/
│   ├── MainActivity.kt
│   ├── SmartHomeMessagingService.kt       ← FCM push handler
│   ├── data/
│   │   ├── model/Models.kt                ← all data classes + enums
│   │   └── repository/FirestoreRepository.kt
│   ├── navigation/SmartHomeNavHost.kt
│   └── ui/
│       ├── devicecontrol/
│       │   ├── AddDeviceDialog.kt
│       │   └── DeviceDetailSheet.kt
│       ├── floorplan/
│       │   ├── FloorPlanListScreen.kt
│       │   ├── FloorPlanViewModel.kt
│       │   ├── FloorPlanGridScreen.kt
│       │   └── FloorPlanGridViewModel.kt
│       ├── reporting/
│       │   ├── ReportingScreen.kt         ← bar chart + log list
│       │   └── ReportingViewModel.kt
│       └── theme/
│           ├── Color.kt
│           ├── Theme.kt
│           └── Typography.kt
│
├── functions/                             ← Firebase Cloud Functions
│   ├── index.js                           ← ironSafetyCutoff + lightScheduler
│   └── package.json
│
├── simulator/
│   └── index.html                         ← Hardware Simulator web dashboard
│
├── docs/
│   └── technical_report.md               ← this file
│
├── firestore.rules                        ← Firestore security rules
├── firebase.json                          ← Firebase CLI config
└── .firebaserc                            ← Firebase project link
```

---

## 6. Deployment Instructions

### Android App
Build and install via Android Studio → Run, or:
```bash
./gradlew assembleRelease
adb install app/build/outputs/apk/release/app-release.apk
```

### Cloud Functions
```bash
cd functions
npm install
cd ..
firebase deploy --only functions
```
> Requires Firebase CLI (`npm install -g firebase-tools`) and Blaze (pay-as-you-go) plan.

### Firestore Rules
```bash
firebase deploy --only firestore:rules
```

### Hardware Simulator
Open `simulator/index.html` in a browser — no deployment needed.
For production, host on Firebase Hosting:
```bash
firebase init hosting    # set public dir to "simulator"
firebase deploy --only hosting
```

---

*Report prepared for SCS 3311 Mini-Project submission.*
