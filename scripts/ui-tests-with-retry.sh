#!/usr/bin/env bash
# Instrumented UI tests with retries for emulator flakes.
#
# Runs the whole suite once; if it fails, re-runs only the test methods that
# failed, up to UI_TEST_RETRIES more times (default 2), on the same emulator.
# Tests that pass only on a retry are reported as warnings so flakes stay
# visible; the job fails only if a test fails on every attempt.
set -u

RESULTS=app/build/outputs/androidTest-results/connected
RETRIES=${UI_TEST_RETRIES:-2}

run() {
  prepare_emulator
  ./gradlew --console=plain :app:connectedDebugAndroidTest "$@"
}

# Tests that send key events wait for the activity's window to gain input
# focus. On CI emulators that can never happen when something sits on top of
# the app: a system "isn't responding" (ANR) dialog, the keyguard, or a
# screen that went to sleep — and every retry on the same emulator then hits
# the same wall. Clear all of that before each attempt.
prepare_emulator() {
  echo "Focused window before run: $(focused_window)"
  adb shell settings put global hide_error_dialogs 1 || true
  adb shell settings put system screen_off_timeout 2147483647 || true
  adb shell svc power stayon true || true
  adb shell input keyevent KEYCODE_WAKEUP || true
  adb shell wm dismiss-keyguard || true
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null || true
  adb shell input keyevent KEYCODE_HOME || true
  sleep 2
  echo "Focused window after prep: $(focused_window)"
}

focused_window() {
  adb shell dumpsys window 2>/dev/null | grep -m1 mCurrentFocus | tr -d '\r' | sed 's/^ *//'
}

# Comma-separated Class#method list of failed/errored test cases.
failed_tests() {
  python3 - "$RESULTS" <<'EOF'
import glob, sys, xml.etree.ElementTree as ET
failed = []
for path in glob.glob(f"{sys.argv[1]}/**/*.xml", recursive=True):
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        continue
    for case in root.iter("testcase"):
        if case.find("failure") is not None or case.find("error") is not None:
            name = f"{case.get('classname')}#{case.get('name')}"
            if name not in failed:
                failed.append(name)
print(",".join(failed))
EOF
}

run && exit 0

for attempt in $(seq 1 "$RETRIES"); do
  failed=$(failed_tests)
  if [ -z "$failed" ]; then
    echo "::error::UI tests failed but no failed test cases were recorded; not retrying."
    exit 1
  fi
  echo "::warning::UI test retry ${attempt}/${RETRIES} for: ${failed}"
  if run "-Pandroid.testInstrumentationRunnerArguments.class=${failed}"; then
    echo "::warning::Passed only on retry ${attempt} (flaky): ${failed}"
    exit 0
  fi
done

echo "::error::UI tests still failing after ${RETRIES} retries: $(failed_tests) (focused window: $(focused_window))"
exit 1
