# SCS 3311: Smart Home Monitoring & Control System — Project Guide

## 🤝 Collaborative Setup (Quick Start)

To collaborate with friends, follow these steps after cloning the repository:

### 1. Web Simulator Setup
- Navigate to the `simulator/` directory.
- Copy `config.js.template` to a new file named `config.js`.
- Replace the placeholders with your own Firebase project keys.
- **Run locally**: Since the page uses JS modules, you must use a local server. Run `npx serve simulator` or use the "Live Server" extension in VS Code.

### 2. Android App Setup
- Navigate to the `app/` directory.
- Copy `google-services.json.template` to `google-services.json`.
- Populate it with your Firebase project's configuration.
- Copy `secrets.properties.template` (in the root) to `secrets.properties` and add any required API keys.

---

## Project Overview

Build a mobile Smart Home Monitoring and Control system consisting of:
1. **Mobile Application Client** (Android APK)
2. **Cloud Database** (Firebase Firestore)
3. **Hardware Simulator Dashboard** (Web-based)
4. **Backend Safety Worker** (Server-side auto-cutoff)

---

## Core Functional Requirements

### 1. Multi-Floor Interactive Dashboard (Mobile Client)

| Feature | Requirement |
|---------|-------------|
| **Floor Plan Layouts** | Support adding/managing multiple house floor plans. View abstract grid mapping overlaid on floor layouts. Free sample plans allowed for demo. |
| **Device Control UI** | Toggle device states with reactive UI updates. Display status: `ON`, `OFF`, `ERROR`, `DISCONNECTED`. |

### 2. Heterogeneous Device Profiles

| Device Type | Behavior |
|-------------|----------|
| **Electrical Outlets** | Simple on/off binary switch |
| **Multi-Switch Units** | Single gang-box with 2, 3, or 5 individually addressable switches |
| **Safety-Critical (Iron)** | Configurable `max_on_duration`. Backend enforces auto-shutoff |
| **Scheduled Lighting** | Auto on/off during preset time windows |
| **Security Cameras** | Mock snapshot image or mock stream URI |

### 3. Online Synchronization

- **Bidirectional Sync**: App changes reflect in cloud instantly. External updates reflect in app instantly without manual refresh.
- **Implementation**: Use Firestore snapshot listeners (live listeners), not polling.

### 4. Server-Side Safety Cutoffs

- Backend worker/cloud function monitors safety-critical devices
- If `max_on_duration` is breached → automatically flips state to `OFF` + pushes alert
- **Why server-side?** Android background execution limits (API 26+) make phone-based timers unreliable for fire safety

### 5. Reporting

- Track usage data of important devices
- Present however you like (charts, logs, timelines, "hours on today", etc.)

### 6. Companion Hardware Simulator

- **Required** — not optional
- Web dashboard listening to same database
- Visually reflects real-time state changes
- Stand-in for physical IoT hardware

---

## Assessment & Submission Deliverables

| # | Deliverable | Details |
|---|-------------|---------|
| 1 | **Source Code & Git** | Hosted on GitHub + link to final APK |
| 2 | **Technical Documentation** | Concise report covering: &lt;br&gt;• Synchronizing mechanism &lt;br&gt;• Floor representation &lt;br&gt;• Simulator operations |
| 3 | **Demo Video** | Max 25 minutes. All 3 members must present with self-intro + individual contribution |

---

## Tech Stack: Compose + Firebase

### Prerequisites
- Android Studio (Apple Silicon build for M-series Macs)
- JDK bundled with Android Studio
- Galaxy S25 Ultra for physical testing (enable USB Debugging)

### Step 1: Create Project
- **New Project → Empty Activity** (Compose template, NOT "Empty Views Activity")
- **Package name**: `lk.ac.ucsc.smarthome` (lock this in with team)
- **Min SDK**: API 26 (Android 8.0)
- **Language**: Kotlin
- **Build config**: Kotlin DSL

### Step 2: Firebase Setup
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Add project → skip Analytics
3. Register Android app with exact package name
4. Download `google-services.json` → place in `app/` folder
5. In Android Studio: **Tools &gt; Firebase &gt; Cloud Firestore &gt; Connect & Add**

