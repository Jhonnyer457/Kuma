# Estado del proyecto

<!-- AUTO:START -->
- Última entrega: 005 — Identidad Kuma: carpetas, copias de seguridad, textos y sin donaciones
- Última versión: 0.3.0 (`v0.3.0`)
- Integrada: 2026-11-25 10:25 UTC
<!-- AUTO:END -->

## Terminado
- Infraestructura CI/CD: integración de ZIP, compilación, firma, verificación y Release (entrega 001, **probada por el usuario: compila e instala**).
- Rebranding (entrega 002, compila e instala): nombre Kuma, paquete `com.kuma.app`, iconos (launcher, monocromo, notificación, logo interno, splash), actualizador apuntando a `Jhonnyer457/Kuma`.

## Cambios respecto al código de Dantotsu
- `app/build.gradle.kts`: versión y firma desde CI; `applicationId = com.kuma.app` (el `namespace` sigue siendo `ani.dantotsu`, no se renombran paquetes de código).
- `app/src/fdroid/.../AppUpdater.kt`: actualizador real (antes era un no-op) contra las Releases de `R.string.repo`.
- Se habilitó el actualizador en el sabor fdroid (`AnilistViewModel`, `SettingsAboutActivity`, `SettingsActivity`).
- Recursos: iconos nuevos; `anim_splash.xml` es ahora un `animation-list` de un fotograma (el código lo castea a `Animatable`).
- Textos visibles: `app_name`, `repo`, `github`, "Read/Watch/Stream on Kuma", "Kuma Profile" y etiquetas del manifiesto.

- Cierre al arrancar **resuelto** (entrega 003 permitió diagnosticarlo): R8 eliminaba clases de ML Kit; se corrigió con reglas en `app/proguard-rules.pro` (arreglo del usuario).
- Limpieza de identidad (entrega 005, **sin compilar todavía**): carpetas y copias de seguridad con nombre Kuma, textos visibles, donaciones ocultas.

## En desarrollo
- Nada.

## Pendiente
Ver `TODO.md`.

## Problemas conocidos
- Las descargas, capturas y copias de seguridad ahora usan la carpeta `Kuma`; lo que ya estuviera en `Dantotsu/` hay que moverlo a mano.
- El actualizador solo funciona si las Releases de `Jhonnyer457/Kuma` son públicas (con el repo privado la API devuelve 404).
- Instalar Kuma no sustituye a Dantotsu: son apps distintas (otro paquete y otra firma). La instalación anterior `ani.dantotsu` (si existe) hay que desinstalarla a mano.
- Los textos largos (FAQ, descripciones) y la carpeta "Dantotsu" de descargas/capturas siguen con el nombre original hasta la etapa de traducción.
- El inicio de sesión de AniList/MAL usa el esquema `dantotsu://` y el cliente de Dantotsu a propósito (cambiarlo rompería el login).

## Próxima etapa
Traducción completa al español (entrega 006).
