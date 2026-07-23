# Wren Skill: android-webview-login

## Cuándo aplica
Preguntas sobre el login vía WebView (`WebLoginScreen.kt`) o la página web
que carga (`login-wren.vercel.app`).

## Contrato entre la app y la página web
La página web, al loguear con éxito, debe llamar:
```js
window.WrenAuthBridge.onLoginSuccess(JSON.stringify({
  token: "<jwt>",
  user: { id, email, role, tier, balance }
}))
```
o, si algo falla del lado de la página: `window.WrenAuthBridge.onLoginError("mensaje")`.

Fallback: si la página redirige a una URL con `?token=...` en vez de llamar
al bridge, `WebLoginScreen` también lo detecta e intercepta.

## Bug ya corregido — no lo repitas
Este archivo se perdió dos veces entre sesiones de trabajo antes de quedar
estable, y en una de esas vueltas rompió el build de CI porque usaba
`TopAppBar` (API experimental de Material3) sin el `@OptIn` correspondiente.
Si tocas este archivo, revisa que cualquier composable con `TopAppBar`,
`ModalBottomSheet`, o similar tenga `@OptIn(ExperimentalMaterial3Api::class)`
arriba de la función — ver skill `android-ui`.

## OAuth (Google / GitHub)
El botón de Google redirige a un backend externo
(`wren-google.onrender.com/oauth/start`) que hace el intercambio con Google y
redirige de vuelta con `?token=...`. La app Android **no** habla
directamente con Google — todo el OAuth pasa por ese backend intermedio.
