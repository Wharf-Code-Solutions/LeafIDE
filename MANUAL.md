# Manual de usuario de LeafIDE

LeafIDE es un programa de escritorio para escribir programas en el lenguaje **Leaf** y **ver cómo los compila el compilador, fase por fase**: léxico, sintáctico, semántico y código intermedio (cuádruplos). Está pensado para estudiar y exponer el funcionamiento del compilador, no para ejecutar programas (todavía no hay fase de ejecución).

Este manual explica el **IDE**. La sintaxis del lenguaje Leaf no se explica aquí; en la carpeta `ejemplos/` hay programas de muestra.

## Contenido
1. [Instalación y arranque](#1-instalación-y-arranque)
2. [Un recorrido por la pantalla](#2-un-recorrido-por-la-pantalla)
3. [Escribir, abrir y guardar programas](#3-escribir-abrir-y-guardar-programas)
4. [Compilar y ver las fases](#4-compilar-y-ver-las-fases)
5. [Errores: el panel Problemas](#5-errores-el-panel-problemas)
6. [Carpeta de trabajo y Explorador](#6-carpeta-de-trabajo-y-explorador)
7. [Las consolas](#7-las-consolas)
8. [Terminal sincronizada con el IDE](#8-terminal-sincronizada-con-el-ide)
9. [Paneles y perspectivas](#9-paneles-y-perspectivas)
10. [Modo demostración](#10-modo-demostración)
11. [Tema, ajustes y archivos que guarda el IDE](#11-tema-ajustes-y-archivos-que-guarda-el-ide)
12. [Atajos de teclado](#12-atajos-de-teclado)
13. [Problemas frecuentes](#13-problemas-frecuentes)
14. [Lo que el IDE todavía no hace](#14-lo-que-el-ide-todavía-no-hace)

---

## 1. Instalación y arranque

### Requisitos
- **Windows** (los scripts son `.bat`).
- **JDK 26** (el kit completo, no solo el JRE). Comprueba con `java -version` que la versión sea 26 y que `java` esté en el PATH.
- **Internet** la primera vez: Maven descarga lo que falta. Después ya no hace falta.

No necesitas instalar Git, Maven ni JavaCC.

### Descargar
- Con Git: `git clone https://github.com/Wharf-Code-Solutions/LeafIDE.git C:\LeafIDE`
- O con el botón **Code → Download ZIP** de GitHub y descomprimiendo.

> **Recomendación:** pon la carpeta en una ruta corta y fuera de OneDrive (por ejemplo `C:\LeafIDE`). OneDrive puede dejar archivos a medias y la compilación falla con errores de «cannot find symbol».

### Abrir el IDE
Abre la carpeta `scripts` y ejecuta **`ejecutar-ide.bat`** (doble clic, o desde una terminal: `scripts\ejecutar-ide.bat`).

- La primera vez compila todo y puede tardar unos minutos.
- Con **`scripts\ejecutar-ide.bat rapido`** abre el IDE sin volver a compilar (usa lo ya compilado).
- Si algo falla, mira la sección [Problemas frecuentes](#13-problemas-frecuentes).

### Primera vez
Al abrir el IDE sin haberlo usado antes, aparece un selector para **elegir la carpeta de trabajo** (ver [sección 6](#6-carpeta-de-trabajo-y-explorador)). Puedes elegir la carpeta `ejemplos` del proyecto o la de tus propios programas. Si cancelas, el IDE abre igual.

---

## 2. Un recorrido por la pantalla

La ventana se organiza en **paneles** que puedes mover:

| Zona | Qué hay |
|---|---|
| Arriba | Menús **Archivo · Compilar · Ver**, y la barra con el botón **▶ Compilar**, los interruptores *Análisis en vivo* y *Resaltado*, y el botón *Demostración*. |
| Izquierda | **Explorador** de archivos de la carpeta de trabajo. |
| Centro | **Editor** donde escribes tu programa. |
| Derecha | Las **fases** de la compilación, en pestañas. |
| Abajo | **Problemas**, la consola **Leaf** y la terminal **Sistema**, en pestañas. |
| Barra inferior | Posición del cursor, archivo, estado, y a la derecha `UTF-8 · Leaf <versión> · API 1`. |

El número que acompaña a «Leaf» en la barra inferior (por ejemplo `9ac4869`) es el **commit del compilador** que el IDE lleva dentro. Sirve para saber exactamente qué versión del compilador estás usando.

---

## 3. Escribir, abrir y guardar programas

- **Editor:** tiene números de línea y **resaltado de colores** del lenguaje. El interruptor **Resaltado** de la barra lo apaga si prefieres texto plano.
- **Análisis en vivo:** mientras escribes, el IDE compila solo medio segundo después de que dejes de teclear y subraya los errores. El interruptor **Análisis en vivo** lo apaga.
- **Un documento a la vez.** Al abrir otro archivo, si hay cambios sin guardar, el IDE pregunta qué hacer.

Menú **Archivo**:

| Opción | Atajo | Qué hace |
|---|---|---|
| Nuevo | Ctrl+N | Documento en blanco. |
| Abrir… | Ctrl+O | Abre un archivo (`.leaf` o `.txt`). |
| Abrir carpeta… | Ctrl+Mayús+O | Elige la carpeta de trabajo. |
| Guardar | Ctrl+S | Guarda el archivo actual. |
| Guardar como… | Ctrl+Mayús+S | Guarda con otro nombre. Si no escribes extensión, añade `.leaf`; `.txt` también es válido. |
| Salir | — | Cierra el IDE (pregunta si hay cambios sin guardar). |

El compilador no distingue por la extensión: un programa en `.txt` y otro en `.leaf` se compilan igual.

---

## 4. Compilar y ver las fases

Pulsa **▶ Compilar** o **F5** (también el menú *Compilar → Compilar*). El IDE compila lo que hay en el editor, **aunque no lo hayas guardado**.

A la derecha aparece una pestaña por fase, con el estado en su título (`· OK`, `· con errores`, `· omitida`, `· próximamente`):

| Fase | Qué muestra |
|---|---|
| **Análisis léxico** | Tabla de **tokens** (número, línea, columna, tipo, lexema, categoría) y un conteo por categoría. Tiene un cuadro de **filtro**. |
| **Análisis sintáctico** | El **árbol sintáctico** (AST) del programa y un volcado en texto. |
| **Análisis semántico** | La **tabla de símbolos** agrupada por ámbito (nombre, tipo, categoría, ámbito, offset…) y la **traza de ámbitos** (entrar, salir, insertar, buscar). |
| **Código intermedio (cuádruplos)** | Tres pestañas: **Cuádruplos** (operación, arg 1, arg 2, resultado, agrupadas en «Programa principal» y «Función X»), **Tres direcciones** (el mismo código escrito como `t1 = $a + $b`) y **Temporales** (el tipo de cada `t1`, `t2`…). |
| Optimización · Código objeto | En gris, «próximamente». |

**Si una fase tiene errores**, las siguientes se marcan como *omitidas*: por ejemplo, si hay un error sintáctico no se hace el análisis semántico, y el código intermedio solo existe cuando el programa **no tiene ningún error**.

### Enlace entre las vistas y el código
- Haz **clic en una fila** de tokens (o en un nodo del árbol, o en una fila de la tabla de símbolos): el IDE **marca ese tramo en el editor**.
- Al revés, al mover el cursor por el editor se **seleccionan** el token o el nodo que están bajo él.
- Las tablas permiten **filtrar** (cuadro de texto) y **agrupar** (casilla *Agrupar*).
- Las filas de la tabla de cuádruplos todavía no enlazan con el código: el compilador no guarda de qué línea viene cada cuádrupla.

---

## 5. Errores: el panel Problemas

La pestaña **Problemas** (abajo) lista todos los errores con su fase, línea, columna y mensaje; su título muestra el total, por ejemplo `Problemas (3)`. Los errores también se **subrayan en el editor**.

- **Clic en un problema** lleva el cursor a esa línea y columna.
- El estado de la barra inferior resume el resultado: «Compilación sin errores» o «✖ N diagnósticos».

---

## 6. Carpeta de trabajo y Explorador

La **carpeta de trabajo** es el «proyecto» sobre el que trabaja el IDE (por ejemplo, la carpeta donde guardas tus programas o la del compilador).

- **Archivo → Abrir carpeta…** (Ctrl+Mayús+O) la elige. El IDE **la recuerda** y la abre sola la próxima vez.
- Las dos consolas (Leaf y Sistema) **arrancan en esa carpeta** y se mueven a ella cuando abres otra. Los diálogos de abrir y guardar también parten de ahí.
- Si la terminal Sistema tiene un programa en marcha, no se mueve y el estado te avisa.

El panel **Explorador** muestra el árbol de la carpeta:
- Solo carpetas y archivos **`.txt` y `.leaf`**. Se ocultan las carpetas que empiezan con punto y las de compilación (`target`, `out`).
- **Doble clic** (o Enter) sobre un archivo lo abre en el editor; si había cambios sin guardar, pregunta.
- El botón **↻** vuelve a leer la carpeta (útil si creaste un archivo desde fuera).

---

## 7. Las consolas

Abajo hay dos consolas en pestañas. La que queda al frente al abrir es **Sistema**; al pulsar F5, el IDE muestra **Leaf** con el resultado.

En ambas: **↑ ↓** recorren el historial y **Ctrl+L** limpia la pantalla. Solo en la consola **Leaf**: **Tab** completa nombres de comandos, fases y valores, y con **doble clic** sobre una línea que diga «Línea N, Columna M» el cursor del editor salta ahí.

### Consola Leaf
Es una consola propia del IDE con comandos para trabajar con el compilador. Escribe `ayuda` para ver la lista, o `ayuda <comando>` para el detalle.

| Comando | Qué hace |
|---|---|
| `compilar [archivo] [--hasta=fase]` | Compila el editor (o un archivo) y muestra los errores. Sin archivo, actualiza las pestañas igual que el botón. |
| `leafc <archivo> [--hasta=fase]` | Compila un archivo del disco sin tocar el editor. |
| `lexico`, `sintactico`, `semantico`, `intermedio` | Compilan hasta esa fase. |
| `tokens [archivo]` | Imprime la tabla de tokens. |
| `ast [archivo]` | Imprime el árbol sintáctico. |
| `simbolos [archivo]` | Imprime la tabla de símbolos. |
| `cuadruplos [archivo]` | Imprime la tabla de cuádruplos. |
| `errores [archivo]` | Muestra solo el informe de errores. |
| `fases` | Lista las fases y cuáles están implementadas. |
| `version` | Versión del IDE, del compilador y de Java. |
| `abrir <ruta>`, `guardar [ruta]`, `nuevo` | Manejan el archivo del editor. |
| `ls [ruta]`, `cd [ruta]` | Listan y cambian la carpeta de esta consola. |
| `tema claro` / `tema oscuro` | Cambia el tema. |
| `perspectiva [nombre]` | Sin nombre, lista las perspectivas; con nombre, la aplica. |
| `limpiar` (o `cls`) | Borra la pantalla. |
| `ejecutar` | Todavía no disponible (avisa). |

Las rutas pueden ser relativas a la carpeta de la consola. Los valores de `--hasta` son `lexico`, `sintactico`, `semantico`, `intermedio`, `optimizacion` y `objeto`.

### Terminal Sistema
Es un `cmd` de Windows dentro del IDE. Cada línea se ejecuta como comando, en la carpeta de trabajo; `cd` cambia de carpeta y `cd C:\otra` o `D:` cambian de unidad. Si un programa pide datos, lo que escribas va a su entrada. **Ctrl+C** detiene el programa en marcha. No es una terminal completa: sirve para comandos y programas de línea de comandos, no para programas de pantalla completa ni colores ANSI.

El IDE prepara el `CLASSPATH` para que funcione el comando del compilador, como en el manual:
```
java generado.Leaf ruta\al\programa.txt
```
Esa línea imprime la salida completa del compilador (informe de errores y, si no hay errores, la tabla de cuádruplos). Con `--guardar` el compilador escribe además un archivo `.cuad` junto al programa, y con `--tres-dir` agrega la forma de tres direcciones.

---

## 8. Terminal sincronizada con el IDE

Cuando escribes en la terminal **Sistema** del IDE `java generado.Leaf <archivo>`, además de ejecutarse el comando de verdad, **el IDE abre ese archivo en el editor y muestra todas sus fases**. Así lo que ves en la terminal y en las pestañas es del mismo programa.

- La ruta relativa se resuelve contra la carpeta que muestra el prompt de la terminal.
- Si el editor tenía cambios sin guardar, el IDE pregunta antes de abrir el archivo.
- Si el archivo no existe, la terminal muestra su propio error y el IDE no cambia nada.
- Con el **modo demostración** activo, la demostración se reinicia con ese archivo.
- Se activa y desactiva en **Compilar → Sincronizar la terminal Sistema con el IDE** (viene activado y se recuerda).
- Solo funciona con la terminal Sistema **del IDE**; un `cmd` abierto aparte no avisa al IDE.

---

## 9. Paneles y perspectivas

Todos los paneles se pueden **arrastrar**: suelta la cabecera de un panel sobre otro para formar pestañas, junto a un borde para dividir, o fuera de la ventana para convertirlo en una **ventana aparte** (útil con un segundo monitor).

- El menú **⋮** de cada panel permite **separarlo a una ventana** o devolverlo.
- Un panel cerrado con la **×** vuelve desde **Ver → Paneles**.
- El Editor no se puede cerrar.

### Perspectivas
Una *perspectiva* es una disposición predefinida de los paneles. Se eligen en **Ver → Perspectiva** o con el comando `perspectiva`:

| Perspectiva | Disposición |
|---|---|
| **Escritura** | Explorador, editor y consolas. Las fases ocultas. |
| **Análisis** | Explorador, editor y las fases léxica, sintáctica y semántica en pestañas. (La que abre la primera vez.) |
| **Síntesis** | Editor y, en columnas, código intermedio, optimización y código objeto. |
| **Evaluación (3 pantallas)** | Pantalla 1: explorador, editor, consolas y las cuatro primeras fases (léxico, sintáctico, semántico e intermedio) en pestañas. Pantalla 2: Optimización. Pantalla 3: Código objeto. Con varios monitores, las ventanas 2 y 3 se abren en el segundo y tercer monitor; con uno solo, se abren encima del principal. |

### Guardar tu propia disposición
- **Ver → Restaurar diseño:** vuelve a armar la última perspectiva elegida.
- **Ver → Guardar disposición actual como…:** guarda cómo tienes los paneles con un nombre.
- **Ver → Disposiciones guardadas:** lista y recupera las que guardaste.
- Al cerrar, el IDE guarda la disposición y **la restaura al abrir**, incluidas las ventanas separadas.

---

## 10. Modo demostración

Sirve para **explicar una compilación paso a paso**. Es **opcional**: siempre está apagado al abrir el IDE y no cambia nada mientras no lo actives.

**Activar:** botón **Demostración** de la barra, o *Compilar → Modo demostración*. Aparece una barra de controles y una línea de narración.

El IDE compila el programa del editor y lo «reproduce» fase por fase: los tokens van apareciendo en la tabla, el árbol crece nodo por nodo, aparecen los símbolos y por último las cuádruplas. La fase en curso lleva **▶** en su título, se pone al frente, y cada elemento marca su tramo en el editor.

| Control | Atajo | Qué hace |
|---|---|---|
| ⏮ Reiniciar | — | Vuelve al principio. |
| ▶ Paso | F6 | Muestra un elemento más. |
| ⏭ Siguiente fase | F7 | Completa la fase actual y pasa a la siguiente. |
| ▶▶ Automático / ⏸ Pausa | F8 | Avanza solo; el deslizador **Velocidad** ajusta el ritmo. |

La línea de narración usa datos reales, por ejemplo: «Léxico: 4 de 14 → 4 · 1 · 17 · ENTERO_T · entero · Tipo de dato». Si una fase tiene errores, la demostración llega hasta ahí y los errores se anuncian al final.

Mientras está activo: F5 y el comando `compilar` reinician la demostración, el análisis en vivo no pisa lo mostrado, y si editas el código la demostración se vuelve a compilar desde cero. Se apaga volviendo a pulsar el botón.

---

## 11. Tema, ajustes y archivos que guarda el IDE

- **Tema:** *Ver → Tema claro / Tema oscuro* (o el comando `tema`).
- **Limpiar consola:** *Ver → Limpiar consola* limpia la consola que tengas en uso.

El IDE guarda todo en la carpeta **`%USERPROFILE%\.leafide`**:

| Archivo | Contenido |
|---|---|
| `ajustes.properties` | Tema, análisis en vivo, resaltado, última perspectiva, carpeta de trabajo y la sincronización de la terminal. |
| `layouts\ultima.xml` | La disposición de los paneles al cerrar (se restaura al abrir). |
| `layouts\<nombre>.xml` | Las disposiciones que guardaste con nombre. |

Para **empezar de cero**, cierra el IDE y borra esa carpeta (o solo el archivo que quieras reiniciar). No contiene tus programas.

---

## 12. Atajos de teclado

| Atajo | Acción |
|---|---|
| F5 | Compilar |
| Ctrl+N / Ctrl+O / Ctrl+S | Nuevo / Abrir / Guardar |
| Ctrl+Mayús+S | Guardar como |
| Ctrl+Mayús+O | Abrir carpeta |
| F6 / F7 / F8 | Demostración: paso / siguiente fase / automático o pausa |
| ↑ ↓ (en una consola) | Historial |
| Tab (en la consola Leaf) | Completar |
| Ctrl+L (en una consola) | Limpiar pantalla |
| Ctrl+C (en la terminal Sistema) | Detener el programa en marcha (con texto seleccionado, copia) |

---

## 13. Problemas frecuentes

| Síntoma | Causa probable y solución |
|---|---|
| `ejecutar-ide.bat` falla con muchos errores `cannot find symbol` en `generado` | Carpeta incompleta o sincronizada por OneDrive. Mueve el proyecto a una ruta como `C:\LeafIDE` y vuelve a clonar con Git (no copies a mano). Si ya había un intento fallido, ejecuta `mvnw.cmd clean package -DskipTests`. |
| Error de versión de Java | Instala el **JDK 26** (no el JRE) y comprueba `java -version`. Si hay otro Java antes en el PATH, quítalo o ajusta `JAVA_HOME`. |
| No descarga nada / se queda esperando | La primera vez hace falta internet. Revisa proxy, firewall o antivirus que bloqueen `repo.maven.apache.org`. |
| `ejecutar-ide.bat rapido` dice que no puede acceder al archivo jar | Aún no se ha compilado: ejecútalo sin `rapido`. |
| No puedo recompilar y aparece un error de acceso al `.jar` | Hay una ventana de LeafIDE abierta (Windows bloquea el archivo). Ciérrala y vuelve a ejecutar. |
| El comando `java generado.Leaf …` de la terminal Sistema dice que no encuentra la clase | Solo está preparado cuando el IDE se abre con `ejecutar-ide.bat`. Si lo abres de otra forma, la terminal no sabe dónde está el compilador. |
| El explorador dice «Sin carpeta» | Usa *Archivo → Abrir carpeta…*. Si la carpeta guardada ya no existe, el IDE vuelve a pedirla. |
| Al arrancar sale un aviso de FlatLaf sobre «restricted method» | Es un aviso de Java sobre una biblioteca; no afecta al funcionamiento. |
| Quiero volver a como estaba al principio | *Ver → Restaurar diseño*, o borra `%USERPROFILE%\.leafide\layouts\ultima.xml`. |

---

## 14. Lo que el IDE todavía no hace
- **No ejecuta programas Leaf:** el comando `ejecutar` y las fases de **optimización** y **código objeto** aún no existen.
- Edita **un archivo a la vez** (sin pestañas de documentos), y el explorador no crea, borra ni renombra archivos.
- El explorador solo lista `.txt` y `.leaf`.
- Las filas de la tabla de cuádruplos no enlazan con la línea del código.
- No hay autocompletado de código.

Versión del IDE: ver `version` en la consola Leaf. Código y ejemplos: <https://github.com/Wharf-Code-Solutions/LeafIDE>.
