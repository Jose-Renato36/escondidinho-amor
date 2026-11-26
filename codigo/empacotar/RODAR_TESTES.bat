@echo off
chcp 65001 >nul
cd /d "%~dp0.."
"_sistema\java\bin\java.exe" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "_sistema\tools\junit-platform-console-standalone.jar;_sistema\diarios.jar;_sistema\testes.jar;_sistema\lib\*" org.junit.platform.console.ConsoleLauncher execute --select-package diarios --details=tree --disable-banner --disable-ansi-colors
pause
