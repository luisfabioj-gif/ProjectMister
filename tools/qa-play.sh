#!/usr/bin/env bash
set -euo pipefail
suite="${1:?Expected minimum, match, deep or latest}"
runner=com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner
mkdir -p qa-output
trap 'adb logcat -d > qa-output/logcat.txt; adb pull /sdcard/Android/data/com.projectmister.game/files/qa qa-output/ >/dev/null 2>&1 || true' EXIT
run_mode() {
    local mode="$1" country="${2:-}" output
    output="qa-output/$mode${country:+-$country}.txt"
    local extras=()
    if [[ -n "$country" ]]; then extras=(-e country "$country"); fi
    timeout 900s adb shell am instrument -w -e mode "$mode" "${extras[@]}" "$runner" | tee "$output"
    grep -q '^PASS' "$output"
    if grep -Eq 'FAIL|INSTRUMENTATION_FAILED|Process crashed' "$output"; then exit 1; fi
}
adb install -r /tmp/boss-baseline/app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/test-manual/qa.apk
adb logcat -c || echo 'Fresh emulator log could not be cleared; crash checks still apply.'
run_mode seed-feedback
grep -q 'PASS baseline career seeded' qa-output/seed-feedback.txt
adb install -r app/build/outputs/apk/debug/app-debug.apk
run_mode editor-lock
grep -q 'PASS editor lock, mutation guard, privacy and free career verified' qa-output/editor-lock.txt
cp qa-output/editor-lock.txt qa-output/debug-editor-lock.txt
if [[ "$suite" != minimum ]]; then
    for mode in matchflow presentation refinements fixes premium natural-match; do run_mode "$mode"; done
    grep -q 'PASS natural full match advances without stalls and preserves its result' qa-output/natural-match.txt
    adb shell wm size 720x1280
    adb shell wm density 320
    run_mode compact
    grep -q 'PASS compact landscape formations usable' qa-output/compact.txt
    adb shell wm size reset
    adb shell wm density reset
fi
if [[ "$suite" == deep ]]; then
    for mode in feedback full rules uefa-players cup-players divisions national-cups; do run_mode "$mode"; done
    grep -q 'PASS all twenty standalone and twenty linked division careers verified' qa-output/divisions.txt
    grep -q 'PASS career backup export, validation, cancellation, restore and load verified' qa-output/divisions.txt
    for country in ENG ES PT DE IT FR NL BE SCO TR; do
        run_mode europe "$country"
        grep -q "PASS $country combined UEFA qualifying domestic calendars two seasons watched quick matches exact archives and backups verified" "qa-output/europe-$country.txt"
    done
    for country in ENG ES IT FR NL BE SCO TR; do
        run_mode expanded-cups "$country"
        grep -q "PASS $country recurring cups watched and quick matches two seasons reloads history and backup rejection verified" "qa-output/expanded-cups-$country.txt"
    done
    # Capture genuine game screens at the recommended Play listing aspect ratio.
    adb shell wm size 1080x1920
    adb shell wm density 420
fi
bash tools/qa-release-bundle.sh
run_mode editor-lock
cp qa-output/editor-lock.txt qa-output/release-editor-lock.txt
if [[ "$suite" == latest ]]; then bash tools/qa-configured-bundle.sh; fi
if [[ "$suite" != minimum ]]; then bash tools/qa-key-migration.sh play; fi
adb shell wm size reset
adb shell wm density reset
sha256sum app/build/outputs/apk/debug/app-debug.apk app/build/outputs/bundle/release/app-release.aab > qa-output/build-sha256.txt
adb logcat -d > qa-output/logcat.txt
if grep -A 80 'FATAL EXCEPTION' qa-output/logcat.txt | grep -q 'com.projectmister.game'; then exit 1; fi
