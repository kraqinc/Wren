# Wren Skill: skills-system

## Cuándo aplica
Preguntas sobre cómo funciona el propio sistema de skills, o cómo agregar
uno nuevo.

## Cómo funciona
`skills_index.json` guarda metadata barata (nombre, descripción, palabras
clave) de cada skill. `skillsLoader.ts` compara el prompt del usuario contra
esas palabras clave (coincidencia simple, case-insensitive) y solo carga el
`skill.md` completo de los que aplican — nunca todos a la vez.

## Por qué no son 500 genéricos
Hubo un intento de usar un paquete de 500 "skills" que resultaron ser la
misma plantilla repetida sin contenido real. Se descartó porque: (1) inflaba
cada request de IA con cientos de miles de tokens sin aportar nada, y
(2) un skill sin contenido específico no cambia en nada la respuesta del
modelo frente a no tener skill en absoluto.

## Al agregar un skill nuevo
1. Solo si hay algo REAL y específico del proyecto que decir — un bug ya
   cometido, una convención concreta, un endpoint con comportamiento no
   obvio. Si el contenido cabría en "sé cuidadoso con X" sin ningún detalle
   del proyecto, no es un skill, es ruido.
2. Agrega la entrada en `skills_index.json` con 5-10 palabras clave reales
   (sinónimos y variantes en español, ya que los usuarios escriben en
   español).
3. Escribe `skills/<nombre>/skill.md` con la misma estructura que los
   existentes: cuándo aplica, cómo funciona de verdad, qué bug ya se cometió
   (si aplica), qué NO hacer.
