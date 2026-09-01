# Demo data for screenshots

`UserServiceClient.java` — `fetchUnsafe` flagged (no deadline);
`fetchSafe` not flagged.

## How to get the screenshot

1. `./gradlew runIde` from `grpc-missing-deadline-companion`, open this
   `demo/` folder as the project.
2. Full Screen, open `UserServiceClient.java` — a warning should
   appear on `fetchUnsafe`'s `.getUser(request)` call but not on
   `fetchSafe`'s.
3. Screenshot with both methods visible, save into
   `grpc-missing-deadline-companion/docs/screenshots/`. Close the
   sandbox.
