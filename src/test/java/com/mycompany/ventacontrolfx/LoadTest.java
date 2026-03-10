package com.mycompany.ventacontrolfx;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;

public class LoadTest {
    @Test
    public void testLoadFXML() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> {
            try {
                String[] fxmls = {
                        "/view/products.fxml",
                        "/view/categories.fxml",
                        "/view/price_lists.fxml",
                        "/view/vat_management.fxml",
                        "/view/clients.fxml",
                        "/view/fiscal_documents.fxml",
                        "/view/manage_roles.fxml",
                        "/view/manage_users.fxml"
                };
                for (String fxml : fxmls) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
                        loader.load();
                        System.out.println("SUCCESS loaded " + fxml);
                    } catch (Exception e) {
                        System.out.println("FAILED loading " + fxml);
                        e.printStackTrace();
                    }
                }
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }
}
