package com.mycompany.test;

import com.mycompany.ventacontrolfx.infrastructure.persistence.BackupService;

public class ManualBackupTest {
    public static void main(String[] args) {
        System.out.println("Iniciando prueba de backup manual...");
        BackupService service = new BackupService();
        BackupService.BackupResult result = service.createDefaultBackup();
        if (result.success) {
            System.out.println("¡EXXITO! Backup creado en: " + result.filePath);
        } else {
            System.err.println("ERROR: " + result.message);
        }
    }
}
