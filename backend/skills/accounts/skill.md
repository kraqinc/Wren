# Wren Skill: accounts

## Cuándo aplica
Preguntas sobre perfil de usuario, tier/plan actual, o datos de cuenta que
no sean estrictamente login (eso es `auth`) ni pagos (eso es `billing`).

## Qué hace
`accounts` expone lectura/edición del perfil (email, nombre, preferencias)
separado de `auth` (que solo emite y valida tokens) y de `billing` (que solo
maneja plan y créditos). Tres responsabilidades separadas a propósito — no
mezcles lógica de pago dentro de `accounts` ni de perfil dentro de `billing`.

## Protegido por middleware
Como los demás servicios privados, necesita su propio `middleware.ts` (ver
skill `auth`) — sin eso, cualquiera podría leer/editar el perfil de otro
usuario con solo cambiar el header `x-wren-user-id`.
