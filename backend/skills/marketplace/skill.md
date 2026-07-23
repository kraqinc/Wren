# Wren Skill: marketplace

## Cuándo aplica
Preguntas sobre extensiones, plugins, o el catálogo público de Wren.

## Flujo real
- `GET /api/marketplace` y `GET /api/marketplace/[id]` son públicos a
  propósito — cualquiera puede navegar el catálogo sin sesión.
- `POST` (publicar), `PUT` (editar), `DELETE` (borrar) exigen JWT válido
  (`Authorization: Bearer`) verificado directamente en el handler — este
  servicio NO depende de un `middleware.ts` de ruta completa porque necesita
  dejar pasar el `GET` sin sesión; en vez de eso, cada handler mutante llama
  `verifyJwt` internamente antes de tocar la base de datos.
- El `GET /api/marketplace/[id]` incrementa el contador de descargas en cada
  vista — no tiene límite de repetición todavía. Si vas a mostrar
  "descargas" como métrica de confianza pública, considera limitarlo (un
  contador por IP/usuario por período) antes de usarlo para rankear.

## Bug ya corregido
Antes, publicar/editar/borrar una extensión no pedía ninguna sesión —
cualquiera en internet podía vandalizar el catálogo completo. Ver skill
`security`, punto 3.
