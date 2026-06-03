# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.3.0] - 2026-06-03

### Added
- GitHub Community health templates (`CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `.github/`).
- GitHub Actions CI/CD workflows for automated build checks.

### Changed
- Refactored project structure to decouple UI screens (`GameSetupScreen`, `TutorialScreen`, `WritingDeskScreen`, `StoryArchiveScreen`, `StoryReaderOverlay`) from `MainActivity`.
- Centralized skeuomorphic components under `ui.components` package.
- Extracted `CompletedStory` and `StoryRepository` into `data` package.

## [1.0.0] - 2026-06-01

### Added
- Initial release of Ink & Echo.
- Interactive turn-based typing screen and story compiler.
- Skeuomorphic typewriter sounds and paper grid styling.
- Local storytelling archives for reading previous cooperative logs.
- Automatic release signing config linked to `keystore.properties`.
- Configuration alignment with Google Play package namespace `com.LakesCorp.FunCoStory`.
