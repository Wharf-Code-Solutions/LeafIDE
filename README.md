# LeafIDE

IDE de escritorio para el compilador del lenguaje **Leaf**, hecho con JavaCC. Es un proyecto escolar de la materia Autómatas 2.

Muestra a la vez el código, una consola de comandos y las fases de compilación: **léxico, sintáctico, semántico y código intermedio (cuádruplos)**. Las fases de optimización y código objeto aparecen como «próximamente».

## Qué incluye
- **Editor** con resaltado de Leaf, subrayado de errores y análisis en vivo.
- **Una vista por fase**: tabla de tokens, árbol sintáctico, tabla de símbolos por ámbito y cuádruplos (con la forma de tres direcciones y los temporales). Al hacer clic en una fila o nodo se marca su tramo en el código.
- **Carpeta de trabajo y explorador:** *Archivo → Abrir carpeta…* elige la carpeta (por ejemplo, la del compilador); el IDE la recuerda y las dos consolas arrancan ahí. Un panel *Explorador* muestra sus carpetas y los archivos `.txt` y `.leaf`.
- **Consolas**: *Leaf* (comandos como `compilar`, `tokens`, `ast`, `simbolos`, `cuadruplos`, `errores`; escribe `ayuda`) y *Sistema* (terminal `cmd`). Si en la terminal *Sistema* escribes `java generado.Leaf archivo.txt`, el IDE abre ese archivo y muestra sus fases (se puede apagar en *Compilar → Sincronizar la terminal Sistema con el IDE*).
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
| `scripts/` | Scripts `.bat` para compilar, abrir y actualizar el compilador |

## Usar el compilador sin el IDE
Tras compilar, el compilador queda en `ide-app\target\lib`. Por ejemplo:
```bat
java -cp "ide-app\target\lib\*" generado.Leaf ejemplos\validos\Fibonacci.txt
```

## Versión del compilador
El IDE usa el compilador del repositorio [Leaf](https://github.com/Wharf-Code-Solutions/Leaf); el commit exacto que lleva dentro se ve en la barra de estado (por ejemplo, `Leaf 9ac4869`). `scripts\actualizar-compilador.bat <carpeta-del-clon>` trae una versión nueva desde un clon de ese repositorio.

## Dependencias
FlatLaf, RSyntaxTextArea, Gson y ModernDocking (todas de Maven Central).
