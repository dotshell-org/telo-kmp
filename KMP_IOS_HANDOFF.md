# 🍎 Passation KMP → travail iOS (lecture pour Claude Code sur Mac)

> **But de ce fichier** : permettre à un agent Claude Code (et à un dev humain) tournant **sur macOS** de comprendre l'état de la migration Kotlin Multiplatform de cette app, puis de **reprendre le travail iOS qui était impossible sur Windows**. Tout le savoir nécessaire est dans ce fichier — la mémoire de l'agent qui a fait la migration n'est PAS dans le repo.

---

## 1. TL;DR

`com.pelotcl.app` (app de transport en commun TCL Lyon, Compose + MapLibre) a été migrée d'une **app Android mono-cible** vers un **projet Kotlin Multiplatform** sur la branche `feat/kmp`. **84% du code Kotlin (233 fichiers) est déjà en `commonMain`.** La cible Android compile, tourne et a été vérifiée à chaque étape.

**Ce qui n'a JAMAIS pu être fait sous Windows et t'attend sur Mac :**
1. **Compiler `commonMain` pour iOS pour de vrai** (`linkDebugFrameworkIosSimulatorArm64`). Jamais exécuté. C'est la priorité n°1 — voir §5.
2. **Publier les klibs iOS de `raptor-kt`** (lib d'itinéraires, repo séparé) depuis macOS — voir §6.
3. **Créer la coquille app iOS** (projet Xcode + `ComposeUIViewController`) — voir §7.
4. **Compléter/valider les 12 actuals iOS** (écrits "au mieux", jamais compilés Native) — voir §8.
5. **Le swap carte** (`PlanScreen` impératif MapLibre-Android → `MapCanvas` déclaratif maplibre-compose), nécessaire pour que la carte s'affiche sur iOS — voir §9.

---

## 2. État actuel (chiffres)

| | Avant (`main`) | Maintenant (`feat/kmp`) |
|---|---|---|
| Structure | 1 source set Android (`app/src/main`) | KMP : commonMain / androidMain / iosMain |
| Fichiers .kt | 242 (100% Android) | **233 common (84%) · 32 android · 12 ios** |
| Lignes Kotlin | — | 22 672 common · 7 186 android · 362 ios |
| Réseau | Retrofit + Gson | **Ktor + kotlinx.serialization** |
| Config | SnakeYAML (`config.yml`) | **kotlinx.serialization (`config.json`)** |
| IO/gzip | java.io + java.util.zip | **okio** |
| Dates | java.time / SimpleDateFormat | **kotlinx-datetime** |
| Ressources | R.drawable/R.string | **Compose Resources** + providers runtime |
| ViewModel | androidx.lifecycle | **JetBrains lifecycle (CMP)** |

57 commits sur la branche. ~960 icônes de ligne converties SVG → vector XML.

**Les 32 fichiers `androidMain` restants** sont soit irréductibles (actuals plateforme, `MainActivity`/`PeloApplication`, services WorkManager/foreground/ProcessLifecycle, Fused location, permission runtime), soit **gated sur le swap carte** (§9 : `PlanScreen`, `MapLibreView`, 4 `*MapManager`, `BitmapUtils`, `BusIconHelper`, `OfflineMapManager`, `JourneyNavigationManager`, `LatLngConversions`, `TransportViewModelIcons.android`).

---

## 3. Toolchain & versions (`gradle/libs.versions.toml`)

- Kotlin **2.3.10**, AGP **9.0.0**, Compose Multiplatform **1.8.1**
- JetBrains lifecycle-viewmodel **2.9.0** (commonMain — garde le package `androidx.lifecycle`)
- Ktor **3.1.3**, kotlinx-serialization **1.10.0**, kotlinx-datetime **0.6.2**, kotlinx-coroutines **1.10.2**, okio **3.9.1**
- maplibre-compose **0.13.0** (commonMain), maplibre-android **12.3.1** (androidMain)
- **raptor-kt `eu.dotshell:raptor-kt:1.6.0`** résolu depuis **`mavenLocal()`** (déjà en tête des repos dans `settings.gradle.kts`)

**Cibles KMP** (`app/build.gradle.kts`) : `androidTarget`, `iosArm64()`, `iosSimulatorArm64()` → framework `ComposeApp` (`isStatic = true`).
⚠️ **`iosX64` a été RETIRÉ** volontairement (maplibre-compose 0.13 ne le publie pas). Si besoin du simulateur Intel, il faudra une autre version de maplibre-compose ou retirer maplibre-compose de `iosX64`.

---

## 4. Contraintes & pièges CRITIQUES (à lire avant de toucher quoi que ce soit)

1. **`./gradlew :app:assembleDebug` (Android) compile `commonMain` contre la stdlib JVM.** Donc des membres **JVM-only** de types communs **passent inaperçus sur Android et ne cassent QU'À la compilation iOS/Native**. Pièges déjà rencontrés & corrigés ; à re-chasser :
   - `LinkedHashMap(capacity, loadFactor, accessOrder=true)` + `removeEldestEntry` → **JVM-only**. Utiliser un LRU manuel.
   - `String.format(...)`, `java.util.UUID`, `System.currentTimeMillis()`, `.javaClass`, `Class`/réflexion JVM, `ThreadLocal`, `ConcurrentHashMap`, `synchronized`, `TreeMap`, `printStackTrace()`, `java.text.*`, `java.time.*`.
   - Remplacements en place dans le projet : `randomId()` (commonMain), `Clock.System.now()`, `@Volatile` (`kotlin.concurrent.Volatile`), `::class`, kotlinx-datetime.
2. **JAMAIS d'annotations `kotlin.jvm.*` (`@JvmField`, `@JvmStatic`…) dans `commonMain`** — ça casse la compilation metadata (a déjà bloqué la publication de raptor-kt).
3. **Ressources** : les assets (`config.json`, `holidays.json`, raptor `.bin`, styles) sont sous `composeResources/files/` → Compose Resources les empaquète sous `assets/composeResources/<pkg>/files/`. La résolution est gérée par `FileSystem` (voir `FileSystem.android.kt` / `FileSystem.ios.kt`). **Ne pas hardcoder de chemins racine.**
4. **Icônes de ligne** : ~960 fichiers en `composeResources/drawable/*.xml` (vector XML). **NE JAMAIS remettre de `.svg`** — Compose Resources sur Android plante sur SVG (« Android platform doesn't support SVG format »).
5. **L'utilisateur Windows ne peut PAS builder iOS** (Kotlin/Native iOS nécessite macOS) — d'où cette passation. Les builds Android étaient vérifiés manuellement par l'utilisateur (la config Gradle "à froid" en CLI ne résout pas les markers de plugins ; seul le cache chaud d'Android Studio marche).

---

## 5. 🎯 PRIORITÉ 1 — Faire compiler `commonMain` pour iOS

Statut : `compileKotlinMetadata` passe sous Windows, mais **le vrai link Native n'a jamais tourné**. C'est la toute première chose à faire.

```bash
# Sanity Android d'abord (doit rester vert)
./gradlew :app:assembleDebug

# LA nouvelle frontière — compilation iOS de commonMain :
./gradlew :app:compileKotlinIosSimulatorArm64
# puis le link du framework :
./gradlew :app:linkDebugFrameworkIosSimulatorArm64
```

**Attends-toi à des erreurs** de membres JVM-only passés au travers des mailles (cf. §4.1). Pour chacune : remplacer par l'équivalent multiplatform. Un **audit statique** avait été fait (zéro `import java.`/`android.`/`kotlin.jvm.` dans commonMain), mais le compilateur Native est le seul juge fiable. Cherche en priorité dans les gros fichiers récemment migrés : `TransportViewModel.kt`, `OfflineDataManager.kt`, `RaptorRepository.kt`, `SchedulesRepository.kt`, `JourneyCache.kt`, les sheets.

---

## 6. 🎯 raptor-kt — klibs iOS

L'app dépend de `eu.dotshell:raptor-kt:1.6.0` via `mavenLocal()`. La lib a été **convertie en KMP** (cibles `androidTarget` + `iosArm64` + `iosSimulatorArm64`) mais **seuls les artefacts Android + metadata ont été publiés depuis Windows**. Les klibs iOS doivent être publiés depuis macOS.

```bash
# Repo séparé (demande l'URL/chemin au collègue — était "Z:\Projets\raptor-kt" côté Windows ;
# remote GitHub : https://github.com/dotshell-org/raptor-kt)
cd <raptor-kt>
./gradlew publishToMavenLocal   # publie metadata + android + KLIBS iOS depuis Mac
```

⚠️ **Risque ABI** : raptor-kt est en **Kotlin 2.1.0**, l'app en **2.3.10**. La compat metadata Android↔ est OK (vérifiée), mais pour les **klibs iOS** un bump de raptor-kt vers Kotlin 2.3.x peut être nécessaire si le link iOS de l'app se plaint d'incompatibilité de version klib. Bumper la version Kotlin dans le `build.gradle.kts` de raptor-kt et re-publier le cas échéant.

API raptor-kt (rappel) : `PeriodData(periodId, stopsBytes: ByteArray, routesBytes: ByteArray)` — tout est en `ByteArray` (plus d'`InputStream`). Côté app, `RaptorRepository` charge les assets via `FileSystem.readAssetBytes`.

---

## 7. 🎯 Coquille app iOS (Xcode)

Il n'existe **pas encore de projet Xcode**. Le module KMP produit le framework `ComposeApp`. À faire :
1. Créer un projet Xcode (`iosApp/`) qui linke le framework `ComposeApp`.
2. Point d'entrée Compose : exposer un `ComposeUIViewController { App() }` depuis Kotlin (`iosMain`) et l'afficher dans un `UIViewController`/SwiftUI `UIViewControllerRepresentable`.
3. Trouver/définir le composable racine commun. Côté Android, l'UI est montée par `MainActivity` (androidMain) — il faudra un `App()` commun équivalent (aujourd'hui la racine et le `NavHost` sont encore dans `MainActivity`, cf. Phase 4 "AppRoot" non terminée dans le plan).
4. `Info.plist` : permissions localisation (`NSLocationWhenInUseUsageDescription`), background modes si besoin (cf. §8).

> Pattern Android actuel à répliquer (`MainActivity`) : `PlanScreen` toujours monté (Box) pour préserver l'état carte, un `AppNavHost` superposé sur l'onglet Réglages, bottom nav simple. `CompositionLocalProvider(LocalPlatformContext provides ...)` est posé par `MainActivity` — l'équivalent iOS devra fournir un `PlatformContext` iOS.

---

## 8. Inventaire expect/actual (les 12 actuals iOS à valider)

Tous les `expect` ont **déjà un fichier `*.ios.kt`** (structure complète) mais **jamais compilés Native** → à valider/compléter. Localisation : `app/src/iosMain/kotlin/com/pelotcl/app/`.

| `expect` (commonMain) | actual iOS | Statut attendu / à faire |
|---|---|---|
| `class FileSystem(context)` | `platform/FileSystem.ios.kt` | Lecture assets `composeResources/files/` — **à vérifier sur device** |
| `class Settings(context, name)` | `platform/Settings.ios.kt` | NSUserDefaults — vérifié "complet" (15 membres) |
| `object Log` | `platform/Log.ios.kt` | `NSLog`/println — OK |
| `fun parseComposeColor(...)` | `platform/ColorParser.ios.kt` | OK probable |
| `class SecureStorage(context, name)` | `platform/SecureStorage.ios.kt` | **Stub NSUserDefaults — remplacer par Keychain pour de vrai** |
| `class BackgroundScheduler(context)` | `platform/BackgroundScheduler.ios.kt` | **Stub — implémenter BGTaskScheduler** (upload télémétrie + traffic alerts) |
| `class LocationProvider(context)` | `utils/location/LocationProvider.ios.kt` | **Stub — implémenter CLLocationManager** |
| `fun createOfflineTileDownloader(context)` | `platform/OfflineTileDownloader.ios.kt` | **Stub** (startDownload→Complete immédiat) — offline tuiles iOS non implémenté |
| `fun mapGestureOptions(interactive)` | `ui/components/MapGestures.ios.kt` | GestureOptions iOS (maplibre-compose) — utile au swap carte (§9) |
| `fun createHttpClientEngine()` | `platform/Platform.ios.kt` | Ktor **Darwin** engine (dep `ktor-client-darwin` déjà dans `iosMain`) — OK |
| `fun appVersionName(context)` | `platform/Platform.ios.kt` | CFBundleShortVersionString |
| `fun provideLineColors()` / `provideTransportLineRules()` / `provideMapStyleConfig()` | `platform/ConfigProvider.ios.kt` | Chargent depuis `config.json` via FileSystem — **à vérifier** |
| `fun showToast(context, message)` | `platform/Toast.ios.kt` | Pas de Toast natif iOS → stub/log ou alternative UI |

**Priorité réelle des stubs** : `LocationProvider` (CLLocationManager) et `SecureStorage` (Keychain) sont les plus impactants fonctionnellement. `BackgroundScheduler` et `OfflineTileDownloader` peuvent rester stubs au début (l'app marche sans, juste sans upload background ni tuiles offline).

---

## 9. Le swap carte (Phase 6) — nécessaire pour la carte sur iOS

Aujourd'hui la carte = `org.maplibre.android.*` (**Android-only**, dans `androidMain`). **Sur iOS il n'y a donc pas de carte tant que ce swap n'est pas fait.**

- Un composant déclaratif **`MapCanvas`** (commonMain, basé sur **maplibre-compose 0.13**) a **déjà été écrit** (`app/src/commonMain/.../ui/components/MapCanvas.kt` + `MapGeoJson.kt`) : style, caméra (`cameraState.animateTo`), couches lignes (couleur data-driven), arrêts, itinéraire, position user, clics → callbacks. **Mais il n'est PAS branché** (dead code).
- **Tâche** : remplacer l'appel `MapLibreView` (~ligne 2171 de `PlanScreen.kt`, ~3000 l.) par `MapCanvas`, supprimer les ~800 l. de `LaunchedEffect` impératifs, convertir `uiState.Success.lines`/`stopsUiState.Success.stops` en GeoJSON pour MapCanvas, câbler caméra mode-nav + véhicules. Puis SUPPRIMER les managers impératifs (`MapLinesManager`/`MapStopsManager`/`ItineraryMapManager`/`LineMapManager`/`MapLibreView`/`BitmapUtils`/`BusIconHelper`/`JourneyNavigationManager`/`LatLngConversions`).
- ⚠️ **Régression visuelle ACTÉE** : maplibre-compose 0.13 **n'expose pas d'enregistrement d'images NOMMÉES** référençables data-driven → impossible de reproduire les **icônes de ligne par arrêt** et les **glyphes véhicules** → ils deviennent des **cercles/points colorés**. (Si une version plus récente de maplibre-compose ajoute l'API images-nommées, on peut revenir aux icônes.)
- **Pourquoi c'était bloqué sous Windows** : on peut compiler mais pas **vérifier le rendu** (compile ≠ s'affiche). Sur Mac + simulateur/device, c'est enfin testable → fais-le en itératif avec **build + run**.

