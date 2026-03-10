package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.PriceListUseCase;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.ModalService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class PriceListController implements Injectable {

    @FXML
    private FlowPane priceListsContainer;

    @FXML
    private Label lblCount;

    private PriceListUseCase priceListUseCase;
    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.priceListUseCase = container.getPriceListUseCase();
        loadPriceLists();
    }

    private void loadPriceLists() {
        try {
            List<PriceList> lists = priceListUseCase.getAll();
            Platform.runLater(() -> {
                updateUI(lists);
            });
        } catch (SQLException e) {
            Platform.runLater(() -> AlertUtil.showError("Error", "No se pudieron cargar las tarifas."));
        }
    }

    private void updateUI(List<PriceList> lists) {
        priceListsContainer.getChildren().clear();
        lblCount.setText(lists.size() + " tarifas registradas");

        for (PriceList pl : lists) {
            priceListsContainer.getChildren().add(createPriceListCard(pl));
        }
    }

    private VBox createPriceListCard(PriceList pl) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setPrefWidth(350);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4);");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblName = new Label(pl.getName());
        lblName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        header.getChildren().add(lblName);

        if (pl.isDefault()) {
            Label lblDefault = new Label("Por Defecto");
            lblDefault.setStyle(
                    "-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-padding: 4 8; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            header.getChildren().add(lblDefault);
        }

        if (!pl.isActive()) {
            Label lblInactive = new Label("Inactiva");
            lblInactive.setStyle(
                    "-fx-background-color: #f3f4f6; -fx-text-fill: #4b5563; -fx-padding: 4 8; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            header.getChildren().add(lblInactive);
        }

        Region r = new Region();
        HBox.setHgrow(r, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().add(r);

        VBox content = new VBox(8);
        Label lblInfo = new Label("ID Tarifa: " + pl.getId());
        lblInfo.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13px;");
        content.getChildren().add(lblInfo);

        if (pl.getDescription() != null && !pl.getDescription().isBlank()) {
            Label lblDesc = new Label(pl.getDescription());
            lblDesc.setWrapText(true);
            lblDesc.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 14px; -fx-font-style: italic;");
            content.getChildren().add(lblDesc);
        }

        if (!pl.isDefault()) {
            try {
                String pct = priceListUseCase.getAveragePercentageDifference(pl.getId());
                Label lblPct = new Label("Diferencia media vs principal: " + pct);
                if (pct.startsWith("+")) {
                    lblPct.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-font-size: 13px;");
                } else if (pct.startsWith("-")) {
                    lblPct.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 13px;");
                } else {
                    lblPct.setStyle("-fx-text-fill: #6b7280; -fx-font-weight: bold; -fx-font-size: 13px;");
                }
                content.getChildren().add(lblPct);
            } catch (SQLException ignore) {
            }
        }

        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = new Button("Editar");
        btnEdit.setStyle(
                "-fx-background-color: #eff6ff; -fx-text-fill: #1e40af; -fx-background-radius: 5; -fx-padding: 5 10;");
        btnEdit.setOnAction(e -> openPriceListForm(pl));

        Button btnDelete = new Button("Eliminar");
        btnDelete.setStyle(
                "-fx-background-color: #fef2f2; -fx-text-fill: #b91c1c; -fx-background-radius: 5; -fx-padding: 5 10;");
        btnDelete.setDisable(pl.isDefault() || pl.getId() == 1);
        btnDelete.setOnAction(e -> deletePriceList(pl));

        Button btnMakeDefault = new Button("Fijar por defecto");
        btnMakeDefault.setStyle(
                "-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-background-radius: 5; -fx-padding: 5 10;");
        btnMakeDefault.setDisable(pl.isDefault());
        btnMakeDefault.setOnAction(e -> makeDefault(pl));

        Button btnClone = new Button("Clonar");
        btnClone.setStyle(
                "-fx-background-color: #fdf2f8; -fx-text-fill: #9d174d; -fx-background-radius: 5; -fx-padding: 5 10;");
        btnClone.setOnAction(e -> handleClone(pl));

        Button btnViewTable = new Button("Ver Precios");
        btnViewTable.setStyle(
                "-fx-background-color: #ecfdf5; -fx-text-fill: #065f46; -fx-background-radius: 5; -fx-padding: 5 10;");
        btnViewTable.setOnAction(e -> openPriceTableView(pl));

        actionBox.getChildren().addAll(btnViewTable, btnClone, btnMakeDefault, btnEdit, btnDelete);

        card.getChildren().addAll(header, content, actionBox);
        return card;
    }

    private void handleClone(PriceList source) {
        String newName = AlertUtil.showInput("Clonar Tarifa", "Nombre de la nueva tarifa",
                "Clon de " + source.getName());
        if (newName != null && !newName.trim().isEmpty()) {
            String pctStr = AlertUtil.showInput("Ajuste de Precios",
                    "Porcentaje de ajuste (ej: 10 para +10%, -5 para -5%)", "0");
            double percentage = 0;
            try {
                if (pctStr != null)
                    percentage = Double.parseDouble(pctStr);
            } catch (NumberFormatException e) {
                AlertUtil.showError("Error", "El porcentaje debe ser un número válido.");
                return;
            }

            try {
                priceListUseCase.clone(source.getId(), newName.trim(), percentage);
                AlertUtil.showInfo("Éxito", "Tarifa clonada y precios ajustados correctamente.");
                loadPriceLists();
            } catch (SQLException e) {
                AlertUtil.showError("Error", "No se pudo clonar la tarifa: " + e.getMessage());
            }
        }
    }

    private void openPriceTableView(PriceList pl) {
        ModalService.showTransparentModal("/view/price_list_content.fxml",
                "Precios - " + pl.getName(),
                container,
                (com.mycompany.ventacontrolfx.presentation.controller.dialog.PriceListContentController c) -> c
                        .initData(pl));
    }

    @FXML
    private void openNewPriceListForm() {
        openPriceListForm(null);
    }

    private void openPriceListForm(PriceList priceList) {
        ModalService.showTransparentModal("/view/price_list_form.fxml",
                priceList == null ? "Nueva Tarifa" : "Editar Tarifa",
                container, (com.mycompany.ventacontrolfx.presentation.controller.dialog.PriceListFormController c) -> c
                        .initData(priceList));

        // Cuando se cierre el modal, recargar siempre por si hubo cambios
        loadPriceLists();
    }

    private void deletePriceList(PriceList pl) {
        if (pl.getId() == 1) {
            AlertUtil.showError("Error", "No se puede eliminar la tarifa principal.");
            return;
        }

        if (AlertUtil.showConfirmation("Confirmar", "Eliminar Tarifa",
                "¿Estás seguro de que deseas eliminar la tarifa '" + pl.getName() + "'?")) {
            try {
                priceListUseCase.delete(pl.getId());
                AlertUtil.showInfo("Tarifa eliminada", "La tarifa ha sido eliminada correctamente.");
                loadPriceLists();
            } catch (SQLException ex) {
                AlertUtil.showError("Error", "No se pudo eliminar la tarifa: " + ex.getMessage());
            }
        }
    }

    private void makeDefault(PriceList pl) {
        try {
            priceListUseCase.setAsDefault(pl.getId());
            AlertUtil.showInfo("Tarifa actualizada", "'" + pl.getName() + "' es ahora la tarifa por defecto.");
            loadPriceLists();
        } catch (SQLException ex) {
            AlertUtil.showError("Error", "No se pudo actualizar la tarifa: " + ex.getMessage());
        }
    }
}
