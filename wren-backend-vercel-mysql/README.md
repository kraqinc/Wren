# Wren Backend v2 — Vercel + MySQL

Reemplaza tanto el backend de Railway (Express + SQLite) como los 15
microservicios en Postgres. Todo consolidado en un solo Next.js, con MySQL
como base de datos.

## Setup local

1. Instala dependencias:
   ```
   npm install
   ```
2. Copia `.env.example` a `.env` y pon tu connection string real de MySQL
   (la que armes con MySQL Shell/Workbench), tu `JWT_SECRET`, y tu
   `GEMINI_API_KEY`.
3. Crea las tablas en tu base MySQL:
   ```
   npx prisma migrate dev --name init
   ```
4. Corre local:
   ```
   npm run dev
   ```

## Deploy a Vercel

```
vercel --prod
```

En el dashboard de Vercel, configura las mismas 3 variables de entorno
(`DATABASE_URL`, `JWT_SECRET`, `GEMINI_API_KEY`) para producción. Tu MySQL
tiene que ser accesible desde internet (un MySQL solo en tu PC local no
sirve para el deploy de Vercel — considera PlanetScale, Railway MySQL, o
un VPS con el puerto abierto).

## Endpoints

- `POST /api/auth` — `{ email, password, mode: "login"|"register" }`
- `GET/POST /api/projects` — proyectos del usuario autenticado
- `GET/POST/PUT/DELETE /api/projects/files` — archivos de un proyecto
- `GET/POST /api/credits` — balance y solicitud de recarga (nunca acredita al instante)
- `POST /api/ai/chat` — chat con Gemini, con skills inyectados por relevancia
- `GET/POST /api/owner` — stats, usuarios, aprobar/rechazar recargas, ajustar créditos (requiere `role: "OWNER"`)

Todos (excepto `/api/auth`) requieren `Authorization: Bearer <token>`.

## Cambiar el rol de un usuario a OWNER

Como no hay UI para esto todavía, hazlo directo en la base de datos:
```sql
UPDATE User SET role = 'OWNER' WHERE email = 'tu@email.com';
```
