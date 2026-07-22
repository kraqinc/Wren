# Wren Skill: rate-limiting

## Cuándo aplica
Preguntas sobre por qué un endpoint devuelve 429, o cómo limitar abuso.

## Cómo funciona hoy
`lib/rateLimiter.ts` (compartido, byte-idéntico en los 15 microservicios) es
un limitador **en memoria**, por instancia. Funciona bien para bloquear
ráfagas obvias, pero tiene una limitación real documentada en el propio
código: en Vercel, cada instancia serverless tiene su propia memoria, así
que un atacante distribuido entre varias instancias no queda limitado de
forma global.

## No lo trates como protección completa
Sirve para frenar abuso accidental (un bug en el cliente reintentando en
loop) pero no reemplaza rate-limiting real a nivel de infraestructura (Vercel
Edge Config, Upstash Redis, o similar) si necesitas garantías duras contra un
atacante decidido. Si vas a proteger algo de alto valor (recargas, login),
no asumas que este limitador basta por sí solo.
