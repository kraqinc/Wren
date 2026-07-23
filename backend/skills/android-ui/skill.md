# Wren Skill: android-ui

## Cuándo aplica
El usuario pide crear o modificar una pantalla de la app Android, o pregunta
por estilo/diseño de la interfaz.

## Identidad visual de Wren — no negociable
- **Nunca** uses la palabra "Cursor" en ningún texto, branding, nombre de
  paquete, o comentario visible al usuario. La marca es "Wren", siempre.
- Estética: Glassmorfismo + Bento Grid + UI espacial. Fondo casi negro
  (`PrimaryObsidian`), paneles translúcidos con borde que capta luz
  (patrón `GlassPanel`: `Brush.verticalGradient` sutil + `border` con alpha
  bajo), nunca tarjetas planas de un solo color sólido.
- Profundidad: 1-2 glows radiales fijos de fondo (no animados en loop) — dan
  atmósfera sin verse como juguete.
- Layout de contenido: mosaico tipo bento (tiles de distinto tamaño), no listas
  planas verticales cuando hay más de 3 elementos relacionados.

## Tipografía
- **UI general / chat del agente**: Sans Serif (el default de Compose /
  `FontFamily.Default`, no una fuente decorativa).
- **Código, terminal, diffs**: `FontFamily.Monospace` — idealmente JetBrains
  Mono real si el `.ttf` está en `res/font/`.
- Nunca mezcles: el código siempre en mono, la UI de chat/botones siempre en
  sans serif.

## Paleta (colores ya definidos en `core/theme/Theme.kt`)
`PrimaryObsidian` (fondo), `SecondaryCard` (paneles), `ElectricCyan` (acento
primario/usuario), `EditorYellow` (acento secundario/IA/dinero), `TerminalGreen`
(éxito), `ErrorRed` (error), `TextLight`/`TextMuted`/`BorderGray`.

## Errores de UI ya cometidos aquí — no los repitas
- Fake fallbacks que muestran una respuesta "bonita" cuando algo falló en vez
  del error real (pasó en login Y en el chat de IA — ambos ocultaban fallos de
  red con contenido inventado, incluso cobrando créditos por una respuesta
  falsa).
- Loaders/empty states que son solo texto estático sin ninguna animación de
  entrada — usa al menos un fade/scale de entrada sutil, no algo muerto.
- APIs experimentales de Material3 (`TopAppBar`, `ModalBottomSheet`, etc.)
  usadas sin `@OptIn(ExperimentalMaterial3Api::class)` en la función — esto
  rompe el build en CI con "This material API is experimental". Revisa esto
  primero si un build falla sin razón aparente.

## Antes de dar por terminada una pantalla nueva
- ¿Tiene `@OptIn(ExperimentalMaterial3Api::class)` si usa TopAppBar/BottomSheet?
- ¿Usa `GlassPanel` para los paneles, no `Card` plano?
- ¿El código usa mono y la UI usa sans serif?
- ¿Los errores reales se muestran tal cual, sin inventar una respuesta falsa?
