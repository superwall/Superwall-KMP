// Required by @superwall/paywalls-js (see the root README, "Web (Kotlin/JS)"):
// - its audience-rule evaluator (@superwall/superscript) is a WebAssembly
//   module, which webpack 5 only loads with asyncWebAssembly enabled;
// - superscript's ESM build uses extensionless relative imports, which strict
//   ESM resolution ("fullySpecified") rejects.
config.experiments = Object.assign({}, config.experiments, { asyncWebAssembly: true });
config.module.rules.push({ test: /[\\/]@superwall[\\/]superscript[\\/].*\.m?js$/, resolve: { fullySpecified: false } });
