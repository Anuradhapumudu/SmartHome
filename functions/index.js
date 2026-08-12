/**
 * SmartHome Firebase Cloud Functions
 * ────────────────────────────────────────────────────────────────────────────
 *
 * Function 1 — ironSafetyCutoff  (runs every minute via Cloud Scheduler)
 *   Scans all IRON devices across all floor plans.
 *   If a device is ON and (now - lastTurnedOnAt) > maxOnDurationMinutes:
 *     • Sets status → "OFF"
 *     • Writes a usageLogs entry with event = "CUTOFF"
 *     • Sends an FCM notification to the app (data message)
 *
 * Function 2 — lightScheduler  (runs every minute via Cloud Scheduler)
 *   Scans all LIGHT devices across all floor plans.
 *   Compares current UTC time to turnOnTime / turnOffTime.
 *   If the light should be ON but is OFF → sets ON + logs SCHEDULE_ON
 *   If the light should be OFF but is ON → sets OFF + logs SCHEDULE_OFF
 *
 * ────────────────────────────────────────────────────────────────────────────
 * Deployment:
 *   cd functions && npm install
 *   firebase deploy --only functions
 *
 * Required Firebase project plan: Blaze (pay-as-you-go)
 * Required Firebase services: Firestore, Cloud Functions, FCM
 */

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { logger } = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

// ─────────────────────────────────────────────────────────────────────────────
// Helper: collect every device across all floor plans that matches a filter
// Returns: Array of { floorPlanId, deviceId, device }
// ─────────────────────────────────────────────────────────────────────────────

