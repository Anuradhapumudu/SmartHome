# SCS 3311: Smart Home Monitoring & Control System — Project Guide

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