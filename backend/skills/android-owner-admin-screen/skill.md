# Wren Skill: android-owner-admin-screen

## Cuándo aplica
Preguntas sobre la consola de administrador en la app (`OwnerAdminScreen.kt`).

## Qué incluye
Estadísticas del servidor (bento grid), lista de usuarios con ajuste manual
de créditos, logs de auditoría, y el panel de **recargas pendientes** —
cada solicitud muestra email del usuario, código de referencia, monto, y
botones "Confirmar pago" / "Rechazar" que llaman
`POST /owner/recharges/:id/approve` y `/reject` respectivamente.

## Regla de UI para esta pantalla
Cualquier ajuste manual de créditos (no solo la aprobación de recargas)
debe pedir un motivo obligatorio (`overrideReasonInput`) — el backend lo
guarda en `audit_logs` junto con quién hizo el cambio. Nunca agregues un
camino de ajuste de créditos que no deje rastro de auditoría; ese fue
exactamente el problema original con las recargas automáticas.

## Acceso
Esta pantalla solo debería ser alcanzable si `sessionManager.userRole ==
"OWNER"` — verifica que la navegación hacia `owner_dashboard` en
`MainActivity.kt` no sea alcanzable por un usuario normal, y que el backend
también valide el rol server-side (nunca confíes solo en que la UI oculte el
botón).
