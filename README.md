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
