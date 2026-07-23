# Wren Skill: ai-gateway

## Cuándo aplica
Preguntas sobre el chat de IA en los microservicios (distinto del chat de
Railway, ver skill `database` para la diferencia de backends).

## Qué hace
Recibe `{ messages, model }`, valida que `messages` no esté vacío, y hace
streaming de la respuesta de Gemini de vuelta al cliente. Antes de reenviar
a Gemini, `getRelevantSkillsContent` (ver skill `skills-system`) revisa el
último mensaje del usuario e inyecta contenido de skills relevante dentro
del mensaje `system`, o crea uno nuevo si no existía.

## Duplicado a propósito
`streaming` es el mismo código que `ai-gateway`. No es un error de copiar y
pegar sin querer — permite desplegar dos dominios Vercel distintos con el
mismo comportamiento (por ejemplo, uno para la app y otro para integraciones
externas) sin duplicar lógica de negocio en otro lado. Si cambias uno,
cambia el otro.

## Variable de entorno requerida
`GEMINI_API_KEY` — sin esto el servicio arranca pero cada request falla.
