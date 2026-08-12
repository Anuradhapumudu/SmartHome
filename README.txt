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