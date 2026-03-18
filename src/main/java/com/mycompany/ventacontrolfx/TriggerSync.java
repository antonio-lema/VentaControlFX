package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.JdbcTaxRepository;
import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;

public class TriggerSync {
    public static void main(String[] args) {
        JdbcTaxRepository repo = new JdbcTaxRepository();
        try {
            System.out.println("Iniciando sincronización manual de IVA...");
            repo.syncMirroredValues();
            System.out.println("Sincronización completada con éxito.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
