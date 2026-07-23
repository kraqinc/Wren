# Wren Skill: api-gateway

## Cuándo aplica
Preguntas sobre cómo se enrutan las llamadas entre servicios, o por qué una
ruta responde 401 en un servicio y en otro no.

## Qué hace realmente
`api-gateway` es el único servicio pensado como punto de entrada único
(opcional): recibe la request, su `middleware.ts` verifica el JWT, y la
reenvía al microservicio correspondiente. El `matcher` de su middleware es
`/api/:path*` — cubre todo bajo `/api`.

## Diferencia clave con los otros 14 servicios
Si despliegas los 15 como proyectos Vercel independientes (dominios propios,
sin pasar por api-gateway), cada uno necesita **su propia copia** de
`middleware.ts` — api-gateway no protege nada fuera de sí mismo, solo actúa
de gateway si de verdad enrutas el tráfico a través de él primero. No asumas
que por existir `api-gateway`, los demás quedan protegidos automáticamente.

## Al tocar este servicio
Si agregas un microservicio nuevo (el #16), decide primero: ¿va detrás de
`api-gateway` o se despliega suelto? Si va suelto, copia `middleware.ts` ahí
también (ver skill `auth`).
