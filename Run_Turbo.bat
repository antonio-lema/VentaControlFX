@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
echo Iniciando VentaControlFX (Modo Turbo)...
java --module-path "target/lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics -jar "target/VentaControlFX-1.0-SNAPSHOT.jar"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] No se pudo lanzar el JAR directamente. 
    echo Asegurate de haber ejecutado 'mvn clean package' al menos una vez.
    echo Cayendo a modo compatible (Maven)...
    mvn javafx:run
)
pause
