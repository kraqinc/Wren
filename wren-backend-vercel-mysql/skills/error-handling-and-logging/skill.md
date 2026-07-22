# Wren Skill: error-handling-and-logging

## Cuándo aplica
Preguntas sobre cómo debería responder un endpoint cuando algo falla, sobre
logs, debug en producción, o por qué algo es lento.

## Manejo de errores
`lib/errorHandler.ts` (compartido, byte-idéntico en los 15 microservicios)
captura el error, lo manda a `logger`, y devuelve un `500` genérico al
cliente — nunca expone el mensaje interno, stack trace, ni detalles de la
excepción original en la respuesta HTTP.

**Regla al escribir un handler nuevo:**
- Nunca devuelvas `err.message` o `err.stack` crudo en la respuesta — puede
  filtrar rutas de archivos, nombres de variables internas, o fragmentos de
  queries SQL.
- Sí es correcto devolver mensajes específicos y controlados por ti ("Plan
  inválido", "Créditos insuficientes") — la regla es sobre errores NO
  esperados, no sobre validaciones normales de negocio.
- Un error de red/conexión (ver skill `auth` sobre fallbacks falsos) nunca
  debería disfrazarse de una respuesta exitosa con contenido inventado.

## Logging
`lib/logger.ts` (compartido) escribe cada log (`info`, `warn`, `error`)
tanto a consola como a Postgres vía Prisma.

**Costo real a vigilar:** cada log de nivel `info` es un INSERT extra a la
base de datos en cada request — en Neon (tier gratuito), esto suma latencia
y consumo real si el tráfico crece. Si el volumen empieza a pesar, la
solución recomendada es dejar `info` solo en consola (Vercel ya la captura)
y persistir en Postgres únicamente `warn`/`error`.

**Al agregar un log nuevo:** no loguees tokens, contraseñas, ni el JWT
completo — ni siquiera en `error`. Si necesitas identificar al usuario, usa
su `userId`, no su email ni datos sensibles adicionales sin necesidad real.
