# Wren Skill: android-ai-agent-screen

## Cuándo aplica
Preguntas sobre la pantalla del agente de IA en la app (`AIAgentScreen.kt`).

## Patrón real de propuesta/aceptación de cambios
El agente nunca aplica un cambio directamente. Responde con texto + un
bloque ```json opcional con acciones (`CREATE_FILE`, `EDIT_FILE`,
`EXECUTE_COMMAND`). La UI muestra cada acción como una tarjeta con diff,
colapsable, con botones "Aceptar"/"Rechazar" por separado — nada se aplica
hasta que el usuario toca "Aceptar" en esa acción específica.

## Bug ya corregido — dos veces, en dos capas distintas
1. **Cliente**: si la llamada de red fallaba, la pantalla mostraba una
   respuesta inventada tipo "No pude contactar al modelo" PERO seguía
   contando como una respuesta válida en el chat. Arreglado: ahora un fallo
   de red muestra un error real, sin fingir una respuesta del agente.
2. **Servidor** (Railway `ai.ts`): el crédito se descontaba ANTES de llamar
   al modelo — si el modelo fallaba, el usuario perdía el crédito por un
   error. Arreglado: el crédito solo se descuenta después de recibir una
   respuesta real.

Si tocas este flujo, no repitas ninguna de las dos versiones del mismo
error: nunca cobres ni finjas una respuesta antes de tener una real.
