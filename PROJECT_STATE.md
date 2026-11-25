# Estado del proyecto

<!-- AUTO:START -->
- Última entrega: 003 — Diagnostico del cierre al arrancar (volcado de error a Descargas)
- Última versión: 0.2.1 (`v0.2.1`)
- Integrada: 2026-11-25 06:02 UTC
<!-- AUTO:END -->

## Terminado
- Infraestructura CI/CD: integración de ZIP, compilación, firma, verificación y Release (entrega 001, **probada por el usuario: compila e instala**).
- Rebranding (entrega 002, **compila e instala, pero la app se cierra al abrirla**): nombre Kuma, paquete `com.kuma.app`, iconos (launcher, monocromo, notificación, logo interno, splash), actualizador apuntando a `Jhonnyer457/Kuma`.

## Cambios respecto al código de Dantotsu
- `app/build.gradle.kts`: versión y firma desde CI; `applicationId = com.kuma.app` (el `namespace` sigue siendo `ani.dantotsu`, no se renombran paquetes de código).
- `app/src/fdroid/.../AppUpdater.kt`: actualizador real (antes era un no-op) contra las Releases de `R.string.repo`.
- Se habilitó el actualizador en el sabor fdroid (`AnilistViewModel`, `SettingsAboutActivity`, `SettingsActivity`).
- Recursos: iconos nuevos; `anim_splash.xml` es ahora un `animation-list` de un fotograma (el código lo castea a `Animatable`).
- Textos visibles: `app_name`, `repo`, `github`, "Read/Watch/Stream on Kuma", "Kuma Profile" y etiquetas del manifiesto.

## En desarrollo
- Diagnóstico del cierre al arrancar (entrega 003): `EarlyCrashDump` guarda el error en Descargas/Kuma-crash.txt.

## Pendiente
Ver `TODO.md`.

## Problemas conocidos
- **La app Kuma 0.2.0 se cierra al abrirla** (aparece "Kuma continúa fallando"). Causa aún sin identificar; la 003 añade el volcado del error para encontrarla.
- El actualizador solo funciona si las Releases de `Jhonnyer457/Kuma` son públicas (con el repo privado la API devuelve 404).
- Instalar Kuma no sustituye a Dantotsu: son apps distintas (otro paquete y otra firma). La instalación anterior `ani.dantotsu` (si existe) hay que desinstalarla a mano.
- Los textos largos (FAQ, descripciones) y la carpeta "Dantotsu" de descargas/capturas siguen con el nombre original hasta la etapa de traducción.
- El inicio de sesión de AniList/MAL usa el esquema `dantotsu://` y el cliente de Dantotsu a propósito (cambiarlo rompería el login).

## Próxima etapa
Entrega 004: corregir el cierre al arrancar según el error volcado; después, traducción al español.