### Step 3: Verify Dependencies (`app/build.gradle.kts`)

```kotlin
dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2025.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation("com.google.firebase:firebase-firestore")
    // NOTE: Do NOT use firebase-firestore-ktx — deprecated July 2025
}

---

## Technical Documentation

### 1. Synchronization Mechanism

The system uses **Firestore real-time snapshot listeners** (`onSnapshot`) — NOT polling. This ensures sub-second propagation of state changes across all connected clients.

#### How it works

```
Android App                    Cloud Firestore                  Simulator (Web)
    │                               │                               │
    │  addSnapshotListener()        │                               │
    │──────────────────────────────►│                               │
    │                               │◄──────────────────────────────│
    │                               │  onSnapshot() (JS SDK)        │
    │                               │                               │
    │  Toggle device (updateDoc)    │                               │
    │──────────────────────────────►│                               │
    │                               │──────────────────────────────►│
    │                               │  Push update to simulator     │
    │◄──────────────────────────────│                               │
    │  Push update to app           │                               │
```

- **Android**: `callbackFlow { addSnapshotListener { ... } }` wrapped in `StateFlow` — collected by Compose via `collectAsStateWithLifecycle()`
- **Simulator**: `onSnapshot(collection(db, ...))` with live DOM re-rendering
- **Result**: Both clients update within ~100–500ms of any write — without polling or manual refresh

#### Data propagation rules
| Trigger | Writer | Readers Updated |
|---------|--------|----------------|
| User toggles device in app | Android app | Simulator (instant) |
| User toggles device in simulator | Web JS | Android app (instant) |
| Iron max duration exceeded | Cloud Function | Both app + simulator + FCM push notification |
| Light scheduled ON/OFF | Cloud Function | Both app + simulator |
| Device stale 24h | Cloud Function | Both app + simulator |

---

### 2. Floor Representation

Devices are placed on a **logical 8×8 grid** overlaid onto a floor plan image.

#### Grid coordinate system
```
(0,0) ──── (7,0)      ← top row
  │                 │
  │   8×8 grid      │
  │                 │
(0,7) ──── (7,7)      ← bottom row
```

- Each device stores `gridX` (column 0–7) and `gridY` (row 0–7) in Firestore
- The Android app renders a `Box` layout with positioned device cells
- The Simulator renders an 8×8 CSS grid with the floor plan PNG as a semi-transparent background overlay (`opacity: 0.22`)
- Occupied cells are identified via a `Map<Pair<Int,Int>, Device>` for O(1) lookup
- Multiple floor plans are supported — each is an independent Firestore document with its own `devices` subcollection

#### Floor plan image
A 2D architectural floor plan (PNG) is stored locally in `simulator/floor_plan.png` and displayed as an overlay behind the device grid in the simulator.

---

### 3. Simulator Operations

The **Hardware Simulator** (`simulator/index.html`) is a single-page web app that acts as a stand-in for physical IoT hardware. It connects to the **same Firebase project** as the Android app.

#### What it does
| Feature | Implementation |
|---------|---------------|
| Real-time device state mirroring | `onSnapshot` listeners — updates grid cells live |
| Device toggling | `updateDoc` writes to Firestore, propagates to Android app |
| Floor plan overlay | Semi-transparent PNG behind the 8×8 CSS grid |
| Iron countdown timer | JavaScript `setInterval` counting seconds remaining |
| Camera panel | Displays `cameraSnapshotUrl` as `<img>` + `cameraStreamUrl` |
| Light schedule display | Shows `turnOnTime` / `turnOffTime` from Firestore |
| Multi-switch child toggles | Individual switch rows with individual toggle buttons |
| Event log with filtering | `usageLogs` collection, filterable by event type |
| Stats bar | Live count of total / ON / OFF / cutoff events |
| Safety cutoff alert | Red banner on `CUTOFF` log event |

#### Connection flow
1. Browser loads `index.html` → Firebase SDK initialises
2. `onSnapshot` on `floorPlans` collection → sidebar list rendered
3. On floor plan select → `onSnapshot` on `floorPlans/{id}/devices` → grid rendered
4. On device click → right-side detail panel expands with type-specific controls
5. `onSnapshot` on `usageLogs` (limit 100, ordered by timestamp desc) → log panel

#### Hosting the Simulator
To make your simulator accessible to friends via a live URL:
1. Ensure you have the Firebase CLI installed: `npm install -g firebase-tools`
2. Login: `firebase login`
3. Deploy: `firebase deploy --only hosting`
4. Firebase will provide a URL like `https://your-project-id.web.app`.

