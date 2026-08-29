# MediaStream Player

Android TV media hub. The project is being implemented incrementally from the
approved v5.2 specification.

## Bootstrap baseline

- Android Gradle Plugin 9.3.2
- Gradle 9.7.1
- Kotlin 2.4.10 (AGP built-in Kotlin)
- KSP 2.3.4
- compileSdk / targetSdk 37
- minSdk 23
- Compose for TV
- Koin
- Room with KSP

Build from PowerShell:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
```

## TMDB catalog

Create a TMDB API Read Access Token in your TMDB account and add it to the local,
git-ignored `local.properties` file before building:

```properties
TMDB_API_TOKEN=your_api_read_access_token
```

The token is injected into the local APK through `BuildConfig` and is never committed.
