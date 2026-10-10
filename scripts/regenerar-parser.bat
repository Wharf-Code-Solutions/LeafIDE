@echo off
rem ============================================================
rem  Regenera el parser de Leaf desde compiler\grammar\Leaf.jjt
rem  Usar SOLO cuando cambie la gramatica. Requiere jjtree y
rem  javacc en el PATH (JavaCC 7.0.13 verificado en M0).
rem  Los .java generados quedan en compiler\src\main\java\generado
rem  y se versionan (ver docs\decisiones.md).
rem ============================================================
setlocal
chcp 65001 >nul
set "RAIZ=%~dp0.."
pushd "%RAIZ%\compiler\src\main\java" || exit /b 1

echo [1/2] jjtree ..\..\..\grammar\Leaf.jjt
call jjtree ..\..\..\grammar\Leaf.jjt
if errorlevel 1 goto :error

echo [2/2] javacc generado\Leaf.jj
call javacc generado\Leaf.jj
if errorlevel 1 goto :error

popd
echo Parser regenerado. Ahora ejecuta scripts\build-compiler.bat
exit /b 0

:error
popd
echo ERROR: fallo la regeneracion del parser.
exit /b 1
