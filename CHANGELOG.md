# Changelog

## [0.1.0.0] - 2026-06-04

### Added
- Initial project scaffold with Gradle 8.7 + Kotlin 2.0 build system
- Full MVVM architecture with Hilt dependency injection
- Multi-source data layer supporting VOD JSON, IPTV M3U8, and overseas node aggregation
- ExoPlayer (Media3) playback engine with hardware acceleration and 4K H.265/AV1 support
- Android TV UI with dark theme and D-pad focus system
- GitHub Actions CI/CD for automated APK builds

### Changed
- Migrated all dependencies from version catalog to hardcoded coordinates for CI reliability
- PlaybackManager updated to Media3 1.2.0 API surface (PlaybackException in common package,
  DataSource.Factory for MediaSource construction, onPlayerErrorChanged callback)
- NetworkModule converter import path corrected for serialization library

### Fixed
- Gradle wrapper DEFAULT_JVM_OPTS quoting that broke CI
- Resource merger error from .json file in res/drawable/
- buildConfig = true missing for BuildConfig reference
- Various nonexistent Maven dependency coordinates

### Removed
- Unused Room, leanback-preference, and coil-okhttp dependencies
- Release build job and Gradle caching from CI (temporary — will restore in follow-up)
