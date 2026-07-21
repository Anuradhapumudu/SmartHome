#!/bin/bash
# ──────────────────────────────────────────────────────────────────────────────
# SmartHome — One-Click Deploy Script
# Run this from the project root: bash deploy.sh
#
# Requirements:
#   • firebase-tools installed: npm install -g firebase-tools
#   • Logged in to Firebase: firebase login
#   • Firebase project on Blaze (pay-as-you-go) plan (required for Cloud Functions)
# ──────────────────────────────────────────────────────────────────────────────

set -e
echo "🚀 SmartHome Deploy Script"
echo "────────────────────────────────────────"

# 1. Install Cloud Function dependencies
echo "📦 Installing Cloud Function dependencies..."
cd functions
npm install
cd ..

# 2. Deploy Firestore security rules
echo "🔒 Deploying Firestore security rules..."
firebase deploy --only firestore:rules

# 3. Deploy Cloud Functions
echo "☁️  Deploying Cloud Functions (iron safety + light scheduler)..."
firebase deploy --only functions

# 4. Build release APK
echo "📱 Building release APK..."
./gradlew assembleRelease

echo ""
echo "✅ All done!"
echo "   • Cloud Functions: https://console.firebase.google.com/project/smarthome-59f93/functions"
echo "   • Release APK: app/build/outputs/apk/release/app-release-unsigned.apk"
echo "   • Hardware Simulator: open simulator/index.html in any browser"
echo ""
echo "📌 Next steps:"
echo "   1. Upload the APK to GitHub Releases"
echo "   2. Open simulator/index.html to verify real-time sync"
echo "   3. Record the demo video (< 25 min, all 3 members)"
