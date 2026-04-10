package com.mycompany.ventacontrolfx.infrastructure.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BackupService {

    public static class BackupResult {
        public final boolean success;
        public final String message;
        public final String filePath;

        public BackupResult(boolean success, String message, String filePath) {
            this.success = success;
            this.message = message;
            this.filePath = filePath;
        }
    }

    public BackupResult createBackup(String dbName, String user, String password, String host, String port) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "backup_" + dbName + "_" + timestamp + ".sql";

            // Create backups directory if not exists
            Path backupDir = Paths.get(System.getProperty("user.home"), "VentaControl", "backups");
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            File outputFile = backupDir.resolve(fileName).toFile();

            // Build mysqldump command
            String mysqldumpPath = findMysqldump();
            java.util.List<String> cmd = new java.util.ArrayList<>();
            cmd.add(mysqldumpPath);
            cmd.add("-h" + host);
            cmd.add("-P" + port);
            cmd.add("-u" + user);

            if (password != null && !password.isEmpty()) {
                cmd.add("-p" + password);
            }

            cmd.add(dbName);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectOutput(outputFile);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return new BackupResult(true, "Backup creado exitosamente", outputFile.getAbsolutePath());
            } else {
                return new BackupResult(false, "Error ejecutando mysqldump: " + output.toString(), null);
            }

        } catch (Exception e) {
            return new BackupResult(false, "Excepción durante el backup: " + e.getMessage(), null);
        }
    }

    /**
     * Crea un backup usando los credenciales por defecto de db.properties.
     */
    public BackupResult createDefaultBackup() {
        try (java.io.InputStream input = getClass().getClassLoader().getResourceAsStream("config/db.properties")) {
            java.util.Properties prop = new java.util.Properties();
            if (input == null) {
                return new BackupResult(false, "No se pudo encontrar config/db.properties", null);
            }
            prop.load(input);
            String url = prop.getProperty("db.url"); // jdbc:mysql://localhost:3306/tpv_bazar
            String user = prop.getProperty("db.user");
            String pass = prop.getProperty("db.password");

            // Parse URL
            String dbName = url.substring(url.lastIndexOf("/") + 1);
            String hostPort = url.substring(url.indexOf("//") + 2, url.lastIndexOf("/"));
            String host = hostPort.contains(":") ? hostPort.split(":")[0] : hostPort;
            String port = hostPort.contains(":") ? hostPort.split(":")[1] : "3306";

            return createBackup(dbName, user, pass, host, port);
        } catch (Exception e) {
            return new BackupResult(false, "Error leyendo configuración para backup: " + e.getMessage(), null);
        }
    }

    public BackupResult restoreBackup(String dbName, String user, String password, String host, String port,
            String filePath) {
        try {
            File inputFile = new File(filePath);
            if (!inputFile.exists()) {
                return new BackupResult(false, "El archivo de backup no existe", null);
            }

            String mysqlPath = findMysql();
            java.util.List<String> cmd = new java.util.ArrayList<>();
            cmd.add(mysqlPath);
            cmd.add("-h" + host);
            cmd.add("-P" + port);
            cmd.add("-u" + user);

            if (password != null && !password.isEmpty()) {
                cmd.add("-p" + password);
            }

            cmd.add(dbName);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectInput(inputFile);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return new BackupResult(true, "Base de datos restaurada exitosamente", filePath);
            } else {
                return new BackupResult(false, "Error ejecutando mysql: " + output.toString(), null);
            }

        } catch (Exception e) {
            return new BackupResult(false, "Excepción durante la restauración: " + e.getMessage(), null);
        }
    }

    private String findMysqldump() {
        String[] commonPaths = {
                "mysqldump",
                "C:\\xampp\\mysql\\bin\\mysqldump.exe",
                "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe",
                "C:\\Program Files\\MySQL\\MySQL Server 5.7\\bin\\mysqldump.exe",
                "C:\\Program Files\\MySQL\\MySQL Server 5.6\\bin\\mysqldump.exe"
        };

        return resolvePath(commonPaths, "--version");
    }

    private String findMysql() {
        String[] commonPaths = {
                "mysql",
                "C:\\xampp\\mysql\\bin\\mysql.exe",
                "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe",
                "C:\\Program Files\\MySQL\\MySQL Server 5.7\\bin\\mysql.exe",
                "C:\\Program Files\\MySQL\\MySQL Server 5.6\\bin\\mysql.exe"
        };

        return resolvePath(commonPaths, "--version");
    }

    private String resolvePath(String[] paths, String versionFlag) {
        for (String path : paths) {
            if (path.equals("mysql") || path.equals("mysqldump")) {
                try {
                    new ProcessBuilder(path, versionFlag).start().waitFor();
                    return path;
                } catch (Exception e) {
                    continue;
                }
            }
            File f = new File(path);
            if (f.exists()) {
                return path;
            }
        }
        return paths[0]; // Fallback
    }
}
