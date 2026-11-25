# Arquitectura y decisiones técnicas

## Base
Dantotsu (Kotlin, Gradle KTS, AGP 9.5.1, Gradle 9.9.0, JDK 21, módulo único `:app`, sabores `google` y `fdroid`).

## Decisiones que no deben cambiarse sin justificación
- **Sabor `fdroid`** como compilación de la app: no aplica Firebase/Crashlytics ni el proveedor GMS de Cronet (no envía datos al proyecto del autor original). Tarea: `assembleFdroidRelease` (en `ci/app.json`).
- **Firma**: `signingConfigs.ci` en `app/build.gradle.kts`, alimentada por las variables `CI_KEYSTORE_PATH`, `KEY_ALIAS`, `STORE_PASSWORD`, `KEY_PASSWORD` (desde GitHub Secrets). Nunca se genera un keystore en compilaciones normales.
- **Versión**: `versionName` = versión del manifiesto de la entrega; `versionCode` = número de entrega (siempre creciente).
- **Publicación atómica**: se integra y compila antes de hacer commit; solo si compila y la firma coincide con el keystore se hace push, se crea el tag `vX.Y.Z` y la Release.
- Los workflows (`.github/workflows/`) se editan a mano: el token de Actions no puede modificarlos. Las entregas ZIP los rechazan.
- Scripts de CI en `ci/` (no `scripts/`, que está en `.gitignore`).

## Formato de entrega
`MANIFEST.json` + `files/…` (+ `LEEME_ENTREGA.md`). Registro de entregas procesadas: `deliveries/ledger.json`.
