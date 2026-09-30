@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist "_sistema\java\bin\java.exe" (
  echo Extraia o zip INTEIRO antes de abrir.
  pause
  exit /b 1
)
"_sistema\java\bin\java.exe" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar "_sistema\diarios.jar" %*
pause
