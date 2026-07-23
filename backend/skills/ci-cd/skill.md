# Wren Skill: ci-cd

## Cuándo aplica
Preguntas sobre GitHub Actions, por qué falla el build, o cómo publicar un
release.

## Errores reales ya cometidos en `android-ci.yml` — revisa esto primero
1. **Credenciales en las notas del release**: el workflow imprimía el email
   y password del owner en texto plano en la descripción del GitHub Release
   público. Nunca pongas credenciales, ni siquiera de prueba, en salidas que
   terminan siendo públicas (release notes, logs de CI visibles, comentarios
   de PR).
2. **Desfase Kotlin/Compose Compiler**: `kotlinCompilerExtensionVersion`
   debe coincidir exactamente con la versión de Kotlin en uso según el mapa
   oficial de Google — un desfase de un solo patch (ej. compiler 1.5.8 con
   Kotlin 1.9.22 en vez de 1.9.21) puede compilar sin error pero crashear en
   runtime con `NoSuchMethodError` en llamadas genéricas internas de
   Compose. Si el build pasa pero la app crashea con ese error, revisa esto
   antes que cualquier otra cosa.
3. **API experimental sin `@OptIn`**: `TopAppBar`, `ModalBottomSheet` y
   otras APIs de Material3 marcadas `@ExperimentalMaterial3Api` rompen
   `compileDebugKotlin` con "This material API is experimental" si falta el
   `@OptIn` en la función. Es un error de compilación real, no un warning
   ignorable en este proyecto.
4. **Archivos borrados sin commitear**: si un archivo referenciado
   (`import`) se borra localmente pero no se commitea el `git rm`, el
   working tree local puede verse bien mientras CI (que usa el último commit
   real) falla con "unresolved reference". Antes de reportar "CI falla sin
   razón", corre `git status` y busca líneas ` D` (borrado sin stage).
