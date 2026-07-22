# Wren Skill: jwt-internals

## Cuándo aplica
Preguntas específicas sobre cómo se firman/verifican los tokens a nivel de
implementación (distinto de skill `auth`, que cubre el flujo general).

## Implementación real
`lib/jwt.ts` usa **Web Crypto** (`crypto.subtle`), no la librería
`jsonwebtoken` de npm — a propósito, porque así el mismo código funciona
tanto en runtime Node como en runtime Edge de Next.js sin depender de
Node-only APIs. Firma con HMAC-SHA256.

## Detalle que hay que conocer antes de tocar esto
`verifyJwt` no valida el campo `alg` del header del token — no es
explotable en esta implementación porque `getKey` siempre usa HMAC-SHA256
sin importar lo que declare el token (no hay branching por algoritmo), pero
si en algún momento se agrega soporte para más de un algoritmo, hay que
validar explícitamente que el `alg` del header sea uno de los permitidos
ANTES de usarlo — si no, se abre la puerta a un ataque clásico de
confusión de algoritmo.

## Railway backend usa algo distinto
El backend de Railway (Express) usa la librería `jsonwebtoken` normal, no
esta implementación con Web Crypto — son dos sistemas de JWT separados que
no comparten `JWT_SECRET` por defecto. Un token emitido por uno no es válido
en el otro a menos que configures el mismo secreto en ambos.
