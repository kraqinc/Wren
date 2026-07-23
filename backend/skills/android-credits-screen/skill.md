# Wren Skill: android-credits-screen

## Cuándo aplica
Preguntas sobre la pantalla de créditos/recarga en la app.

## Flujo real que muestra la UI
1. Usuario toca un paquete → se abre PayPal en el navegador Y se llama
   `POST /credits/recharge`.
2. La pantalla muestra el `activePendingRequest` con el código de referencia
   (`WREN-XXXXXX`) resaltado — el usuario debe copiarlo en la nota del pago.
3. La UI deja claro que esto es una **solicitud pendiente**, no una recarga
   instantánea — nunca muestres el balance actualizado hasta que el owner
   apruebe.

## No repitas el bug original
La primera versión de esta pantalla mostraba el balance actualizado en el
mismo momento en que se abría PayPal, como si el pago ya hubiera pasado. Ver
skill `billing` para el detalle completo del bug y por qué el flujo actual
(aprobación manual) es la única versión correcta.
