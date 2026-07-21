SCS 3311: Mobile Application Design &
Development Mini-Project
Problem Specification: Smart Home Monitoring &
Control System
Project Overview
The objective of this project is to build a mobile Smart Home Monitoring and Control system
consisting of a mobile application client and a companion cloud-connected hardware simulation
system. The system allows users to interact with multiple floor plans, monitor and toggle
equipment with differing specialized capabilities (e.g., individual power outlets, multi-switch
units, lighting, safety-critical devices like irons, and security cameras), and execute automated
backend-driven safety rules.
Core Functional Requirements
Multi-Floor Interactive Dashboard (Mobile Client)
Floor Plan Layouts: The app must support adding and managing multiple house-floor
plans. Users should be able to view an abstract(simple) grid mapping overlaid onto specific
floor layouts. You can use some free sample plans for demo purpose.
Device Control UI: Toggling device states must reactively update on the user interface.
Devices must display their current operational status ( ON , OFF , ERROR , DISCONNECTED ).
Heterogeneous Device Profiles
Electrical Outlets: Simple single-node binaries representing a continuous power supply.
Multi-Switch Units: A single physical gang-box unit managing a variable number of
separate, individually addressable switches (e.g., a 2,3 or 5 switch unit mapped to a single
entity in the system).
Scheduling of Operation: Specialized slots assigned to appliances prone to fire hazards
(e.g., clothing irons). These must support a maximum permissible active duration
configuration. Also some light bulbs may be set to turn on and off automatically during a
preset time period.
Security Cameras: Interface elements dedicated to monitoring spaces via mock camera
snapshots / mock uri streams.
Online Synchronization
Bidirectional Sync: State changes made in the mobile application must reflect quickly in
the cloud database, and any state updates driven externally must quickly update the mobile
viewport without manual refresh triggers.
Server-Side Safety Cutoffs: To protect life and property, safety-critical slots may be
regulated by a backend cloud listener or a worker process. If a high-power device
configuration's max_on_duration is breached, an automatic flip of the database state to
OFF and push of an alert is important.
Reporting: Usage data of the important devices can be tracked from the mobile app. You
may decide the best way to present this information to the user.
Companion Hardware Simulator
To bypass physical component constraints while maintaining a realistic IoT development
experience, you may deploy a web-based Hardware Simulator Dashboard. This
simulator represents the physical home appliances, listening directly to database updates
and visually reflecting changes online.
Assessment & Submission Deliverables
Evaluation will be based on both group cohesion and individual defense.
1. Source Code & Git: Hosted on a platform like GitHub and link to final apk must be
shared.
2. Technical Documentation: A concise report outlining the synchronizing mechanism,
floor representation and simulator operations.
3. Recorded Video for Demonstration: All three members must present in the video with
presenter introduction and their contribution. Do not exceed 25 minutes of video.

This above is the given project right 

now this below is the I found things 
Looking at this, it's a group mini-project for a Mobile App Design & Development course — three of you build a Smart Home system that's really three connected pieces: a mobile app, a cloud database, and a companion web dashboard, all kept in sync in real time, plus a background system that can shut off dangerous appliances on its own.

That safety worker is really a fourth participant — it reads and writes the same database independently of the phone, watching for anything that's been left on too long.

Multi-floor dashboard (the mobile app)

Support adding and managing several floor plans, positioning devices with a simple grid overlay rather than pixel-perfect placement — free sample floor plan images are fine for the demo.
Tapping a device toggles it, and the screen has to update reactively for any change, not just your own taps. Every device shows one of four states: ON, OFF, ERROR, DISCONNECTED.

Device types — where the real design complexity lives
"Device" isn't one shape of data here — you're modeling several different behaviors:

Electrical outlet — plain on/off.
Multi-switch unit — one physical gang-box containing 2, 3, or 5 independently switchable channels. This needs a parent device with a list of child switches, not a single boolean.
Fire-hazard devices (irons, etc.) — a configurable max_on_duration that the system actually has to enforce, not just store.
Scheduled lighting — on/off automatically during a preset time window. A separate concern from the fire-hazard cutoff above — this one's just a timer, no safety enforcement needed.
Security camera — a mock snapshot image or mock stream URL, not a real camera integration.

Real-time sync + the safety cutoff — the engine of the project

"Without manual refresh triggers" is really ruling out request/poll patterns. You want a live listener setup — Firestore/Realtime Database-style snapshot listeners, or a WebSocket push channel — so both directions of sync happen instantly.
The cutoff has to live server-side: a cloud function, worker, or cron job independently tracks how long a safety-critical device has been on, and if it crosses max_on_duration, that backend flips it to OFF and pushes an alert — not the phone. That lines up with the Android background-processing constraints you've been studying: the OS aggressively suspends background app processes, especially on 8.0+, so a phone app genuinely can't be trusted to catch a fire hazard reliably on its own. The spec is forcing the safer architecture on purpose here.
Usage reporting is loosely specified — track the important devices and present it however you like. Even a simple "hours on today" per device would satisfy this.

