# Wren Skill: security

## Cuándo aplica
El usuario pide revisar, auditar o escribir código que toca autenticación,
pagos, datos de otros usuarios, permisos de admin, o publica contenido público
(actualizaciones de la app, extensiones del marketplace).

## Vulnerabilidades reales ya encontradas y arregladas en Wren — patrón a vigilar

1. **Suplantación de identidad vía header spoofeable.** 9 microservicios
   confiaban en `x-wren-user-id` sin que ningún middleware verificara el JWT
   primero. Cualquiera podía mandar ese header con cualquier valor y actuar
   como otro usuario. Regla: todo servicio que lea `x-wren-user-id` DEBE tener
   `middleware.ts` verificando el JWT antes de esa ruta, o el propio handler
   debe verificar el `Authorization: Bearer` directamente.

2. **Publicación no autenticada de "actualizaciones obligatorias".**
   `POST /api/updates` no pedía nada — cualquiera podía publicar una
   actualización falsa apuntando a un APK malicioso, hacia todos los usuarios.
   Regla: cualquier endpoint que publique contenido que la app instale o
   ejecute automáticamente necesita protección de admin (clave separada,
   `x-wren-admin-key`, o rol OWNER), nunca solo "estar logueado".

3. **Mutaciones públicas en el marketplace.** Crear/editar/borrar extensiones
   no pedía sesión. Regla: `GET` puede ser público para navegar, pero
   `POST`/`PUT`/`DELETE` siempre necesitan JWT válido.

4. **"Signed URL" decorativa.** `storage` generaba un token y expiración
   simulados para las URLs de archivos, pero el `GET` nunca los validaba — el
   archivo quedaba accesible para siempre con solo conocer el `key`. Si vas a
   simular una URL firmada con expiración, o la validas de verdad (columna
   `expiresAt` + chequeo server-side), o no la llames "firmada".

5. **Pago instantáneo sin verificación** (ver skill `billing`).

6. **Credenciales hardcodeadas.** Contraseña del owner por defecto en el código
   fuente Y en las notas de un GitHub Release público. Nunca hardcodees
   secretos ni los imprimas en salidas públicas (releases, logs de CI visibles).

## Checklist antes de dar por buena una ruta nueva
- ¿Alguien sin sesión puede leer/escribir algo que no debería?
- ¿El monto, precio, o cantidad de créditos lo decide el servidor o el cliente?
- ¿Esta ruta, si se llama repetidamente o con datos falsos, puede dar algo
  gratis, duplicado, o dañino a otros usuarios?
- ¿Hay alguna acción "de administrador" (publicar, aprobar, banear) protegida
  con algo más que "estar logueado"?
