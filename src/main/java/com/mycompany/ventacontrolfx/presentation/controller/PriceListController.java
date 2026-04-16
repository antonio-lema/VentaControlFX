package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.PriceListUseCase;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.util.Searchable;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class PriceListController implements Injectable, Searchable {

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

    @Override
    public void handleSearch(String text) {
        loadPriceLists(text);
    }

    private void loadPriceLists() {
        loadPriceLists("");
    }

    private void loadPriceLists(String filter) {
        try {
            List<PriceList> lists = priceListUseCase.getAll();
            Platform.runLater(() -> {
                updateUI(lists, filter);
            });
        } catch (SQLException e) {
            Platform.runLater(() -> AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("price_list.error.load")));
        }
    }

    private void updateUI(List<PriceList> lists, String filter) {
        priceListsContainer.getChildren().clear();

        long count = lists.stream()
                .filter(pl -> filter == null || filter.isEmpty()
                        || pl.getName().toLowerCase().contains(filter.toLowerCase()))
                .count();

        lblCount.setText(count + " " + container.getBundle().getString("price_lists.count_suffix"));

        for (PriceList pl : lists) {
            if (filter != null && !filter.isEmpty() &&
                    !pl.getName().toLowerCase().contains(filter.toLowerCase())) {
                continue;
            }
            try {
                priceListsContainer.getChildren().add(createPriceListCard(pl));
            } catch (Exception e) {
                System.err.println("Error creating card for product list " + pl.getName() + ": " + e.getMessage());
            }
        }
    }

    private VBox createPriceListCard(PriceList pl) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: -fx-bg-surface; -fx-background-radius: 12; -fx-effect: -fx-card-shadow; -fx-cursor: hand;");
        card.setMinHeight(220); // Altura mínima uniforme
        card.setPrefWidth(350);

        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                openPriceTableView(pl);
            }
        });

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblName = new Label(pl.getName());
        lblName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -fx-text-custom-main;");

        header.getChildren().add(lblName);

        if (pl.isDefault()) {
            Label lblDefault = new Label(container.getBundle().getString("price_list.status.default"));
            lblDefault.setStyle(
                    "-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-padding: 4 8; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            header.getChildren().add(lblDefault);
        }

        if (!pl.isActive()) {
            Label lblInactive = new Label(container.getBundle().getString("price_list.status.inactive"));
            lblInactive.setStyle(
                    "-fx-background-color: #f3f4f6; -fx-text-fill: #4b5563; -fx-padding: 4 8; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            header.getChildren().add(lblInactive);
        }

        Region r = new Region();
        HBox.setHgrow(r, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().add(r);

        VBox content = new VBox(8);
        Label lblInfo = new Label(container.getBundle().getString("price_list.label.id") + ": " + pl.getId());
        lblInfo.setStyle("-fx-text-fill: -fx-text-custom-muted; -fx-font-size: 13px;");
        content.getChildren().add(lblInfo);

        if (pl.getDescription() != null && !pl.getDescription().isBlank()) {
            Label lblDesc = new Label(pl.getDescription());
            lblDesc.setWrapText(true);
            lblDesc.setStyle("-fx-text-fill: -fx-text-custom-medium; -fx-font-size: 14px; -fx-font-style: italic;");
            content.getChildren().add(lblDesc);
        }

        if (!pl.isDefault()) {
            try {
                String pct = priceListUseCase.getAveragePercentageDifference(pl.getId());
                // Evitar redundancia si el porcentaje ya incluye la palabra "media"
                String cleanPct = pct.replace(" media", "").replace(" Media", "");
                Label lblPct = new Label(
                        container.getBundle().getString("price_list.label.avg_diff") + ": " + cleanPct);

                if (pct.startsWith("+")) {
                    lblPct.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 13px;");
                } else if (pct.startsWith("-")) {
                    lblPct.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 13px;");
                } else {
                    lblPct.setStyle(
                            "-fx-text-fill: -fx-text-custom-muted; -fx-font-weight: bold; -fx-font-size: 13px;");
                }
                content.getChildren().add(lblPct);
            } catch (SQLException ignore) {
            }
        }

        HBox actionBox = new HBox(12);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.setPadding(new Insets(5, 0, 0, 0));

        // Ver Precios (Ojo)
        Button btnViewTable = new Button();
        btnViewTable.getStyleClass().add("btn-table-icon");
        FontAwesomeIconView viewIcon = new FontAwesomeIconView(FontAwesomeIcon.EYE);
        viewIcon.setSize("18");
        viewIcon.setFill(javafx.scene.paint.Color.valueOf("#2196F3")); // Azul Primario
        btnViewTable.setGraphic(viewIcon);
        btnViewTable.setOnAction(e -> openPriceTableView(pl));
        Tooltip.install(btnViewTable, new Tooltip(container.getBundle().getString("price_list.btn.view_prices")));

        // Clonar (Copy)
        Button btnClone = new Button();
        btnClone.getStyleClass().add("btn-table-icon");
        FontAwesomeIconView cloneIcon = new FontAwesomeIconView(FontAwesomeIcon.COPY);
        cloneIcon.setSize("18");
        cloneIcon.setFill(javafx.scene.paint.Color.valueOf("#10b981")); // Verde esmeralda
        btnClone.setGraphic(cloneIcon);
        btnClone.setOnAction(e -> handleClone(pl));
        Tooltip.install(btnClone, new Tooltip(container.getBundle().getString("price_list.dialog.clone")));

        // Establecer como Predeterminada (Estrella)
        Button btnMakeDefault = new Button();
        btnMakeDefault.getStyleClass().add("btn-table-icon");
        FontAwesomeIconView starIcon = new FontAwesomeIconView(FontAwesomeIcon.STAR);
        starIcon.setSize("18");
        starIcon.setFill(javafx.scene.paint.Color.valueOf("#f59e0b")); // Ámbar/Amarillo
        btnMakeDefault.setGraphic(starIcon);
        btnMakeDefault.setVisible(!pl.isDefault());
        btnMakeDefault.setManaged(!pl.isDefault());
        btnMakeDefault.setOnAction(e -> makeDefault(pl));
        Tooltip.install(btnMakeDefault, new Tooltip(container.getBundle().getString("price_list.status.default")));

        // Editar (Lápiz)
        Button btnEdit = new Button();
        btnEdit.getStyleClass().add("btn-table-icon");
        FontAwesomeIconView editIcon = new FontAwesomeIconView(FontAwesomeIcon.PENCIL);
        editIcon.setSize("18");
        editIcon.setFill(javafx.scene.paint.Color.valueOf("#6366f1")); // Índigo/Púrpura
        btnEdit.setGraphic(editIcon);
        btnEdit.setOnAction(e -> openPriceListForm(pl));
        Tooltip.install(btnEdit, new Tooltip(container.getBundle().getString("price_list.dialog.edit")));

        // Eliminar (Papelera)
        Button btnDelete = new Button();
        btnDelete.getStyleClass().add("btn-table-icon-danger");
        FontAwesomeIconView trashIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
        trashIcon.setSize("18");
        trashIcon.setFill(javafx.scene.paint.Color.valueOf("#ef4444")); // Rojo
        btnDelete.setGraphic(trashIcon);
        btnDelete.setDisable(pl.isDefault() || pl.getId() == 1);
        btnDelete.setVisible(pl.getId() != 1); // No mostrar borrar en la principal
        btnDelete.setManaged(pl.getId() != 1);
        btnDelete.setOnAction(e -> deletePriceList(pl));
        Tooltip.install(btnDelete, new Tooltip(container.getBundle().getString("btn.delete")));

        actionBox.getChildren().addAll(btnViewTable, btnClone, btnMakeDefault, btnEdit, btnDelete);

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        card.getChildren().addAll(header, content, spacer, actionBox);
        return card;
    }

    private void handleClone(PriceList source) {
        com.mycompany.ventacontrolfx.presentation.controller.dialog.PriceListCloneController ctrl = ModalService
                .showTransparentModal("/view/dialog/price_list_clone_dialog.fxml",
                        container.getBundle().getString("price_list.dialog.clone"), container,
                        (com.mycompany.ventacontrolfx.presentation.controller.dialog.PriceListCloneController c) -> {
                            c.initData(container.getBundle().getString("price_list.clone_prefix") + " "
                                    + source.getName());
                        });

        if (ctrl != null && ctrl.isConfirmed()) {
            try {
                priceListUseCase.clone(source.getId(), ctrl.getName(), ctrl.getPercentage());
                AlertUtil.showInfo(container.getBundle().getString("alert.success"),
                        container.getBundle().getString("price_list.success.cloned"));
                loadPriceLists();
            } catch (SQLException e) {
                AlertUtil.showError(container.getBundle().getString("alert.error"),
                        container.getBundle().getString("price_list.error.clone") + ": " + e.getMessage());
            }
        }
    }

    private void openPriceTableView(PriceList pl) {
        container.getNavigationService().navigateTo("/view/price_list_content.fxml",
                (com.mycompany.ventacontrolfx.presentation.controller.dialog.PriceListContentController c) -> c
                        .initData(pl));
    }

    @FXML
    private void openNewPriceListForm() {
        openPriceListForm(null);
    }

    private void openPriceListForm(PriceList priceList) {
        ModalService.showTransparentModal("/view/price_list_form.fxml",
                priceList == null ? container.getBundle().getString("price_list.dialog.new")
                        : container.getBundle().getString("price_list.dialog.edit"),
                container, (com.mycompany.ventacontrolfx.presentation.controller.dialog.PriceListFormController c) -> c
                        .initData(priceList));

        // Cuando se cierre el modal, recargar siempre por si hubo cambios
        loadPriceLists();
    }

    private void deletePriceList(PriceList pl) {
        if (pl.getId() == 1) {
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("price_list.error.delete_main"));
            return;
        }

        if (AlertUtil.showConfirmation(container.getBundle().getString("alert.confirm"),
                container.getBundle().getString("price_list.confirm.delete_title"),
                container.getBundle().getString("price_list.confirm.delete_msg") + " '" + pl.getName() + "'?")) {
            try {
                priceListUseCase.delete(pl.getId());
                AlertUtil.showInfo(container.getBundle().getString("price_list.success.deleted_title"),
                        container.getBundle().getString("price_list.success.deleted_msg"));
                loadPriceLists();
            } catch (SQLException ex) {
                AlertUtil.showError(container.getBundle().getString("alert.error"),
                        container.getBundle().getString("price_list.error.delete") + ": " + ex.getMessage());
            }
        }
    }

    private void makeDefault(PriceList pl) {
        try {
            priceListUseCase.setAsDefault(pl.getId());
            AlertUtil.showInfo(container.getBundle().getString("price_list.success.default_title"),
                    "'" + pl.getName() + "' " + container.getBundle().getString("price_list.success.default_msg"));
            loadPriceLists();
        } catch (SQLException ex) {
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("price_list.error.update") + ": " + ex.getMessage());
        }
    }
}
