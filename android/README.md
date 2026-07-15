# TankerFlow Android (Kotlin + Jetpack Compose)

Beautiful, role-based tanker management app for the Spring Boot API.

## Open in Android Studio

1. Install [Android Studio](https://developer.android.com/studio) (Ladybug / latest).
2. **File → Open** → select the `android` folder.
3. Let Gradle sync (JDK 17).
4. Start the backend: `cd backend && mvn -s .mvn/settings.xml spring-boot:run`
5. Run the **app** on an emulator.

### API URL

Default in `app/build.gradle.kts`:

- Emulator → PC: `http://10.0.2.2:8080/`
- Physical phone → use your PC LAN IP, e.g. `http://192.168.1.20:8080/`

## What’s inside

| Role | Screens |
|------|---------|
| Owner / Manager | Splash, login/register, dashboard, trips, book trip, fleet (tankers/drivers/bore), money (diesel/salaries/bore power), trip timeline |
| Driver | Assigned trips, status updates, live GPS sharing |
| Customer | Track by token (no login) — map preview + ETA; hidden after complete |

## Design

- Brand: **TankerFlow** (lagoon teal + coral CTAs)
- Motion: splash scale, pulsing truck, animated auth switch, status timeline
- Friendly copy and large tap targets

## Deep link

`tankermanager://track/{token}` opens the tracking screen.
