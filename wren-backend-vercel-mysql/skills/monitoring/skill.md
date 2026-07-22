# Wren Skill: monitoring

## Cuándo aplica
Preguntas sobre métricas de sistema, alertas, o el panel de salud interno
(distinto de `health-check`, que es un ping simple público).

## Qué hace
Guarda métricas internas (latencia, tasa de error, uso por servicio) que
requieren sesión para leerse — a diferencia de `health-check`, esto no es
para un monitor externo, es para que el propio equipo revise el estado del
sistema. Por eso sí necesita `middleware.ts` protegiéndolo.

## Al agregar una métrica nueva
No guardes aquí nada que identifique a un usuario final (email, IP completa)
sin pensarlo dos veces — esto es telemetría de sistema, no de usuario. Para
eventos de usuario específicos existe `telemetry`.