async function getAllDevicesOfType(typeFilter) {
  const results = [];
  const floorPlansSnap = await db.collection("floorPlans").get();

  await Promise.all(
    floorPlansSnap.docs.map(async (planDoc) => {
      const devicesSnap = await planDoc.ref
        .collection("devices")
        .where("type", "==", typeFilter)
        .get();
      devicesSnap.docs.forEach((devDoc) => {
        results.push({
          floorPlanId: planDoc.id,
          deviceId: devDoc.id,
          device: devDoc.data(),
        });
      });
    })
  );

  return results;
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: write a usageLogs entry
// ─────────────────────────────────────────────────────────────────────────────

async function logEvent(deviceId, deviceName, floorPlanId, floorPlanName, event) {
  await db.collection("usageLogs").add({
    deviceId,
    deviceName: deviceName || "",
    floorPlanId,
    floorPlanName: floorPlanName || "",
    event,
    timestamp: admin.firestore.FieldValue.serverTimestamp(),
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: send FCM alert to the app's topic (or all tokens)
// Using the "smarthome_alerts" topic — app subscribes on first launch.
// ─────────────────────────────────────────────────────────────────────────────

async function sendCutoffAlert(deviceName, minutesOn) {
  try {
    const message = {
      topic: "smarthome_alerts",
      data: {
        title: "⚠ Iron Safety Cutoff",
        body: `"${deviceName}" was automatically switched OFF after ${minutesOn} min (max duration exceeded).`,
      },
      android: {
        priority: "high",
      },
    };
    const response = await messaging.send(message);
    logger.info("FCM cutoff alert sent:", response);
  } catch (err) {
    // FCM errors are non-fatal — the device was already turned off in Firestore
    logger.warn("FCM send failed (non-fatal):", err.message);
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// FUNCTION 1 — Iron Safety Cutoff
// Trigger: every minute
// ─────────────────────────────────────────────────────────────────────────────

exports.ironSafetyCutoff = onSchedule(
  {
    schedule: "every 1 minutes",
    timeZone: "Asia/Colombo", // adjust to your time zone
    region: "us-central1",
  },
  async (event) => {
    logger.info("ironSafetyCutoff: checking all IRON devices");

    const ironDevices = await getAllDevicesOfType("IRON");
    const nowMs = Date.now();
    const batch = db.batch();
    const cutoffPromises = [];

    // Cache floor plan names to avoid repeated reads
    const planNameCache = {};
    const getPlanName = async (planId) => {
      if (planNameCache[planId]) return planNameCache[planId];
      const snap = await db.collection("floorPlans").doc(planId).get();
      planNameCache[planId] = snap.exists ? (snap.data().name || "") : "";
      return planNameCache[planId];
    };

    for (const { floorPlanId, deviceId, device } of ironDevices) {
      if (device.status !== "ON") continue;

      const lastOnSec = device.lastTurnedOnAt?.seconds;
      if (!lastOnSec) continue;

      const elapsedMin = (nowMs - lastOnSec * 1000) / 60_000;
      const maxMin = device.maxOnDurationMinutes || 30;

      if (elapsedMin >= maxMin) {
        logger.info(
          `CUTOFF: device "${device.name}" (${deviceId}) in plan ${floorPlanId}`,
          `elapsed=${elapsedMin.toFixed(1)}min max=${maxMin}min`
        );

        // Flip status to OFF in batch
        const docRef = db
          .collection("floorPlans")
          .doc(floorPlanId)
          .collection("devices")
          .doc(deviceId);
        batch.update(docRef, { status: "OFF" });

        // Schedule log + notification (can't await inside batch)
        cutoffPromises.push(
          getPlanName(floorPlanId).then((planName) =>
            logEvent(deviceId, device.name, floorPlanId, planName, "CUTOFF")
          ),
          sendCutoffAlert(device.name, Math.round(elapsedMin))
        );
      }
    }

    await batch.commit();
    await Promise.all(cutoffPromises);

    logger.info("ironSafetyCutoff: done");
  }
);

// ─────────────────────────────────────────────────────────────────────────────
// FUNCTION 2 — Light Auto-Toggle Scheduler
// Trigger: every minute
// ─────────────────────────────────────────────────────────────────────────────

exports.lightScheduler = onSchedule(
  {
    schedule: "every 1 minutes",
    timeZone: "Asia/Colombo", // adjust to your time zone
    region: "us-central1",
  },
  async (event) => {
    logger.info("lightScheduler: checking all LIGHT devices");

    const lightDevices = await getAllDevicesOfType("LIGHT");

    // Current time as "HH:mm" in 24h format (Asia/Colombo)
    const now = new Date();
    const hhmm = now.toLocaleTimeString("en-GB", {
      timeZone: "Asia/Colombo",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }); // → "18:05"

    const batch = db.batch();
    const logPromises = [];

    for (const { floorPlanId, deviceId, device } of lightDevices) {
      const onTime = device.turnOnTime || "";
      const offTime = device.turnOffTime || "";

      if (!onTime || !offTime) continue; // schedule not configured

      const shouldBeOn = isInTimeWindow(hhmm, onTime, offTime);
      const currentlyOn = device.status === "ON";

      if (shouldBeOn && !currentlyOn) {
        logger.info(`SCHEDULE_ON: "${device.name}" (${deviceId})`);
        const docRef = db
          .collection("floorPlans")
          .doc(floorPlanId)
          .collection("devices")
          .doc(deviceId);
        batch.update(docRef, { status: "ON" });
        logPromises.push(
          logEvent(deviceId, device.name, floorPlanId, "", "SCHEDULE_ON")
        );
      } else if (!shouldBeOn && currentlyOn) {
        logger.info(`SCHEDULE_OFF: "${device.name}" (${deviceId})`);
        const docRef = db
          .collection("floorPlans")
          .doc(floorPlanId)
          .collection("devices")
          .doc(deviceId);
        batch.update(docRef, { status: "OFF" });
        logPromises.push(
          logEvent(deviceId, device.name, floorPlanId, "", "SCHEDULE_OFF")
        );
      }
    }

    await batch.commit();
    await Promise.all(logPromises);

    logger.info("lightScheduler: done");
  }
);

// ─────────────────────────────────────────────────────────────────────────────
// Utility: check whether `currentTime` (HH:mm) falls within the window
// [onTime, offTime). Handles overnight windows (e.g. 22:00 → 06:00).
// ─────────────────────────────────────────────────────────────────────────────

function isInTimeWindow(currentTime, onTime, offTime) {
  const toMin = (hhmm) => {
    const [h, m] = hhmm.split(":").map(Number);
    return h * 60 + m;
  };

  const cur = toMin(currentTime);
  const on = toMin(onTime);
  const off = toMin(offTime);

  if (on <= off) {
    // Same-day window: 08:00 → 22:00
    return cur >= on && cur < off;
  } else {
    // Overnight window: 22:00 → 06:00
    return cur >= on || cur < off;
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// FUNCTION 3 — Device Health Checker
// Trigger: every hour
// Marks devices DISCONNECTED if their state hasn't changed in 24+ hours
// and they are currently showing as ON (simulates hardware going offline).
// ─────────────────────────────────────────────────────────────────────────────

exports.deviceHealthChecker = onSchedule(
  {
    schedule: "every 60 minutes",
    timeZone: "Asia/Colombo",
    region: "us-central1",
  },
  async (event) => {
    logger.info("deviceHealthChecker: scanning for stale ON devices");

    const floorPlansSnap = await db.collection("floorPlans").get();
    const nowMs = Date.now();
    const staleThresholdMs = 24 * 60 * 60 * 1000; // 24 hours
    const batch = db.batch();
    const logPromises = [];
    let staleCount = 0;

    await Promise.all(
      floorPlansSnap.docs.map(async (planDoc) => {
        const devicesSnap = await planDoc.ref.collection("devices")
          .where("status", "==", "ON")
          .get();

        devicesSnap.docs.forEach((devDoc) => {
          const device = devDoc.data();
          const updatedAt = device.lastTurnedOnAt?.seconds;
          if (!updatedAt) return;

          const ageMs = nowMs - updatedAt * 1000;
          // Only mark as DISCONNECTED if the device has been ON for more than 24h
          // and it is NOT an IRON (those are handled by ironSafetyCutoff)
          if (ageMs > staleThresholdMs && device.type !== "IRON") {
            logger.info(`DISCONNECTED: "${device.name}" (${devDoc.id}) - stale for ${(ageMs / 3_600_000).toFixed(1)}h`);
            batch.update(devDoc.ref, { status: "DISCONNECTED" });
            logPromises.push(
              logEvent(devDoc.id, device.name, planDoc.id, planDoc.data().name || "", "DISCONNECTED")
            );
            staleCount++;
          }
        });
      })
    );

    await batch.commit();
    await Promise.all(logPromises);

    logger.info(`deviceHealthChecker: done. Marked ${staleCount} device(s) as DISCONNECTED.`);
  }
);
