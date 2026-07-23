# Wren Skill: telemetry

## Cuándo aplica
Preguntas sobre analítica de uso, eventos de producto, o qué acciones se
registran del usuario.

## Qué hace
Guarda eventos de producto (`ai_chat_sent`, `file_created`,
`credits_purchased`, etc.) con `userId` y timestamp — pensado para responder
"cuánta gente usa X función", no para debug de sistema (eso es
`monitoring`).

## Antes de registrar un evento nuevo
Pregúntate si el dato es necesario para una decisión de producto real. No
todo necesita ser un evento — un log de cada tecla presionada no aporta
nada y sí es un problema de privacidad. Si el evento incluye contenido
generado por el usuario (el prompt que le mandó a la IA, por ejemplo), piensa
si hace falta guardarlo completo o solo metadata (longitud, modo usado).
