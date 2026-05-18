@echo off
setlocal
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "target\ventacontrolfx-1.0.jar" goto MAVEN_MODE

echo [INFO] Iniciando en Modo Turbo (Directo)...
java -jar "target\ventacontrolfx-1.0.jar"
if %ERRORLEVEL% EQU 0 goto END

:MAVEN_MODE
echo [INFO] Iniciando via Maven (Lento)...
call "C:\Program Files\apache-maven-3.9.12.bin\apache-maven-3.9.12\bin\mvn.cmd" javafx:run

:END
pause
