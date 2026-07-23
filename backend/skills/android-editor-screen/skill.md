# Wren Skill: android-editor-screen

## Cuándo aplica
Preguntas sobre la pantalla principal del editor (`IDEWorkspaceScreen.kt`).

## Estructura
Pantalla completa con el editor de código como protagonista, y un explorador
de archivos + terminal accesibles como bottom sheets modales (no paneles
fijos permanentes) — pensado para pantallas pequeñas donde mostrar todo a la
vez no cabe. El editor soporta múltiples archivos abiertos.

## Convención de navegación
Desde aquí se navega a `ai_agent`, `credits`, y `owner_dashboard` (este
último solo visible si el usuario es OWNER). El logout limpia la sesión
(`sessionManager.clearSession()`) Y limpia el token del `NetworkClient`
(`NetworkClient.setAuthToken(null)`) — ambos pasos son necesarios; olvidar
el segundo deja el token viejo pegado en memoria aunque la sesión local ya
se haya borrado.
