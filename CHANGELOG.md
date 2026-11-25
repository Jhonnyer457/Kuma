# Changelog

## [0.3.0] - 2026-11-25 (entrega 005) — Identidad Kuma: carpetas, copias de seguridad, textos y sin donaciones

### Cambiado
- Descargas, capturas, copias de seguridad automaticas y manuales usan la carpeta/nombre Kuma en vez de Dantotsu
- Textos visibles (menu Acerca de, preguntas frecuentes, ajustes, notificaciones, widget, Discord) dicen Kuma; se conservan los creditos y la licencia de Dantotsu
- Reglas de R8 para ML Kit (app/proguard-rules.pro) incluidas en el proyecto
- Eliminadas las donaciones (boton Sponsor, objetivo de donacion y aviso aleatorio)
- Politica de privacidad de Dantotsu e iconos de Discord/Forgejo de Dantotsu ocultos

### Corregido
- La app ya no se cierra al arrancar (R8 eliminaba piezas de ML Kit)

## [0.2.2] - 2026-11-25 (entrega 004) — Diagnostico del cierre al arrancar (volcado de error a Descargas)

### Añadido
- Si la app se cierra por un error, guarda el detalle en Descargas/Kuma-crash.txt para poder diagnosticarlo desde el telefono (no envia nada a Internet)

### Cambiado
- Documentacion del proyecto actualizada con el problema conocido y el nuevo plan de entregas

## [0.2.1] - 2026-11-25 (entrega 003) — Diagnostico del cierre al arrancar (volcado de error a Descargas)

### Añadido
- Si la app se cierra por un error, guarda el detalle en Descargas/Kuma-crash.txt para poder diagnosticarlo desde el telefono (no envia nada a Internet)

### Cambiado
- Documentacion del proyecto actualizada con el problema conocido y el nuevo plan de entregas

## [0.2.0] - 2026-11-25 (entrega 002) — Rebranding Kuma: nombre, paquete, iconos y actualizador

### Añadido
- Iconos de Kuma: launcher adaptativo, icono monocromo (iconos temáticos), icono de notificaciones, logo interno y splash
- Actualizador propio: busca nuevas versiones en las Releases de Jhonnyer457/Kuma y descarga la APK de la ABI del dispositivo
- Opción 'Buscar actualizaciones' y comprobación manual (mantener pulsado el logo en Ajustes) disponibles en esta compilación

### Cambiado
- Nombre de la app: Kuma
- Paquete (applicationId): com.kuma.app
- Enlaces del repositorio y de GitHub apuntan a Jhonnyer457/Kuma
- Textos visibles 'Leer/Ver/Stream en Kuma', 'Perfil de Kuma' y etiquetas del manifiesto

## [0.1.0] - 2026-11-25 (entrega 001) — Base Dantotsu + infraestructura CI/CD

### Añadido
- Código base de Dantotsu 3.2.2 importado como punto de partida
- Integración automática de entregas ZIP, compilación firmada y publicación de Releases
- Documentación persistente del proyecto

### Cambiado
- Versión y firma de la APK controladas por CI (app/build.gradle.kts)

