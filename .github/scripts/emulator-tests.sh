#!/usr/bin/env bash
# Runs inside android-emulator-runner. Seeds the gallery, then runs the instrumented tests in
# three permission states: none (onboarding + real permission dialog), full, and (API 34+)
# partial "selected photos" access. Uses `am instrument` directly instead of
# connectedAndroidTest so the app is not uninstalled before screenshots are pulled.
set -uo pipefail
api="$1"
pkg=com.hatuka.swipeclean
runner="$pkg.test/androidx.test.runner.AndroidJUnitRunner"
out=emulator-out
mkdir -p "$out"
failed=0

adb wait-for-device
adb shell settings put global package_verifier_enable 0 || true

echo "::group::Seed media"
# boot_completed fires before shared storage is mounted on newer images; wait for it.
for i in $(seq 1 60); do adb shell ls /sdcard/Download > /dev/null 2>&1 && break; sleep 3; done
adb shell mkdir -p /sdcard/DCIM/SeedCamera /sdcard/Pictures/SeedScreens /sdcard/Download
# One file at a time: pushing "dir/." makes adb create "dir/./file", which FUSE rejects.
push_dir() { for f in "$1"/*; do adb push "$f" "$2/" > /dev/null || echo "push failed: $f"; done; }
push_dir seed/SeedCamera /sdcard/DCIM/SeedCamera
push_dir seed/SeedScreens /sdcard/Pictures/SeedScreens
push_dir seed/Download /sdcard/Download
# WhatsApp keeps its media under Android/media on Android 11+; other apps cannot move files
# out of there in place (the app falls back to copy + trash).
wa="/sdcard/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images"
adb shell mkdir -p "\"$wa\"" || true
for f in seed/WhatsApp/*; do adb push "$f" "$wa/" > /dev/null || echo "push failed: $f"; done
adb shell content call --uri content://media --method scan_volume --arg external_primary || true
sleep 5
adb shell content query --uri content://media/external/file --projection _display_name:bucket_display_name --where "media_type!=0" || true
echo "::endgroup::"

# Let the freshly booted system (System UI, permission controller) settle before UI tests.
sleep 45
adb shell input keyevent 82 || true
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

perms="android.permission.READ_EXTERNAL_STORAGE"
if [ "$api" -ge 33 ]; then perms="android.permission.READ_MEDIA_IMAGES android.permission.READ_MEDIA_VIDEO"; fi
if [ "$api" -ge 34 ]; then perms="$perms android.permission.READ_MEDIA_VISUAL_USER_SELECTED"; fi

run() { # $1 = label, $2 = test class
  echo "::group::$1"
  adb shell am force-stop $pkg
  adb shell am instrument -w -r -e class "$pkg.$2" "$runner" | tee "$out/$1.txt"
  if grep -qE "FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|INSTRUMENTATION_CODE: 0" "$out/$1.txt" \
     || ! grep -q "^OK (" "$out/$1.txt"; then
    echo "::error::$1 failed on API $api"
    failed=1
  fi
  echo "::endgroup::"
}

# 1) No access: onboarding and the real system permission dialog.
for p in $perms; do adb shell pm revoke $pkg "$p" 2>/dev/null || true; done
run "no-access" OnboardingTest

# 2) Full access: folder list, filters, navigation.
for p in $perms; do adb shell pm grant $pkg "$p" 2>/dev/null || true; done
run "full-access" FolderListTest
run "video" VideoPlaybackTest
run "swipe-flow" SwipeFlowTest
run "move-flow" MoveFlowTest
run "whatsapp-move" WhatsAppMoveTest

# 3) Android 14+: only "selected photos" access.
if [ "$api" -ge 34 ]; then
  adb shell pm revoke $pkg android.permission.READ_MEDIA_IMAGES || true
  adb shell pm revoke $pkg android.permission.READ_MEDIA_VIDEO || true
  adb shell pm grant $pkg android.permission.READ_MEDIA_VISUAL_USER_SELECTED || true
  run "partial-access" PartialAccessTest
fi

adb pull "/sdcard/Android/data/$pkg/files/shots" "$out/" || echo "no screenshots pulled"
adb logcat -d > "$out/logcat.txt" || true
exit $failed
