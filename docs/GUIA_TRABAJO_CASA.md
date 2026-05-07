# 🏠 Guía para trabajar en VentaControlFX desde casa

Para que la aplicación funcione exactamente igual que en la oficina, sigue estos pasos:

## 1. Preparación del Entorno
Asegúrate de tener instalado en tu PC de casa:
* **Java 21 JDK**
* **MySQL** (te recomiendo instalar **XAMPP** igual que aquí para no tener líos de configuración)
* **VS Code** con la extensión de **Antigravity**.

## 2. Configurar la Base de Datos
1. Abre tu panel de control de MySQL en casa.
2. Crea una base de datos vacía llamada `tpv_bazar`.
3. Importa el archivo de respaldo que he generado (`backup_tpv_bazar.sql`). Desde una terminal en la carpeta del proyecto ejecuta:
   ```powershell
   # Usando la ruta de XAMPP
   C:\xampp\mysql\bin\mysql.exe -u root tpv_bazar < backup_tpv_bazar.sql
   ```

## 3. Llevarte el "Cerebro" de Antigravity
Si quieres que yo sepa todo lo que hemos hecho hoy:
1. Copia la carpeta: `C:\Users\practicassoftware1\.gemini\antigravity`
2. Pégala en tu casa en: `C:\Users\TU_USUARIO\.gemini\antigravity`

## 4. Ejecutar
Usa el archivo `VentaControl.bat` que está en la raíz o ejecuta:
```bash
mvn javafx:run
```
