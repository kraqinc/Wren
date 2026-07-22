# Wren Skill: android-terminal

## Cuándo aplica
Preguntas sobre la terminal integrada de la app (`mkdir`, `touch`, `find`,
`echo`, etc.)

## Cómo funciona — y por qué NO es shell real
Los comandos de terminal operan sobre el **árbol de archivos virtual del
proyecto** (guardado en la base de datos, tabla `files`), no sobre el
sistema de archivos real del servidor. `mkdir` crea una fila con
`is_directory=true`, `touch` crea un archivo vacío, `echo "texto" > archivo`
escribe contenido a una fila existente, `find` busca por nombre/patrón
dentro del árbol del usuario autenticado.

## Por qué esta decisión, explícitamente
Dar ejecución real de shell (`child_process.exec` con input del usuario) en
un backend compartido y multi-usuario es una vulnerabilidad de ejecución
remota de código — cualquier usuario autenticado tendría acceso al servidor
completo, a los secretos de otros servicios, y podría pivotear a otros
usuarios. La terminal de Wren es intencionalmente una terminal *simulada y
segura* sobre datos propios del usuario, no un shell real sobre el host. Si
en algún momento se pide "que la terminal ejecute código real", la respuesta
correcta es un sandbox aislado por usuario (contenedor efímero), nunca exec
directo en el proceso del backend.

## Al agregar un comando nuevo
Cada comando nuevo debe: (1) operar solo sobre filas con `userId`/`projectId`
del usuario autenticado, (2) nunca aceptar un path que escape del proyecto
actual (sin `../` hacia otro proyecto), (3) devolver un error real y
específico si el comando no aplica, no una simulación silenciosa.
