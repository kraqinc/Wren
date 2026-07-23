# Wren Skill: health-check

## Cuándo aplica
Preguntas sobre monitoreo de uptime, o integraciones con herramientas
externas de status (UptimeRobot, BetterStack, etc).

## Qué hace
`GET /api/health` es público, no toca base de datos por defecto, y devuelve
un `200 OK` simple con timestamp — pensado para que un servicio externo lo
pinguee cada X minutos sin gastar cuota de Postgres en cada chequeo.

## Si le agregas un chequeo de base de datos
Si decides que el health-check también verifique la conexión a Postgres
(`SELECT 1`), ten cuidado con la frecuencia — un monitor externo pingueando
cada 30 segundos puede sumar una carga real a la base de datos gratuita de
Neon. Considera cachear el resultado del chequeo de DB por unos segundos en
vez de consultarla en cada request.