---

### 4. Safety Cutoff Architecture

#### Why server-side?
Android imposes strict background execution limits (API 26+). A phone-based `WorkManager` job could be deferred for hours, making it **unsuitable for fire-hazard safety**. The solution uses **Firebase Cloud Scheduler** — a server-side cron that runs reliably every minute regardless of app state.

#### Iron Safety Cutoff flow
```
Cloud Scheduler (every 1 min)
    │
    ▼
ironSafetyCutoff() function
    │
    ├── Read all IRON devices across all floor plans
    ├── For each IRON device that is ON:
    │       elapsedMin = (now - lastTurnedOnAt) / 60
    │       if elapsedMin >= maxOnDurationMinutes:
    │           ├── Firestore batch.update → status = "OFF"
    │           ├── usageLogs.add → event = "CUTOFF", floorPlanName populated
    │           └── FCM messaging.send → topic "smarthome_alerts"
    │
    └── batch.commit()
```

#### Light Scheduler flow
```
Cloud Scheduler (every 1 min)
    │
    ▼
lightScheduler() function
    │
    ├── Read all LIGHT devices across all floor plans
    ├── Get current time as "HH:mm" (Asia/Colombo TZ)
    ├── For each LIGHT with turnOnTime + turnOffTime configured:
    │       shouldBeOn = isInTimeWindow(now, onTime, offTime)
    │       if shouldBeOn && !currentlyOn → set ON + log SCHEDULE_ON
    │       if !shouldBeOn && currentlyOn → set OFF + log SCHEDULE_OFF
    └── batch.commit()
```

#### Device Health Checker (new)
```
Cloud Scheduler (every 60 min)
    │
    ▼
deviceHealthChecker() function
    │
    ├── Read all ON devices across all floor plans
    ├── For each non-IRON ON device:
    │       if (now - lastTurnedOnAt) > 24h:
    │           → set DISCONNECTED + log DISCONNECTED
    └── batch.commit()
```

---

### 5. Data Model

#### Firestore Collection Structure

```
floorPlans/                        ← Top-level collection
  {floorPlanId}/
    name: "Ground Floor"
    imageUrl: ""
    createdAt: Timestamp
    devices/                       ← Subcollection
      {deviceId}/
        name: "Living Room Light"
        type: "LIGHT"             ← OUTLET | MULTI_SWITCH | IRON | LIGHT | CAMERA
        status: "ON"              ← ON | OFF | ERROR | DISCONNECTED
        gridX: 3
        gridY: 2
        createdAt: Timestamp
        
        # IRON-specific
        maxOnDurationMinutes: 30
        lastTurnedOnAt: Timestamp
        
        # LIGHT-specific
        turnOnTime: "18:00"       ← HH:mm 24h format
        turnOffTime: "06:00"
        
        # CAMERA-specific
        cameraSnapshotUrl: "https://..."
        cameraStreamUrl: "rtsp://..."
        
        # MULTI_SWITCH-specific
        switchCount: 3
        switches: [
          { switchIndex: 0, label: "Fan", status: "ON" },
          { switchIndex: 1, label: "AC", status: "OFF" },
          { switchIndex: 2, label: "Light", status: "ON" }
        ]

usageLogs/                         ← Top-level collection
  {logId}/
    deviceId: "..."
    deviceName: "Iron"
    floorPlanId: "..."
    floorPlanName: "Ground Floor"
    event: "CUTOFF"               ← ON | OFF | CUTOFF | SCHEDULE_ON | SCHEDULE_OFF | DISCONNECTED
    timestamp: Timestamp
```