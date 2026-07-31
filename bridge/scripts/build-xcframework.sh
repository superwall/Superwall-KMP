#!/usr/bin/env bash
# Builds SuperwallKMPBridge.xcframework from bridge/Package.swift
# (device + simulator archives, combined via -create-xcframework).
#
# SPM archives need two fixups before -create-xcframework:
#   1. The library product must be `type: .dynamic` (Package.swift) or the
#      archive contains no framework at all.
#   2. The archived framework carries no Headers/ or Modules/ — but Kotlin
#      cinterop consumes the framework via its generated ObjC header, so we
#      inject the generated ${SCHEME}-Swift.h plus a framework modulemap.
set -euo pipefail

BRIDGE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="${BRIDGE_DIR}/build"
SCHEME="SuperwallKMPBridge"
XCFRAMEWORK="${BUILD_DIR}/SuperwallKMPBridge.xcframework"

rm -rf "${XCFRAMEWORK}" "${BUILD_DIR}/archives" "${BUILD_DIR}/derived"
mkdir -p "${BUILD_DIR}/archives"

archive() {
  local destination="$1"
  local name="$2"
  local sdk_dir="$3" # Release-iphoneos | Release-iphonesimulator

  xcodebuild archive \
    -workspace "${BRIDGE_DIR}" \
    -scheme "${SCHEME}" \
    -destination "${destination}" \
    -archivePath "${BUILD_DIR}/archives/${name}" \
    -derivedDataPath "${BUILD_DIR}/derived/${name}" \
    SKIP_INSTALL=NO \
    BUILD_LIBRARY_FOR_DISTRIBUTION=NO

  local fw="${BUILD_DIR}/archives/${name}.xcarchive/Products/Library/Frameworks/${SCHEME}.framework"
  if [[ ! -d "${fw}" ]]; then
    echo "ERROR: no framework at ${fw}. Archive contents:" >&2
    find "${BUILD_DIR}/archives/${name}.xcarchive" -maxdepth 4 >&2
    exit 70
  fi

  # Inject the generated ObjC interface header + a modulemap so the framework
  # is consumable by Kotlin cinterop (and plain ObjC importers).
  local header
  header="$(find "${BUILD_DIR}/derived/${name}" -path "*${sdk_dir}*" -name "${SCHEME}-Swift.h" -print -quit)"
  if [[ -z "${header}" ]]; then
    echo "ERROR: generated ${SCHEME}-Swift.h not found under ${BUILD_DIR}/derived/${name} for ${sdk_dir}. Candidates:" >&2
    find "${BUILD_DIR}/derived/${name}" -name "*-Swift.h" >&2
    exit 70
  fi
  mkdir -p "${fw}/Headers" "${fw}/Modules"
  cp "${header}" "${fw}/Headers/${SCHEME}-Swift.h"
  cat > "${fw}/Modules/module.modulemap" <<EOF
framework module ${SCHEME} {
  umbrella header "${SCHEME}-Swift.h"
  export *
  module * { export * }
}
EOF

  # Some Xcode versions omit Info.plist for SPM-product frameworks;
  # -create-xcframework requires one.
  if [[ ! -f "${fw}/Info.plist" ]]; then
    cat > "${fw}/Info.plist" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>CFBundleDevelopmentRegion</key><string>en</string>
  <key>CFBundleExecutable</key><string>${SCHEME}</string>
  <key>CFBundleIdentifier</key><string>com.superwall.sdk.kmp.bridge</string>
  <key>CFBundleInfoDictionaryVersion</key><string>6.0</string>
  <key>CFBundleName</key><string>${SCHEME}</string>
  <key>CFBundlePackageType</key><string>FMWK</string>
  <key>CFBundleShortVersionString</key><string>1.0</string>
  <key>CFBundleVersion</key><string>1</string>
  <key>MinimumOSVersion</key><string>14.0</string>
</dict>
</plist>
EOF
  fi
}

archive "generic/platform=iOS" "ios" "Release-iphoneos"
archive "generic/platform=iOS Simulator" "ios-simulator" "Release-iphonesimulator"

xcodebuild -create-xcframework \
  -archive "${BUILD_DIR}/archives/ios.xcarchive" -framework "${SCHEME}.framework" \
  -archive "${BUILD_DIR}/archives/ios-simulator.xcarchive" -framework "${SCHEME}.framework" \
  -output "${XCFRAMEWORK}"

echo "Built ${XCFRAMEWORK}"
find "${XCFRAMEWORK}" -maxdepth 2
