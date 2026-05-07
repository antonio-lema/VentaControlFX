package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.infrastructure.persistence.BackupService;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.application.Platform;
import java.io.InputStream;
import java.util.Properties;

public class BackupController implements Injectable {

    @FXML
    private Label lblStatus;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private javafx.scene.control.ListView<String> lvBackups;

    private ServiceContainer container;
    private final javafx.collections.ObservableList<String> backupFiles = javafx.collections.FXCollections
            .observableArrayList();

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        progressBar.setVisible(false);
        lvBackups.setItems(backupFiles);
        refreshBackupList();
    }

    private void refreshBackupList() {
        backupFiles.clear();
        java.io.File folder = new java.io.File(System.getProperty("user.home"), "VentaControl/backups");
        if (folder.exists() && folder.isDirectory()) {
            java.io.File[] files = folder.listFiles((dir, name) -> name.endsWith(".sql"));
            if (files != null) {
                // Sort by date (newest first)
                java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                for (java.io.File f : files) {
                    backupFiles.add(f.getName());
                }
            }
        }
    }

    @FXML
    private void handleCreateBackup() {
        progressBar.setVisible(true);
        lblStatus.setText(container.getBundle().getString("backup.status.running"));

        new Thread(() -> {
            try {
                Properties prop = new Properties();
                try (InputStream input = getClass().getClassLoader().getResourceAsStream("config/db.properties")) {
                    prop.load(input);
                }

                String url = prop.getProperty("db.url");
                String user = prop.getProperty("db.user");
                String pass = prop.getProperty("db.password");

                // Parse URL (e.g. jdbc:mysql://localhost:3306/tpv_bazar?...)
                String cleanUrl = url.substring(url.indexOf("//") + 2);
                int firstSlash = cleanUrl.indexOf("/");
                String hostPort = cleanUrl.substring(0, firstSlash);
                String host = "localhost";
                String port = "3306";
                if (hostPort.contains(":")) {
                    String[] parts = hostPort.split(":");
                    host = parts[0];
                    port = parts[1];
                }

                String dbName = cleanUrl.substring(firstSlash + 1);
                if (dbName.contains("?")) {
                    dbName = dbName.substring(0, dbName.indexOf("?"));
                }
                dbName = dbName.trim();

                System.out.println("[BACKUP] Info: DB=" + dbName + ", Host=" + host + ", User=" + user);

                BackupService service = new BackupService();
                BackupService.BackupResult result = service.createBackup(dbName, user, pass, host, port);

                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    if (result.success) {
                        lblStatus.setText(container.getBundle().getString("backup.status.success"));
                        AlertUtil.showInfo(container.getBundle().getString("backup.title"),
                                container.getBundle().getString("backup.msg.success") + "\n" + result.filePath);
                        refreshBackupList();
                    } else {
                        lblStatus.setText(container.getBundle().getString("backup.status.error"));
                        AlertUtil.showError(container.getBundle().getString("backup.title"),
                                container.getBundle().getString("backup.msg.error") + ": " + result.message);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    lblStatus.setText("Error: " + e.getMessage());
                    AlertUtil.showError("Backup", "Error: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleRestoreBackup() {
        String selected = lvBackups.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning(container.getBundle().getString("backup.title"), "Seleccione una copia de la lista");
            return;
        }

        boolean confirm = AlertUtil.showConfirmation(container.getBundle().getString("backup.title"),
                "\u00bfEst\u00e1 seguro de restaurar esta copia? Se sobreescribir\u00e1 la base de datos actual.",
                "Esta acci\u00f3n no se puede deshacer.");

        if (!confirm)
            return;

        progressBar.setVisible(true);
        lblStatus.setText("Restaurando base de datos...");

        new Thread(() -> {
            try {
                Properties prop = new Properties();
                try (InputStream input = getClass().getClassLoader().getResourceAsStream("config/db.properties")) {
                    prop.load(input);
                }

                String url = prop.getProperty("db.url");
                String user = prop.getProperty("db.user");
                String pass = prop.getProperty("db.password");

                String cleanUrl = url.substring(url.indexOf("//") + 2);
                int firstSlash = cleanUrl.indexOf("/");
                String hostPort = cleanUrl.substring(0, firstSlash);
                String host = "localhost";
                String port = "3306";
                if (hostPort.contains(":")) {
                    String[] parts = hostPort.split(":");
                    host = parts[0];
                    port = parts[1];
                }

                String dbName = cleanUrl.substring(firstSlash + 1);
                if (dbName.contains("?")) {
                    dbName = dbName.substring(0, dbName.indexOf("?"));
                }
                dbName = dbName.trim();

                java.io.File backupFile = new java.io.File(System.getProperty("user.home"),
                        "VentaControl/backups/" + selected);

                BackupService service = new BackupService();
                BackupService.BackupResult result = service.restoreBackup(dbName, user, pass, host, port,
                        backupFile.getAbsolutePath());

                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    if (result.success) {
                        lblStatus.setText("Restauraci\u00f3n completada");
                        AlertUtil.showInfo(container.getBundle().getString("backup.title"),
                                "Base de datos restaurada correctamente. Se recomienda reiniciar la aplicaci\u00f3n.");
                    } else {
                        lblStatus.setText("Error en restauraci\u00f3n");
                        AlertUtil.showError(container.getBundle().getString("backup.title"),
                                "No se pudo restaurar: " + result.message);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    lblStatus.setText("Error: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleOpenFolder() {
        try {
            java.io.File folder = new java.io.File(System.getProperty("user.home"), "VentaControl/backups");
            if (folder.exists()) {
                java.awt.Desktop.getDesktop().open(folder);
            } else {
                folder.mkdirs();
                java.awt.Desktop.getDesktop().open(folder);
            }
        } catch (Exception e) {
            AlertUtil.showError("Backup", "No se pudo abrir la carpeta: " + e.getMessage());
        }
    }
}

