#!/usr/bin/env bash
# Generates the test gallery pushed to the emulator:
#   SeedCamera: 10 photos + 2 videos, SeedScreens: 5 photos, Download: 3 photos.
set -euo pipefail
out="${1:-seed}"
mkdir -p "$out/SeedCamera" "$out/SeedScreens" "$out/Download"

photo() { # $1 = file, $2 = hue shift, $3 = size
  ffmpeg -loglevel error -y -f lavfi -i "testsrc2=size=$3:rate=1" -vf "hue=h=$2" -frames:v 1 -q:v 3 "$1"
}

for i in $(seq 1 10); do photo "$out/SeedCamera/IMG_000$i.jpg" $((i * 33)) 1200x1600; done
for i in $(seq 1 5); do photo "$out/SeedScreens/Screenshot_$i.png" $((i * 60)) 540x1170; done
for i in $(seq 1 3); do photo "$out/Download/download_$i.jpg" $((i * 90)) 800x800; done
for i in 1 2; do
  ffmpeg -loglevel error -y -f lavfi -i "testsrc2=size=720x1280:rate=30" -t $((i * 3)) \
    -pix_fmt yuv420p -c:v libx264 "$out/SeedCamera/VID_000$i.mp4"
done
ls -R "$out"
