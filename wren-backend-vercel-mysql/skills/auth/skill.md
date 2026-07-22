# Wren Skill: auth

## Cuándo aplica
El usuario pregunta por login, registro, JWT, sesiones, o por qué una ruta
deja/no deja entrar a alguien.

## Cómo funciona la autenticación real en Wren

**Railway backend (wren-backend, Express + SQLite) — en uso HOY por la app:**
- `POST /api/auth/register` y `/api/auth/login` devuelven `{ message, token, user }`.
- El token es un JWT firmado con `JWT_SECRET` (variable de entorno, sin fallback
  hardcodeado — si no está seteada, el server debe rechazar arrancar, no usar
  un secreto por defecto).
- `middleware/auth.ts` → `authenticateToken` valida el JWT en cada ruta protegida
  y expone `req.user = { id, email, role, tier }`.
- `requireRole('OWNER')` es un middleware aparte para las rutas de `owner.ts`.

**Microservicios nuevos (Next.js + Prisma, Vercel):**
- `auth/route.ts` firma el JWT con Web Crypto (`lib/jwt.ts`, `signJwt`/`verifyJwt`,
  HS256, funciona tanto en runtime Node como en Edge).
- El JWT viaja como `Authorization: Bearer <token>`.
- **CRÍTICO**: cada microservicio que toca datos de usuario (billing, storage,
  sync, queue, telemetry, monitoring, accounts, ai-gateway) DEBE tener su propio
  `middleware.ts` (copiado de `api-gateway`) que llama `verifyJwt` y, si es válido,
  reescribe el header `x-wren-user-id` con el `sub` del token verificado.
- Las rutas (`route.ts`) SIEMPRE deben leer `x-wren-user-id` asumiendo que el
  middleware ya lo verificó — nunca deben confiar en ese header si no hay
  `middleware.ts` presente en ese mismo servicio. Si vas a crear un microservicio
  nuevo que toca datos privados, el checklist es: JWT_SECRET compartido + copiar
  middleware.ts + matcher `/api/:path*`.
- Rutas públicas por diseño (no requieren JWT): `/api/auth`, `/api/cdn`,
  `/api/health`, y el `GET` de `/api/updates` y `/api/marketplace` (pero NO sus
  `POST`/`PUT`/`DELETE`).

## Errores ya cometidos en este proyecto — no los repitas
- Un fallback catch-all que generaba un "token falso" cuando el login fallaba
  (`firebase_fallback_token_...`) y dejaba pasar al usuario igual. Nunca hagas
  fallbacks de auth que oculten un error real.
- Confiar en `x-wren-user-id` sin que ningún middleware lo haya verificado antes
  — permitía suplantar a cualquier usuario cambiando un header.
- Password de owner hardcodeada como fallback en el código (`WrenOwner2026!`).
  Si `OWNER_PASSWORD` no está seteada, genera una aleatoria y solo imprímela en
  logs, nunca la escribas en el código fuente.

## Al escribir código de auth nuevo
1. Nunca inventes un token o sesión "de emergencia" si algo falla — muestra el
   error real.
2. Valida email + password mínimo 8 caracteres en el cliente Y en el servidor.
3. Cualquier ruta nueva que toque datos de un usuario específico necesita
   `middleware.ts` o un chequeo explícito de JWT dentro del propio handler.
4. Nunca hardcodees secretos, contraseñas por defecto, o API keys en el código.
