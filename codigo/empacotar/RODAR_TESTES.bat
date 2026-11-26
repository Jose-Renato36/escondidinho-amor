@echo off
chcp 65001 >nul
cd /d "%~dp0.."
"_sistema\java\bin\java.exe" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar "_sistema\lib\junit-platform-console-standalone.jar" execute --class-path "_sistema\diarios.jar;_sistema\testes.jar;_sistema\lib\sqlite-jdbc.jar" --select-package diarios --details=tree --disable-banner --disable-ansi-colors
pause
