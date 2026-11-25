# Estado del proyecto

<!-- AUTO:START -->
- Última entrega: 006 — Kuma en espanol: traduccion de la interfaz
- Última versión: 0.4.0 (`v0.4.0`)
- Integrada: 2026-11-25 23:20 UTC
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
- Limpieza de identidad (entrega 005, **probada: funciona**): carpetas y copias de seguridad con nombre Kuma, textos visibles, donaciones ocultas.
- Traducción al español (entrega 006, **sin compilar todavía**): `values-es/strings.xml` con 1.191 textos y 9 listas desplegables; el teléfono en español muestra la app en español.

## En desarrollo
- Nada.

## Pendiente
Ver `TODO.md`.

## Problemas conocidos
- Quedan en inglés unos 300 mensajes escritos directamente en el código (avisos, errores, algunos diálogos). Se traducen en la entrega 007.
- Los textos largos de las preguntas frecuentes mencionan funciones de Dantotsu tal cual (traducidos, sin enlaces a su Discord).
- Las descargas, capturas y copias de seguridad ahora usan la carpeta `Kuma`; lo que ya estuviera en `Dantotsu/` hay que moverlo a mano.
- El actualizador solo funciona si las Releases de `Jhonnyer457/Kuma` son públicas (con el repo privado la API devuelve 404).
- Instalar Kuma no sustituye a Dantotsu: son apps distintas (otro paquete y otra firma). La instalación anterior `ani.dantotsu` (si existe) hay que desinstalarla a mano.
- Los textos largos (FAQ, descripciones) y la carpeta "Dantotsu" de descargas/capturas siguen con el nombre original hasta la etapa de traducción.
- El inicio de sesión de AniList/MAL usa el esquema `dantotsu://` y el cliente de Dantotsu a propósito (cambiarlo rompería el login).

## Próxima etapa
Entrega 007: mensajes del código que siguen en inglés (avisos y errores).
