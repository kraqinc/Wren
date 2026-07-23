# Wren Skill: cdn

## Cuándo aplica
Preguntas sobre servir assets estáticos, imágenes, o por qué esta ruta no
pide sesión.

## Por qué es público a propósito
`cdn` está en la lista `PUBLIC_PATHS` de `middleware.ts` en cualquier
servicio que lo incluya. Sirve contenido estático (imágenes de perfil,
iconos de extensiones del marketplace, assets de la app) que no tiene
sentido proteger con JWT — es contenido de lectura pública por diseño, igual
que un CDN real.

## Al agregar contenido nuevo aquí
No subas nada aquí que dependa del usuario que lo pide (datos privados,
archivos de `storage` con dueño) — para eso existe el servicio `storage`,
que sí valida sesión y dueño. `cdn` es solo para lo que cualquiera debería
poder ver sin autenticarse.
