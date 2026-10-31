@echo off
rem ============================================================
rem  Actualiza la COPIA DE RESPALDO del compilador que lleva el IDE
rem  (compiler\) desde un clon del repositorio Leaf.
rem    scripts\actualizar-compilador.bat <carpeta-del-clon>
rem  Copia src\Leaf.jjt y TODOS los .java de src\generado\ (los hechos
rem  a mano: hoy son 12) y regenera el parser (necesita jjtree y javacc
rem  en el PATH). NO toca ServicioCompilacion ni ResultadoCompilacion,
rem  que son del IDE. Solo LEE el clon; nunca lo modifica.
rem  Usa un clon limpio (git clone): si apuntas a tu carpeta de trabajo,
rem  tambien se copian los .java que alli haya generado JavaCC.
rem  Despues: mvnw.cmd test, y si los dorados cambian a proposito,
rem  regeneralos (ver AGENTS.md) y revisa el git diff.
rem  Ojo: el IDE usa por defecto el compilador de C:\Leaf (sus .class);
rem  esta copia es el respaldo (Compilar - Compilador - Copia del IDE).
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
if not exist "%CLON%\src\generado\VisitanteSemantico.java" (
    echo ERROR: faltan los archivos del compilador en "%CLON%\src\generado".
    exit /b 1
)
set "DESTINO=%RAIZ%\compiler\src\main\java\generado"

echo [1/3] Copiando la gramatica y los .java del compilador...
copy /y "%CLON%\src\Leaf.jjt" "%RAIZ%\compiler\grammar\Leaf.jjt" >nul || exit /b 1
set /a N=0
for %%F in ("%CLON%\src\generado\*.java") do (
    copy /y "%%~fF" "%DESTINO%\%%~nxF" >nul || exit /b 1
    set /a N+=1
)
echo       %N% archivos .java copiados.

echo [2/3] Regenerando el parser...
call "%~dp0regenerar-parser.bat" || exit /b 1

echo [3/3] Anotando la version importada...
if not exist "%RAIZ%\compiler\src\main\resources" mkdir "%RAIZ%\compiler\src\main\resources"
git -C "%CLON%" rev-parse HEAD > "%RAIZ%\compiler\src\main\resources\VERSION-IMPORTADA.txt" 2>nul

echo Compilador actualizado. Ahora ejecuta mvnw.cmd test.
exit /b 0
