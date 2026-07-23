# Wren Skill: incomplete-known-gaps

## Cuándo aplica
Antes de asumir que un servicio o pantalla ya funciona completamente —
revisa esto para no alucinar funcionalidad que no existe todavía.

## Lo que NO está terminado, hoy
- **`database` (el servicio, no el concepto)**: el zip original nunca trajo
  ningún `route.ts`. Solo existe el `prisma/schema.prisma` y las libs
  compartidas. No asumas que expone ningún endpoint hasta que alguien lo
  implemente.
- **`storage`**: el token/expiración de la URL firmada es cosmético, no se
  valida en el servidor (ver skill `storage`).
- **`sync`**: no hay resolución de conflictos si dos dispositivos escriben
  al mismo tiempo — gana el último write sin merge.
- **Dos backends en paralelo**: Railway (en uso real por la app hoy) y los
  15 microservicios Vercel (arquitectura nueva, no completamente migrada).
  No asumas que un cambio en uno se refleja en el otro — hoy no comparten
  base de datos ni `JWT_SECRET` por defecto.
- **`ai-gateway` / `streaming`**: mismo código, duplicado a propósito para
  dos deploys distintos — no asumas que son servicios con lógica
  independiente.

## Regla general
Si no estás seguro si algo está implementado, dilo explícitamente en vez de
asumir que sí — es mejor decir "esto no lo veo implementado, ¿lo armamos?"
que generar código que asume una función que no existe.
