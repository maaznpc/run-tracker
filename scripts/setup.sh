#!/usr/bin/env bash
# Optional Linux command-line SDK setup. Android Studio users only need fetch_model.py.
set -euo pipefail
stride_root="$(cd "$(dirname "$0")/.." && pwd)"
stride_sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$stride_root/.toolchain/android-sdk}}"
stride_accept=false
if [[ "${1:-}" == "--accept-sdk-licenses" ]]; then stride_accept=true; fi
if [[ "$(uname -s)" != Linux ]]; then
    echo 'Use Android Studio to install SDK 35 on this platform. See README.md.' >&2
    exit 1
fi
command -v java >/dev/null
command -v curl >/dev/null
command -v unzip >/dev/null
if [[ ! -x "$stride_sdk/cmdline-tools/latest/bin/sdkmanager" ]]; then
    mkdir -p "$stride_sdk/cmdline-tools"
    stride_temp="$(mktemp -d)"
    trap 'rm -rf "$stride_temp"' EXIT
    curl --fail --location --retry 2 --connect-timeout 15 --max-time 300 \
      https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip \
      -o "$stride_temp/tools.zip"
    unzip -q "$stride_temp/tools.zip" -d "$stride_temp"
    mv "$stride_temp/cmdline-tools" "$stride_sdk/cmdline-tools/latest"
fi
if [[ "$stride_accept" == true ]]; then
    # The caller explicitly selected license acceptance. Avoid pipefail on yes's SIGPIPE.
    set +o pipefail
    yes | "$stride_sdk/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$stride_sdk" --licenses >/dev/null
    set -o pipefail
else
    "$stride_sdk/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$stride_sdk" --licenses
fi
"$stride_sdk/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$stride_sdk" 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools'
python3 - "$stride_root" "$stride_sdk" <<'PY'
from pathlib import Path
import sys
root, sdk = sys.argv[1:]
Path(root, 'local.properties').write_text('sdk.dir=' + sdk.replace('\\', '\\\\').replace(':', '\\:') + '\n')
PY
python3 "$stride_root/scripts/fetch_model.py"
echo 'Ready. Run ./gradlew testDebugUnitTest lintDebug assembleDebug'
