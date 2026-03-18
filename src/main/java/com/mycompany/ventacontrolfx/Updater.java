package com.mycompany.ventacontrolfx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Updater {
    public static void main(String[] args) throws IOException {
        updateSignatures("c:/Users/practicassoftware1/Documents/NetBeansProjects/VentaControlFX/src/main/java/com/mycompany/ventacontrolfx/domain/repository/IPriceRepository.java", false);
        updateSignatures("c:/Users/practicassoftware1/Documents/NetBeansProjects/VentaControlFX/src/main/java/com/mycompany/ventacontrolfx/infrastructure/persistence/JdbcPriceRepository.java", true);
        updateSignatures("c:/Users/practicassoftware1/Documents/NetBeansProjects/VentaControlFX/src/main/java/com/mycompany/ventacontrolfx/application/usecase/MassivePriceUpdateUseCase.java", false);
        System.out.println("Java signature updates completed!");
    }

    private static void updateSignatures(String path, boolean isRepoImpl) throws IOException {
        String content = Files.readString(Paths.get(path));
        
        // 1. Update Signatures
        content = content.replace("double multiplier, String reason)", "double multiplier, String reason, java.time.LocalDateTime startDate)");
        content = content.replace("double amount, String reason)", "double amount, String reason, java.time.LocalDateTime startDate)");
        content = content.replace("double roundingTarget, String reason)", "double roundingTarget, String reason, java.time.LocalDateTime startDate)");
        content = content.replace("double targetDecimal, String reason)", "double targetDecimal, String reason, java.time.LocalDateTime startDate)");
        content = content.replace("boolean isPercentage)", "boolean isPercentage, java.time.LocalDateTime startDate)");

        // 2. Fix JdbcPriceRepository internal uses
        if (isRepoImpl) {
            content = content.replace("LocalDateTime now = LocalDateTime.now();", "");
            content = content.replace(", now, ", ", startDate, ");
            // Particular edge cases from view_file lines:
            content = content.replace("executeBulkRounding(conn, ps, priceListId, roundingTarget, reason, now, globalTax)", "executeBulkRounding(conn, ps, priceListId, roundingTarget, reason, startDate, globalTax)");
        }

        Files.writeString(Paths.get(path), content);
    }
}
