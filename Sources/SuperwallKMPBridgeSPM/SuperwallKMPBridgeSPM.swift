// Intentionally (almost) empty.
//
// SwiftPM requires regular targets to contain at least one source file. This
// wrapper target exists only so the SuperwallKMPBridge library product can
// carry the SuperwallKit dependency transitively alongside the binary target —
// binary targets cannot declare dependencies themselves. See the repo-root
// Package.swift header comment for the full rationale (RevenueCat pattern).
