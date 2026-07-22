# Wren Skill: storage

## Cuándo aplica
Preguntas sobre subir/leer archivos, URLs de descarga, o el modelo
`StorageFile`.

## Flujo real
- `POST /api/storage` genera una `key` con forma `${userId}/${uuid}-${nombre}`
  y devuelve una URL "firmada" simulada (con `token` y `expiresAt` en el
  query string).
- `GET /api/storage?key=...` exige sesión (header `x-wren-user-id`,
  verificado por el `middleware.ts` de este servicio) Y valida que el `key`
  empiece con `${userId}/` del que hace la request — así nadie lee el
  archivo de otro aunque adivine un key ajeno.

## Deuda técnica conocida — no la des por resuelta
El `token`/`expiresAt` de la URL firmada **no se validan en el servidor**
todavía porque el modelo `StorageFile` no tiene esas columnas. Hoy la
"expiración de 15 minutos" es cosmética: el archivo sigue accesible
indefinidamente mientras el usuario dueño tenga sesión. Si el usuario pide
que la expiración sea real, la respuesta correcta es: agregar `token` y
`expiresAt` a `schema.prisma`, generar una migración, y chequear ambos
campos en el `GET` antes de devolver el archivo — no fingir que ya funciona.
