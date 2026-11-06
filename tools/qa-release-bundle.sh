#!/usr/bin/env bash
set -euo pipefail
# This exercises Play-style split packaging with the PUBLIC TEST KEY only.
# It does not sign the AAB for production or publish anything to Play.
bundletool=app/build/toolchain/bundletool.jar
bundle=app/build/outputs/bundle/release/app-release.aab
mkdir -p app/build/bundle-qa qa-output
java -jar "$bundletool" validate --bundle="$bundle"
java -jar "$bundletool" dump manifest --bundle="$bundle" > qa-output/release-manifest.xml
python3 - <<'PY'
import xml.etree.ElementTree as ET
root=ET.parse('qa-output/release-manifest.xml').getroot()
a='{http://schemas.android.com/apk/res/android}'
assert root.attrib['package']=='com.projectmister.game'
assert root.find('application').get(a+'debuggable','false')=='false'
assert root.find('uses-sdk').get(a+'targetSdkVersion')=='36'
permissions={p.attrib[a+'name'] for p in root.findall('uses-permission')}
if root.get(a+'versionCode')=='44':
    assert root.find('uses-sdk').get(a+'minSdkVersion')=='26'
    assert root.find("application/meta-data[@"+a+"name='com.google.android.play.billingclient.version']").get(a+'value')=='9.1.0'
    assert 'com.android.vending.BILLING' in permissions
    allowed={'com.android.vending.BILLING','android.permission.INTERNET','android.permission.ACCESS_NETWORK_STATE',
        'android.permission.WAKE_LOCK','android.permission.RECEIVE_BOOT_COMPLETED',
        'com.projectmister.game.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'}
    assert permissions <= allowed, 'Unexpected release permission: '+str(permissions-allowed)
    activities=root.findall('application/activity')
    assert [p.attrib[a+'name'] for p in activities if p.get(a+'exported')=='true']==['com.projectmister.game.MainActivity']
    print('PASS release manifest: target 36, min 26, non-debuggable, billing 9.1.0, no sensitive permissions')
else:
    assert not permissions, 'Unexpected release permission'
    print('PASS release manifest: correct package, non-debuggable, target 36, no permissions')
PY
java -jar "$bundletool" get-device-spec --output=app/build/bundle-qa/device.json --overwrite
java -jar "$bundletool" build-apks --bundle="$bundle" --output=app/build/bundle-qa/release-test.apks --device-spec=app/build/bundle-qa/device.json --overwrite --ks=project-mister-dev.keystore --ks-key-alias=projectmister --ks-pass=pass:projectmisterdev --key-pass=pass:projectmisterdev
java -jar "$bundletool" get-size total --apks=app/build/bundle-qa/release-test.apks --device-spec=app/build/bundle-qa/device.json > qa-output/release-delivery-size.txt
adb shell am force-stop com.projectmister.game
java -jar "$bundletool" install-apks --apks=app/build/bundle-qa/release-test.apks
adb shell pm path com.projectmister.game > qa-output/release-installed-paths.txt
timeout 180s adb shell am instrument -w -e mode release com.projectmister.game.test/com.projectmister.game.qa.SmokeRunner | tee qa-output/release.txt
grep -q 'PASS release bundle upgrade and feature navigation verified' qa-output/release.txt
! grep -q 'FAIL\|INSTRUMENTATION_FAILED' qa-output/release.txt
