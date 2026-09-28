#!/usr/bin/env bash
# Offline SDK build for this dependency-free Java Android project.
set -euo pipefail
: "${ANDROID_HOME:?Set ANDROID_HOME to Android SDK}"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17}"
bt="$ANDROID_HOME/build-tools/35.0.0"
jar="$ANDROID_HOME/platforms/android-35/android.jar"
out=app/build/manual
mkdir -p "$out/generated" "$out/classes" "$out/dex"
python3 - <<'PY'
from pathlib import Path
import re
s=Path('app/src/main/AndroidManifest.xml').read_text().replace('<manifest ', '<manifest package="com.projectmister.game" ',1).replace('<application','<application android:debuggable="true"',1)
Path('app/build/manual/AndroidManifest.xml').write_text(s)
g=Path('app/build.gradle').read_text()
Path('app/build/manual/version-code').write_text(re.search(r'versionCode (\d+)',g)[1])
Path('app/build/manual/version-name').write_text(re.search(r'versionName "([^"]+)"',g)[1])
PY
"$bt/aapt2" compile --dir app/src/main/res -o "$out/resources.zip"
assets=()
if [ -d app/src/main/assets ]; then assets=(-A app/src/main/assets); fi
"$bt/aapt2" link -o "$out/resources.apk" -I "$jar" --manifest "$out/AndroidManifest.xml" --java "$out/generated" --min-sdk-version 26 --target-sdk-version 35 --version-code "$(cat "$out/version-code")" --version-name "$(cat "$out/version-name")" "${assets[@]}" "$out/resources.zip"
"$JAVA_HOME/bin/javac" -source 17 -target 17 -cp "$jar" -d "$out/classes" "$out/generated/com/projectmister/game/R.java" app/src/main/java/com/projectmister/game/*.java
"$JAVA_HOME/bin/jar" cf "$out/classes.jar" -C "$out/classes" .
JAVA_HOME="$JAVA_HOME" "$bt/d8" --lib "$jar" --min-api 26 --output "$out/dex" "$out/classes.jar"
python3 - <<'PY'
import zipfile, shutil
from pathlib import Path
p=Path('app/build/manual')
shutil.copy(p/'resources.apk',p/'unsigned.apk')
with zipfile.ZipFile(p/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for d in (p/'dex').glob('*.dex'):z.write(d,d.name)
PY
"$bt/zipalign" -f -p 4 "$out/unsigned.apk" "$out/aligned.apk"
JAVA_HOME="$JAVA_HOME" "$bt/apksigner" sign --ks project-mister-dev.keystore --ks-key-alias projectmister --ks-pass pass:projectmisterdev --key-pass pass:projectmisterdev --out "$out/BOSS-XI-$(cat "$out/version-name")-candidate.apk" "$out/aligned.apk"
JAVA_HOME="$JAVA_HOME" "$bt/apksigner" verify --verbose "$out/BOSS-XI-$(cat "$out/version-name")-candidate.apk"
