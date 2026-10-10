# LeafIDE

IDE de escritorio para el compilador del lenguaje **Leaf**, hecho con JavaCC. Es un proyecto escolar de la materia Autómatas 2.

Muestra a la vez el código, una consola de comandos y las fases de compilación: **léxico, sintáctico y semántico** (las fases de código intermedio, optimización y código objeto aparecen como «próximamente»).

## Qué incluye
- **Editor** con resaltado de Leaf, subrayado de errores y análisis en vivo.
- **Una vista por fase**: tabla de tokens, árbol sintáctico y tabla de símbolos por ámbito. Al hacer clic en una fila o nodo se marca su tramo en el código.
- **Consolas**: *Leaf* (comandos como `compilar`, `tokens`, `ast`, `simbolos`, `errores`; escribe `ayuda`) y *Sistema* (terminal `cmd`).
- **Paneles acoplables** y **perspectivas** (Escritura, Análisis, Síntesis y Evaluación de 3 pantallas); la disposición y los ajustes se guardan en `%USERPROFILE%\.leafide`.
- **Modo demostración** (opcional): reproduce la compilación paso a paso, fase por fase.

## Requisitos
- Windows 11
- JDK 26 (`java -version` debe mostrar la versión 26)
- Conexión a internet la primera vez (Maven descarga las dependencias)

## Compilar y abrir
```bat
scripts\ejecutar-ide.bat
```
Compila sin pruebas y abre la ventana. Con `scripts\ejecutar-ide.bat rapido` abre lo ya compilado.

Para compilar y correr todas las pruebas:
```bat
mvnw.cmd verify
```

## Estructura
| Carpeta | Contenido |
|---|---|
| `compiler/` | El compilador de Leaf (gramática JavaCC y código generado) |
| `api/` | Contratos entre el compilador y el IDE |
| `adapter-v1/` | Adaptador que conecta el compilador con la API |
| `ide-core/` | Lógica del IDE sin interfaz (pipeline, consola de comandos, demostración, ajustes) |
| `ide-ui/` | Interfaz Swing (FlatLaf, RSyntaxTextArea, ModernDocking) |
| `ide-app/` | Ensamblado y arranque |
| `ejemplos/` | Programas Leaf válidos y con errores, usados también por las pruebas |
| `scripts/` | Scripts `.bat` para compilar y abrir |

## Usar el compilador sin el IDE
Tras compilar, el compilador queda en `ide-app\target\lib`. Por ejemplo:
```bat
java -cp "ide-app\target\lib\*" generado.Leaf ejemplos\validos\Fibonacci.txt
```

## Dependencias
FlatLaf, RSyntaxTextArea, Gson y ModernDocking (todas de Maven Central).
