# Arquitectura y decisiones técnicas

## Base
Dantotsu (Kotlin, Gradle KTS, AGP 9.5.1, Gradle 9.9.0, JDK 21, módulo único `:app`, sabores `google` y `fdroid`).

## Decisiones que no deben cambiarse sin justificación
- **Sabor `fdroid`** como compilación de la app: no aplica Firebase/Crashlytics ni el proveedor GMS de Cronet (no envía datos al proyecto del autor original). Tarea: `assembleFdroidRelease` (en `ci/app.json`).
- **Firma**: `signingConfigs.ci` en `app/build.gradle.kts`, alimentada por las variables `CI_KEYSTORE_PATH`, `KEY_ALIAS`, `STORE_PASSWORD`, `KEY_PASSWORD` (desde GitHub Secrets). Nunca se genera un keystore en compilaciones normales.
- **Versión**: `versionName` = versión del manifiesto de la entrega; `versionCode` = número de entrega (siempre creciente).
- **Publicación atómica**: se integra y compila antes de hacer commit; solo si compila y la firma coincide con el keystore se hace push, se crea el tag `vX.Y.Z` y la Release.
- **Identidad**: `applicationId = com.kuma.app`; `namespace = ani.dantotsu` se mantiene (renombrar los ~500 archivos de código no aporta nada y aumenta el riesgo de romper la compilación).
- **Cosas de Dantotsu que NO se renombran a propósito**: esquema `dantotsu://` (redirección del login de AniList/MAL/Discord), nombres internos de preferencias/acciones (`ani.dantotsu.*`) y carpetas de descargas hasta decidir lo contrario.
- **Actualizaciones**: `src/fdroid/.../AppUpdater.kt` lee `https://api.github.com/repos/<R.string.repo>/releases/latest`, compara la etiqueta (`vX.Y.Z`) con `BuildConfig.VERSION_NAME` y descarga la APK cuya ABI coincide con el dispositivo (nombres `vX.Y.Z-app-fdroid-<abi>-release.apk`). Requiere Releases públicas.
- **Iconos**: foreground/monocromo PNG en `drawable-nodpi`, fondo adaptativo `#0A0E0F`, notificación en `drawable-*dpi/notification_icon.png`. `anim_splash` debe seguir siendo `Animatable` (MainActivity y SettingsActivity hacen `as Animatable`).
- **Diagnóstico**: `util/EarlyCrashDump.kt` (instalado en `App.attachBaseContext` y llamado desde `FinalExceptionHandler`) guarda el stack trace en Descargas/Kuma-crash.txt vía MediaStore (API 29+) o en la carpeta externa de la app (API 26-28). No envía datos a ningún sitio.
- **R8 / ML Kit**: `app/proguard-rules.pro` conserva `com.google.mlkit.**`, `mlkit_common`, `mlkit_vision` y los registradores `com.google.firebase.components.*`; sin ellas la app se cierra al arrancar (`MlKitInitProvider`). No quitar.
- **Carpetas y archivos**: descargas/capturas/copias usan `Kuma` (`DownloadsManager.BASE_LOCATION`, `Pictures/Kuma`, `Kuma/Backups`, `kuma_autobackup_*.ani`, `Kuma_<fecha>`). La extensión `.ani` no cambia (compatibilidad de restauración).
- **Se conserva de Dantotsu a propósito** (funcionamiento y créditos): licencia y créditos (Developers/Helpers, texto de origen), esquema `dantotsu://` y cliente de AniList/MAL, claves de metadatos de extensiones (`dantotsu.novelextension.class`), addon de descargas nativas (`dantotsu.downloadAddon` desde `rebelonion/Dantotsu-Download-Addon`), servidor de comentarios `api.dantotsu.app`, lista de contribuidores, nombres internos de estilos/ids/paquetes.
- **Donaciones**: ocultas (vistas `gone`, sin diálogo aleatorio). Política de privacidad de Dantotsu oculta en Acerca de; iconos de Discord/Forgejo de Dantotsu ocultos.
- Los workflows (`.github/workflows/`) se editan a mano: el token de Actions no puede modificarlos. Las entregas ZIP los rechazan.
- Scripts de CI en `ci/` (no `scripts/`, que está en `.gitignore`).

## Formato de entrega
`MANIFEST.json` + `files/…` (+ `LEEME_ENTREGA.md`). Registro de entregas procesadas: `deliveries/ledger.json`.
