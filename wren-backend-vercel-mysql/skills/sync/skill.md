# Wren Skill: sync

## Cuándo aplica
Preguntas sobre sincronizar proyectos/configuración entre dispositivos.

## Qué hace
Guarda el estado (archivos abiertos, cursor, preferencias) asociado a
`userId`, para que abrir la app en otro dispositivo continúe donde quedó.
Todo scoped por `userId` del header verificado — nunca por un `deviceId`
suelto que el cliente podría falsificar.

## Cuidado con esto al implementarlo
Si dos dispositivos escriben al mismo tiempo, hoy gana el último `write` sin
ningún merge — no hay resolución de conflictos todavía. Si vas a soportar
edición simultánea real, esto necesita replantearse (versionado, timestamps
por campo, o un CRDT) antes de prometerle al usuario que "sincroniza en
tiempo real".
