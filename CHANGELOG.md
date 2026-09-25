<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# gRPC Missing Deadline Companion Changelog

## [Unreleased]

## [0.1.1]

### Fixed

- Review/star CTA now links to this plugin's own Marketplace
  reviews page instead of the vendor's generic plugin list.

## [0.1.0]

### Added

- Warning on a gRPC blocking-stub RPC call with no
  `.withDeadline(...)`/`.withDeadlineAfter(...)` anywhere in its
  stub-construction chain -- gRPC's own docs warn this can hang the
  calling thread forever.
- Only flags the terminal RPC call in a chain -- a dead-end
  stub-building chain with no real RPC invocation is never flagged.

[Unreleased]: https://github.com/GapHunterLabs/grpc-missing-deadline-companion/compare/0.1.1...HEAD
[0.1.1]: https://github.com/GapHunterLabs/grpc-missing-deadline-companion/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/GapHunterLabs/grpc-missing-deadline-companion/commits/0.1.0
