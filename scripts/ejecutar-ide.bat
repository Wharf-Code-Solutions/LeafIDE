@echo off
rem ============================================================
rem  Compila y abre LeafIDE.
rem    scripts\ejecutar-ide.bat           -> mvnw package (sin pruebas) y abre la ventana
rem    scripts\ejecutar-ide.bat rapido    -> abre sin recompilar (usa el ultimo build)
rem ============================================================
setlocal
chcp 65001 >nul
set "RAIZ=%~dp0.."
if /i not "%~1"=="rapido" (
    call "%RAIZ%\mvnw.cmd" -q -f "%RAIZ%\pom.xml" package -DskipTests || exit /b 1
)
java -Dfile.encoding=UTF-8 -jar "%RAIZ%\ide-app\target\leaf-ide-app-0.1.0-SNAPSHOT.jar"
exit /b %errorlevel%
