# Wren Skill: queue

## Cuándo aplica
Preguntas sobre procesos en segundo plano (compilaciones largas, envío de
notificaciones, generación de reportes).

## Qué hace
`POST /api/queue` crea un `QueueJob` (`userId` opcional — puede ser un job
del sistema sin dueño). `GET /api/queue` lista los jobs del usuario
autenticado, siempre filtrado por `userId` del header verificado.

## Convención al agregar un tipo de job nuevo
Cada job necesita un `type` string reconocible (`"compile"`, `"notify"`,
etc.) y un `payload` JSON serializable. No pongas lógica de negocio pesada
directamente en el handler de creación del job — el handler solo debe
encolar; quien procese la cola (worker separado, o un cron) hace el trabajo
real. Mezclar ambas cosas hace que crear un job bloquee la respuesta HTTP.
