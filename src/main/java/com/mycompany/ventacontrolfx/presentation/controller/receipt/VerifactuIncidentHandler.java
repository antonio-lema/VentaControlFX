package com.mycompany.ventacontrolfx.presentation.controller.receipt;

import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.infrastructure.external.aeat.JdbcVerifactuRepository;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.List;

/**
 * Gestor de incidencias fiscales (VeriFactu / AEAT).
 * Maneja la visualización del diálogo de justificación legal ante fallos técnicos.
 */
public class VerifactuIncidentHandler {

    private final ServiceContainer container;
    private final JdbcVerifactuRepository verifactuRepository;

    public VerifactuIncidentHandler(ServiceContainer container) {
        this.container = container;
        this.verifactuRepository = new JdbcVerifactuRepository();
    }

    public void handleIncident(List<Integer> affectedSaleIds, List<Integer> affectedReturnIds) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/receipt/verifactu_incident_dialog.fxml"), container.getBundle());
                Parent root = loader.load();
                VerifactuIncidentDialogController controller = loader.getController();

                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Incidencia Técnica VeriFactu");

                Scene scene = new Scene(root);
                container.getThemeManager().applyFullTheme(scene);
                stage.setScene(scene);

                controller.setOnSave(reason -> {
                    verifactuRepository.saveIncidentReason(affectedSaleIds, affectedReturnIds, reason);
                    AlertUtil.showInfo("Éxito", "Justificación registrada correctamente.");
                });

                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}



