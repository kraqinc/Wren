# Wren Skill: database

## Cuándo aplica
El usuario pide crear una tabla, migrar datos, o pregunta por qué algo no
persiste entre despliegues.

## Dos bases de datos distintas en este proyecto — no las confundas
1. **Railway backend (`wren-backend-railway`)**: SQLite local, vía
   `db/database.ts`. Usa `DATA_DIR` (variable de entorno) para la ruta del
   archivo — **nunca** un path relativo fijo sin volumen montado.
2. **Microservicios nuevos (`wren-microservices/*`)**: Postgres vía Prisma,
   cada servicio con su propio `prisma/schema.prisma` pero apuntando a la
   misma base con `DATABASE_URL`.

## El bug que ya se cometió aquí — no lo repitas
`database.ts` (Railway) guardaba el SQLite en un path relativo dentro del
propio contenedor. Railway borra el disco local en cada redeploy/reinicio si
no hay un Volume montado — las cuentas "desaparecían" después de cada deploy.
Regla: cualquier base de datos basada en archivo (SQLite) necesita su ruta en
una variable de entorno (`DATA_DIR`) que apunte a un volumen persistente, nunca
a un path relativo dentro del código del contenedor.

## Al agregar una tabla/modelo nuevo
- Ponle `created_at`/`createdAt` siempre.
- Si es dinero o créditos: nunca actualices el balance directo — pasa por una
  fila de auditoría (`credit_logs`/`audit_logs`) en la misma transacción.
- Si vas a exponer un campo por API, revisa primero si debería estar scoped a
  `userId` (ver skill `security`, punto 1 — no expongas datos de otros
  usuarios solo porque tienen la misma forma de tabla).
