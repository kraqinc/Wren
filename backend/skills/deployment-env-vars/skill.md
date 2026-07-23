# Wren Skill: deployment-env-vars

## Cuándo aplica
Preguntas sobre variables de entorno, o por qué algo funciona en local pero
no en producción.

## Railway (`wren-backend-railway`)
```
DATA_DIR=/data          # requiere un Volume montado, si no SQLite se borra en cada deploy
JWT_SECRET=<secreto>
OWNER_PASSWORD=<opcional -- si falta, se genera uno random y se imprime en logs>
```

## Cada microservicio Vercel (`wren-microservices/*`)
```
DATABASE_URL=<Postgres>
JWT_SECRET=<el MISMO valor en los 15, si no un token firmado por uno no lo valida otro>
```

## Además, solo `ai-gateway` / `streaming` / el monolito
```
GEMINI_API_KEY=<Google AI Studio>
```

## Además, solo `updates` (y el monolito)
```
ADMIN_API_KEY=<random, ej. openssl rand -hex 32>
```

## Error típico si algo falla en producción y no en local
Casi siempre es una de estas dos: `JWT_SECRET` distinto entre servicios (un
token válido en uno da 401 en otro), o una variable que existe en `.env`
local pero nunca se configuró en el dashboard de Vercel/Railway — revisa
ambas antes de asumir que es un bug de código.