Hardware simulator
A separate web dashboard listening to that same database and visually reflecting whatever's happening — your stand-in for real IoT hardware. Worth flagging: the overview names this as one of the two core system components ("a mobile application client and a companion cloud-connected hardware simulation system"), so treat it as required, not optional. The "you may deploy a web-based..." phrasing is about the suggested implementation, not whether you need one at all.

What you actually submit

Source code & Git — a GitHub repo, plus a link to the final APK. Worth noting: an APK specifically means Android, so whatever stack you pick (native Kotlin/Java, Flutter, React Native) needs to build to Android — a pure iOS app wouldn't satisfy this on its own. Convenient that you've got a Galaxy S25 Ultra to test on real hardware once you're past the emulator.
Technical documentation — a concise report, and the spec names exactly three things it wants covered: the sync mechanism, floor plan representation, and simulator operation. Make sure those three show up explicitly, even if you add more.
Demo video, 25 minutes max — all three of you present, each with a self-intro and a clear statement of your individual contribution.

Got it — Compose + Firebase. Here's the setup, start to finish:

1. Prerequisites
Grab Android Studio for Apple Silicon specifically (the M-series build, not Intel) if you don't have it — JDK comes bundled, nothing else to install separately. Current stable is the 2025.3.2 release. 
Medium

2. Create the project

New Project → Empty Activity — make sure it's the Compose one. Android Studio still lists a separate Views-based template too (sometimes labeled "Empty Views Activity"), easy to grab by mistake.
Fill in:
Package name: reverse-domain style, e.g. lk.ac.ucsc.smarthome — lock this in with your teammates now, it gets baked into the Firebase config next and is annoying to change later
Minimum SDK: I'd go API 26 (Android 8.0) — that's exactly where the background-execution limits you've been studying kick in, so your target devices will actually behave the way you've learned about. Not a hard requirement, just a good fit
Language: Kotlin · Build configuration language: Kotlin DSL — this has been the default for new projects since Android Studio Giraffe, so it'll likely already be selected 
Android Developers
Finish, let Gradle sync

3. Create the Firebase project
console.firebase.google.com → Add project → name it → Analytics is optional, skip it for a course project.

4. Wire the app to Firebase
Back in Android Studio: Tools > Firebase opens the Firebase Assistant. Search "Firestore" → open Cloud Firestore → Connect to Firebase (registers your app using its package name) → Add Cloud Firestore to your app. That last click handles the annoying parts: downloads google-services.json into your app/ folder and adds the right plugin + dependencies automatically.

5. Double-check what it added
Open app/build.gradle.kts, confirm something like:

kotlin
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation("com.google.firebase:firebase-firestore")
}

Gotcha: a lot of tutorials online still show firebase-firestore-ktx. Skip that — Firebase stopped releasing new KTX module versions in July 2025 and removed them from the BoM entirely as of v34.0.0, folding that functionality into the main modules. So firebase-firestore alone is correct now. Also, the BoM is currently around v34.15 — it moves fast, so check the actual latest on Firebase's release notes page rather than trusting a number I give you today. 
Google
Firebase

6. Sanity-check the connection
Before building anything real, confirm read/write actually works. Drop this in MainActivity.kt's onCreate:

kotlin
import com.google.firebase.firestore.FirebaseFirestore

val db = FirebaseFirestore.getInstance()
db.collection("test").document("ping")
    .set(mapOf("hello" to "world"))
    .addOnSuccessListener { Log.d("FirebaseTest", "Write ok") }
    .addOnFailureListener { e -> Log.e("FirebaseTest", "Write failed", e) }

Run it, then check the Firestore data tab in the Firebase Console for a test/ping doc. If it's there, the plumbing works — delete the snippet once confirmed.

7. Folders for where the actual features go

com.yourteam.smarthome/
├─ data/            → Device, FloorPlan data classes + a FirestoreRepository
├─ ui/
│  ├─ floorplan/     → floor plan grid screen
│  ├─ devicecontrol/ → device toggle screens per type
│  ├─ reporting/     → usage screens
│  └─ theme/
├─ navigation/       → NavHost + routes
└─ MainActivity.kt

8. Run it
Easiest path on Apple Silicon: skip the emulator, enable Developer Options + USB debugging on your Galaxy S25 Ultra, plug it in, select it as the run target. If you want an emulator anyway, pick an ARM64 system image in the AVD Manager — x86_64 images run painfully slow (or not at all) on M-series Macs.

9. Two loose ends

Git: tick "Create Git repository" during project creation (or VCS > Enable Version Control Integration later) — Source Code & Git is literally one of your deliverables.
Firestore in test mode: those wide-open rules auto-expire in 30 days. Not urgent now, just don't let it catch you off guard near submission.