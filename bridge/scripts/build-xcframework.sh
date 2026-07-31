#!/usr/bin/env bash
# Builds SuperwallKMPBridge.xcframework from bridge/Package.swift
# (device + simulator archives, combined via -create-xcframework).
set -euo pipefail

BRIDGE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="${BRIDGE_DIR}/build"
SCHEME="SuperwallKMPBridge"
XCFRAMEWORK="${BUILD_DIR}/SuperwallKMPBridge.xcframework"

rm -rf "${XCFRAMEWORK}" "${BUILD_DIR}/archives"
mkdir -p "${BUILD_DIR}/archives"

archive() {
  local destination="$1"
  local name="$2"
  xcodebuild archive \
    -workspace "${BRIDGE_DIR}" \
    -scheme "${SCHEME}" \
    -destination "${destination}" \
    -archivePath "${BUILD_DIR}/archives/${name}" \
    SKIP_INSTALL=NO \
    BUILD_LIBRARY_FOR_DISTRIBUTION=NO
}

archive "generic/platform=iOS" "ios"
archive "generic/platform=iOS Simulator" "ios-simulator"

xcodebuild -create-xcframework \
  -archive "${BUILD_DIR}/archives/ios.xcarchive" -framework "${SCHEME}.framework" \
  -archive "${BUILD_DIR}/archives/ios-simulator.xcarchive" -framework "${SCHEME}.framework" \
  -output "${XCFRAMEWORK}"

echo "Built ${XCFRAMEWORK}"