API maplibre-compose 0.13 (repères) : `MaplibreMap(baseStyle=BaseStyle.Uri(url), cameraState, zoomRange=0f..20f, pitchRange=0f..60f, styleState, onMapClick, options=MapOptions(renderOptions, gestureOptions, ornamentOptions))` ; `CameraPosition(target=Position(lat,lng), zoom, bearing, tilt[0..60], padding)` ; `rememberGeoJsonSource(GeoJsonData.JsonString(s), GeoJsonOptions(cluster=...))`. Offline tuiles = pas couvert par maplibre-compose (reste derrière `OfflineTileDownloader` expect/actual).

---

## 10. Ordre de marche recommandé (pour le Claude Code du collègue)

1. `./gradlew :app:assembleDebug` → confirmer que l'Android est vert sur cette machine.
2. **§6** : cloner raptor-kt, `publishToMavenLocal` depuis Mac (klibs iOS).
3. **§5** : `./gradlew :app:linkDebugFrameworkIosSimulatorArm64` → corriger les pièges JVM-only un par un jusqu'au vert.
4. **§7** : créer la coquille Xcode + point d'entrée Compose, lancer dans le simulateur.
5. **§8** : valider les actuals au runtime ; implémenter en priorité `LocationProvider` (CLLocationManager) et `SecureStorage` (Keychain).
6. **§9** : brancher `MapCanvas` dans `PlanScreen` (build + run itératif), supprimer les managers impératifs.
7. Re-vérifier que **Android reste vert** après chaque étape touchant `commonMain`.

---

## 11. Carte du repo (où sont les choses)

- `app/src/commonMain/kotlin/com/pelotcl/app/` — le gros du code (233 fichiers) : `generic/` (data, ui, service, utils), `specific/` (impls Lyon), `platform/` (les `expect`).
- `app/src/androidMain/kotlin/...` — actuals Android + carte impérative + `MainActivity`/`PeloApplication` + services/workers.
- `app/src/iosMain/kotlin/...` — les 12 actuals iOS (§8).
- `app/src/commonMain/composeResources/` — `files/` (config.json, holidays.json, raptor `.bin`, styles), `drawable/` (~960 vector XML), `values/strings.xml`.
- `.claude/plans/termine-la-migration-compl-te-unified-hamming.md` — **le plan détaillé de la migration** (phases 0→10, décisions, risques). À lire pour le contexte complet.

Bon courage — l'essentiel du chemin est fait, il reste la partie « Mac-only ». 🚀
