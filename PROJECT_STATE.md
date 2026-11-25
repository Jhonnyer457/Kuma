# Estado del proyecto

<!-- AUTO:START -->
- Última entrega: 001 — Base Dantotsu + infraestructura CI/CD
- Última versión: 0.1.0 (`v0.1.0`)
- Integrada: 2026-11-25 01:02 UTC
<!-- AUTO:END -->

## Terminado
- Infraestructura CI/CD: integración de ZIP, compilación, firma, verificación y Release (entrega 001; **sin probar todavía en GitHub**).
- Código base de Dantotsu importado sin cambios funcionales.

## Cambios al código de Dantotsu
- `app/build.gradle.kts`: versión (`-PappVersionName/-PappVersionCode`) y firma `release` desde variables de entorno.

## En desarrollo
- Nada.

## Pendiente
Ver `TODO.md`.

## Problemas conocidos
- La primera compilación en GitHub no se ha ejecutado aún: puede fallar por memoria (runner privado de ~7 GB) o por R8/minificado.
- `applicationId` sigue siendo `ani.dantotsu`: no instalar sobre Dantotsu oficial (la firma es distinta).

## Próxima etapa
Entrega 002: renombrado (nombre, applicationId, iconos), tras definir el nombre y el prompt definitivo.
