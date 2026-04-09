package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.PriceListUseCase;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.ModalService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
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
            Platform.runLater(() -> AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("price_list.error.load")));
        }
    }

    private void updateUI(List<PriceList> lists) {
        priceListsContainer.getChildren().clear();
        lblCount.setText(lists.size() + " " + container.getBundle().getString("price_lists.count_suffix"));

        for (PriceList pl : lists) {
            priceListsContainer.getChildren().add(createPriceListCard(pl));
        }
    }

    private VBox createPriceListCard(PriceList pl) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setPrefWidth(350);
        card.setStyle(
                "-fx-background-color: -fx-bg-surface; -fx-background-radius: 12; -fx-effect: -fx-card-shadow; -fx-cursor: hand;");

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
                Label lblPct = new Label(container.getBundle().getString("price_list.label.avg_diff") + ": " + pct);
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

        FlowPane actionBox = new FlowPane(8, 8);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = createIconButton(container.getBundle().getString("btn.edit"), FontAwesomeIcon.PENCIL,
                "#eff6ff", "#1e40af");
        btnEdit.setOnAction(e -> openPriceListForm(pl));

        Button btnDelete = createIconButton(container.getBundle().getString("btn.delete"), FontAwesomeIcon.TRASH,
                "#fef2f2", "#b91c1c");
        btnDelete.setDisable(pl.isDefault() || pl.getId() == 1);
        btnDelete.setOnAction(e -> deletePriceList(pl));

        Button btnMakeDefault = createIconButton(container.getBundle().getString("price_list.btn.set_default"),
                FontAwesomeIcon.STAR, "#f3f4f6", "#374151");
        btnMakeDefault.setDisable(pl.isDefault());
        btnMakeDefault.setOnAction(e -> makeDefault(pl));

        Button btnClone = createIconButton(container.getBundle().getString("btn.clone"), FontAwesomeIcon.COPY,
                "#fdf2f8", "#9d174d");
        btnClone.setOnAction(e -> handleClone(pl));

        Button btnViewTable = createIconButton(container.getBundle().getString("price_list.btn.view_prices"),
                FontAwesomeIcon.EYE, "#ecfdf5", "#065f46");
        btnViewTable.setOnAction(e -> openPriceTableView(pl));

        actionBox.getChildren().addAll(btnViewTable, btnClone, btnMakeDefault, btnEdit, btnDelete);

        card.getChildren().addAll(header, content, actionBox);
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

    private Button createIconButton(String text, FontAwesomeIcon iconEnum, String bgColor, String textColor) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor
                + "; -fx-background-radius: 6; -fx-padding: 6 12; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 12px;");

        FontAwesomeIconView icon = new FontAwesomeIconView(iconEnum);
        icon.setGlyphSize(14);
        icon.setFill(javafx.scene.paint.Color.valueOf(textColor));

        btn.setGraphic(icon);
        btn.setContentDisplay(ContentDisplay.LEFT);
        btn.setGraphicTextGap(8);

        return btn;
    }
}
