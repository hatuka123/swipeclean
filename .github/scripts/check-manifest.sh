#!/usr/bin/env bash
# Fails the build if the merged release manifest requests anything Play would reject for this
# app or that contradicts the "media never leaves the device" promise.
set -euo pipefail
manifest=$(find app/build/intermediates -path '*release*' -name AndroidManifest.xml | grep -i merged | head -1)
[ -n "$manifest" ] || { echo "merged release manifest not found"; exit 1; }
echo "Checking $manifest"
forbidden="MANAGE_EXTERNAL_STORAGE|android.permission.INTERNET|SCHEDULE_EXACT_ALARM|USE_EXACT_ALARM|WRITE_EXTERNAL_STORAGE\""
if grep -E "uses-permission[^>]*($forbidden)" "$manifest"; then
  echo "::error::Forbidden permission in merged manifest"
  exit 1
fi
grep -E "uses-permission" "$manifest"
echo "Manifest OK"
