@echo off
rem ============================================================
rem  Compila el modulo compiler\ con javac (sin Maven).
rem  Salida: compiler\target\classes (fuera del codigo fuente).
rem ============================================================
setlocal
chcp 65001 >nul
set "RAIZ=%~dp0.."
set "SRC=%RAIZ%\compiler\src\main\java"
set "OUT=%RAIZ%\compiler\target\classes"

if not exist "%OUT%" mkdir "%OUT%"
dir /s /b "%SRC%\*.java" > "%RAIZ%\compiler\target\fuentes.txt"

javac -encoding UTF-8 -d "%OUT%" @"%RAIZ%\compiler\target\fuentes.txt"
if errorlevel 1 (
    echo ERROR: fallo la compilacion del compilador.
    exit /b 1
)
echo Compilador compilado en compiler\target\classes
exit /b 0
