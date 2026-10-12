@echo off
rem ============================================================
rem  Actualiza el compilador del IDE desde un clon del repositorio Leaf.
rem    scripts\actualizar-compilador.bat <carpeta-del-clon>
rem  Copia src\Leaf.jjt y los 5 archivos escritos a mano de src\generado\
rem  (Cuadrupla, GeneradorCodigoIntermedio, Simbolo, TablaSimbolos y
rem  VisitanteSemantico) y regenera el parser (necesita jjtree y javacc
rem  en el PATH). NO toca ServicioCompilacion ni ResultadoCompilacion,
rem  que son del IDE. Solo LEE el clon; nunca lo modifica.
rem  Despues: mvnw.cmd test, y si los dorados cambian a proposito,
rem  regeneralos (ver AGENTS.md) y revisa el git diff.
rem ============================================================
setlocal
chcp 65001 >nul
set "RAIZ=%~dp0.."
set "CLON=%~1"
if "%CLON%"=="" (
    echo Uso: scripts\actualizar-compilador.bat ^<carpeta-del-clon-de-Leaf^>
    exit /b 1
)
if not exist "%CLON%\src\Leaf.jjt" (
    echo ERROR: no existe "%CLON%\src\Leaf.jjt". Es la carpeta del clon?
    exit /b 1
)
set "DESTINO=%RAIZ%\compiler\src\main\java\generado"

echo [1/3] Copiando la gramatica y los archivos hechos a mano...
copy /y "%CLON%\src\Leaf.jjt" "%RAIZ%\compiler\grammar\Leaf.jjt" >nul || exit /b 1
for %%F in (Cuadrupla GeneradorCodigoIntermedio Simbolo TablaSimbolos VisitanteSemantico) do (
    if not exist "%CLON%\src\generado\%%F.java" (
        echo ERROR: falta "%CLON%\src\generado\%%F.java"
        exit /b 1
    )
    copy /y "%CLON%\src\generado\%%F.java" "%DESTINO%\%%F.java" >nul || exit /b 1
)

echo [2/3] Regenerando el parser...
call "%~dp0regenerar-parser.bat" || exit /b 1

echo [3/3] Anotando la version importada...
if not exist "%RAIZ%\compiler\src\main\resources" mkdir "%RAIZ%\compiler\src\main\resources"
git -C "%CLON%" rev-parse HEAD > "%RAIZ%\compiler\src\main\resources\VERSION-IMPORTADA.txt" 2>nul

echo Compilador actualizado. Ahora ejecuta mvnw.cmd test.
exit /b 0
