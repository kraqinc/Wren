# Wren Skill: billing

## Cuándo aplica
El usuario pregunta por créditos, puntos, PayPal, recargas, planes/tiers,
suscripciones, o pagos en general.

## Cómo funciona el sistema de créditos real en Wren

**Regla de oro, no negociable**: ningún endpoint puede acreditar puntos de forma
instantánea a partir de una acción del cliente. Siempre pasa por una solicitud
`PENDING` que un OWNER aprueba a mano después de confirmar el pago real.

**Flujo actual (Railway backend + Android app):**
1. Usuario toca "Pagar con PayPal" → la app abre el navegador a un link de
   paypal.me (o similar) Y llama `POST /api/credits/recharge` con `packageId`.
2. El servidor busca el paquete en un catálogo fijo del lado del servidor
   (`PACKAGE_CATALOG` en `credits.ts` — nunca confíes en un monto que mande el
   cliente) y crea una fila en `pending_recharges` con un `reference_code` único
   (`WREN-XXXXXX`). **No se acredita nada todavía.**
3. El usuario debe escribir ese código en la nota del pago de PayPal.
4. El OWNER ve las solicitudes pendientes en `GET /owner/recharges/pending`,
   confirma manualmente que el pago llegó, y llama
   `POST /owner/recharges/:id/approve` — solo ESE endpoint mueve el balance real,
   dentro de una transacción SQL, y deja un `audit_logs` con quién aprobó qué.
5. Si el pago nunca llegó, `POST /owner/recharges/:id/reject`.

## El bug que ya se cometió aquí — no lo repitas
La primera versión de `/credits/recharge` acreditaba los puntos EN EL MISMO
REQUEST que abría el link de PayPal, sin ninguna verificación de que el pago se
hubiera completado. Cualquiera podía tocar "Pagar", cancelar el navegador sin
pagar, y quedarse con los puntos gratis, las veces que quisiera. El comentario
en el código literalmente decía `// Recharge credits (Mock Payment)`.

**Nunca escribas un endpoint de pago que:**
- Acredite algo antes de que exista una confirmación server-side real del pago.
- Confíe en un monto o `packageId` sin validarlo contra un catálogo fijo del
  servidor.
- Trate "el usuario abrió el link de pago" como equivalente a "el usuario pagó".

## Planes / tiers
`FREE`, `PRO`, `TEAM` (o `PREMIUM` según el servicio). Al aprobar una recarga de
tipo `subscription_monthly`, además de acreditar puntos, actualiza
`users.tier = 'PREMIUM'`.
