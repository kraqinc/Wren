# Wren Skill: updates

## Cuándo aplica
Preguntas sobre el sistema de auto-actualización de la app, o publicar una
nueva versión.

## Flujo real
- `GET /api/updates?platform=android&version=X` es público — cualquiera
  puede consultar si hay una versión más nueva, sin sesión.
- `POST /api/updates` (publicar una versión nueva) exige el header
  `x-wren-admin-key` que debe coincidir con la variable de entorno
  `ADMIN_API_KEY`. Esto es intencionalmente distinto de un JWT normal de
  usuario — publicar una actualización que la app va a instalar
  automáticamente no debería depender de "estar logueado", sino de una clave
  separada que solo el equipo tiene.

## Bug ya corregido
Antes, `POST` no pedía nada — cualquiera podía publicar una "actualización
obligatoria" falsa apuntando a un APK malicioso, hacia todos los usuarios.
Es el vector de ataque más peligroso que se encontró en todo el proyecto
porque no afecta a una cuenta, afecta a todos los usuarios de la app a la
vez. Ver skill `security`, punto 2.

## Si migras esto a un rol OWNER en vez de una clave separada
Es válido, pero asegúrate de que el JWT tenga un campo `role` real (hoy el
payload solo tiene `sub`/`email`/`plan` en los microservicios) antes de
confiar en `role === "OWNER"` para proteger esto.
