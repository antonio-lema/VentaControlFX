package com.mycompany.ventacontrolfx.presentation.controller.cart;

import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import com.mycompany.ventacontrolfx.presentation.navigation.ModalService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Gestor de la Interfaz de Usuario para el Carrito.
 * Centraliza la creación de diálogos y popups complejos.
 */
public class CartUIManager {

    private final ServiceContainer container;

    public CartUIManager(ServiceContainer container) {
        this.container = container;
    }

    public void showPriceListSelector(int currentId, Consumer<com.mycompany.ventacontrolfx.domain.model.PriceList> onSelected) {
        try {
            List<com.mycompany.ventacontrolfx.domain.model.PriceList> lists = container.getPriceListUseCase().getAll();
            if (lists.isEmpty()) return;

            com.mycompany.ventacontrolfx.domain.model.PriceList current = lists.stream()
                    .filter(l -> l.getId() == currentId)
                    .findFirst()
                    .orElse(lists.get(0));

            VBox root = createModalRoot(420);
            
            // Header
            VBox header = createHeader(FontAwesomeIcon.TAGS, "#2196f3", 
                    container.getBundle().getString("cart.price_list.change.title"),
                    container.getBundle().getString("cart.price_list.change.subtitle"));

            // Content
            VBox content = new VBox(8);
            content.setAlignment(Pos.CENTER_LEFT);
            Label lblHint = new Label(container.getBundle().getString("cart.price_list.change.label"));
            lblHint.setStyle("-fx-font-weight: bold; -fx-text-fill: #555555;");

            ComboBox<com.mycompany.ventacontrolfx.domain.model.PriceList> cmb = new ComboBox<>(
                    javafx.collections.FXCollections.observableArrayList(lists));
            cmb.getSelectionModel().select(current);
            cmb.getStyleClass().add("input-field-modern");
            cmb.setMaxWidth(Double.MAX_VALUE);
            cmb.setPrefHeight(45);
            
            // Cell rendering
            cmb.setCellFactory(lv -> new ListCell<com.mycompany.ventacontrolfx.domain.model.PriceList>() {
                @Override protected void updateItem(com.mycompany.ventacontrolfx.domain.model.PriceList item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });
            cmb.setButtonCell(cmb.getCellFactory().call(null));

            content.getChildren().addAll(lblHint, cmb);

            // Footer
            Stage stage = createStage(root);
            HBox footer = createFooter(stage, container.getBundle().getString("cart.price_list.change.apply"), e -> {
                onSelected.accept(cmb.getSelectionModel().getSelectedItem());
                stage.close();
            });

            root.getChildren().addAll(header, content, footer);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showSuspendDialog(Consumer<String> onConfirm) {
        VBox root = createModalRoot(420);
        VBox header = createHeader(FontAwesomeIcon.PAUSE_CIRCLE, "#fb8c00",
                container.getBundle().getString("cart.suspend.title"),
                container.getBundle().getString("cart.suspend.subtitle"));

        VBox content = new VBox(8);
        content.setAlignment(Pos.CENTER_LEFT);
        Label lblHint = new Label(container.getBundle().getString("cart.suspend.alias_label"));
        lblHint.setStyle("-fx-font-weight: bold; -fx-text-fill: #555555;");

        TextField txtAlias = new TextField(container.getBundle().getString("cart.suspend.alias_default")
                + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        txtAlias.getStyleClass().add("input-field-modern");
        txtAlias.setPrefHeight(45);
        content.getChildren().addAll(lblHint, txtAlias);

        Stage stage = createStage(root);
        HBox footer = createFooter(stage, container.getBundle().getString("cart.suspend.apply"), e -> {
            String alias = txtAlias.getText().trim();
            if (!alias.isEmpty()) {
                onConfirm.accept(alias);
                stage.close();
            }
        });

        root.getChildren().addAll(header, content, footer);
        stage.showAndWait();
    }

    public void showManualItemDialog(BiConsumer<String, Double> onConfirm) {
        VBox root = createModalRoot(400);
        VBox header = createHeader(FontAwesomeIcon.PLUS_SQUARE, "#4caf50",
                container.getBundle().getString("cart.manual_item.title"), "");

        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER_LEFT);

        TextField txtName = new TextField();
        txtName.setPromptText(container.getBundle().getString("cart.manual_item.name_prompt"));
        txtName.getStyleClass().add("input-field-modern");

        TextField txtPrice = new TextField();
        txtPrice.setPromptText("0.00");
        txtPrice.getStyleClass().add("input-field-modern");
        txtPrice.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) txtPrice.setText(oldVal);
        });

        content.getChildren().addAll(new Label(container.getBundle().getString("cart.manual_item.name_label")), 
                txtName, new Label(container.getBundle().getString("cart.manual_item.price_label")), txtPrice);

        Stage stage = createStage(root);
        HBox footer = createFooter(stage, container.getBundle().getString("cart.manual_item.apply"), e -> {
            String name = txtName.getText().trim();
            if (!name.isEmpty()) {
                double price = txtPrice.getText().isEmpty() ? 0.0 : Double.parseDouble(txtPrice.getText());
                onConfirm.accept(name, price);
                stage.close();
            }
        });

        root.getChildren().addAll(header, content, footer);
        stage.showAndWait();
    }

    public void showCashNotOpenAlert(String message) {
        VBox root = createModalRoot(450);
        
        // Icono de advertencia premium
        FontAwesomeIconView iconView = new FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_CIRCLE);
        iconView.setSize("60px");
        iconView.setFill(Color.valueOf("#fb8c00"));

        VBox textContent = new VBox(10);
        textContent.setAlignment(Pos.CENTER);

        Label title = new Label(container.getBundle().getString("cart.cash_closed.title"));
        title.getStyleClass().add("modal-title");

        Label content = new Label(message);
        content.getStyleClass().add("modal-subtitle");
        content.setWrapText(true);
        content.setMaxWidth(380);
        content.setStyle("-fx-text-alignment: center;");

        textContent.getChildren().addAll(title, content);

        Stage stage = createStage(root);
        Button btnClose = new Button(container.getBundle().getString("btn.close").toUpperCase());
        btnClose.getStyleClass().add("btn-primary");
        btnClose.setPrefWidth(180);
        btnClose.setPrefHeight(45);
        btnClose.setOnAction(e -> stage.close());

        root.getChildren().addAll(iconView, textContent, btnClose);
        stage.showAndWait();
    }

    public void showObservationDialog(String currentObs, Consumer<String> onConfirm) {
        VBox root = createModalRoot(400);
        VBox header = createHeader(FontAwesomeIcon.COMMENTING, "#2196f3",
                container.getBundle().getString("cart.observation.title"), "");

        TextArea txtObs = new TextArea(currentObs);
        txtObs.setPromptText(container.getBundle().getString("cart.observation.prompt"));
        txtObs.getStyleClass().add("text-area-modern");
        txtObs.setPrefHeight(100);
        txtObs.setWrapText(true);

        Stage stage = createStage(root);
        HBox footer = createFooter(stage, container.getBundle().getString("cart.observation.apply"), e -> {
            onConfirm.accept(txtObs.getText().trim());
            stage.close();
        });

        root.getChildren().addAll(header, txtObs, footer);
        stage.showAndWait();
    }

    // --- Helpers Privados para evitar repetición de código ---

    private VBox createModalRoot(double width) {
        VBox root = new VBox(20);
        root.getStyleClass().add("modal-container");
        root.setPrefWidth(width);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        return root;
    }

    private VBox createHeader(FontAwesomeIcon iconName, String color, String titleStr, String subtitleStr) {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        FontAwesomeIconView icon = new FontAwesomeIconView(iconName);
        icon.setSize("45px");
        icon.setFill(Color.valueOf(color));
        Label title = new Label(titleStr);
        title.getStyleClass().add("custom-modal-title");
        header.getChildren().addAll(icon, title);
        if (!subtitleStr.isEmpty()) {
            Label subtitle = new Label(subtitleStr);
            subtitle.setStyle("-fx-text-fill: #999999; -fx-font-size: 14px;");
            header.getChildren().add(subtitle);
        }
        return header;
    }

    private HBox createFooter(Stage stage, String confirmText, javafx.event.EventHandler<javafx.event.ActionEvent> onConfirm) {
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER);
        Button btnCancel = new Button(container.getBundle().getString("btn.cancel").toUpperCase());
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setOnAction(e -> stage.close());

        Button btnConfirm = new Button(confirmText);
        btnConfirm.getStyleClass().add("btn-primary");
        btnConfirm.setOnAction(onConfirm);

        footer.getChildren().addAll(btnCancel, btnConfirm);
        return footer;
    }

    private Stage createStage(VBox root) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        container.getThemeManager().applyFullTheme(scene);
        stage.setScene(scene);

        root.setOpacity(0);
        root.setScaleX(0.9);
        root.setScaleY(0.9);
        stage.setOnShowing(evt -> {
            FadeTransition ft = new FadeTransition(Duration.millis(250), root);
            ft.setToValue(1.0);
            ScaleTransition st = new ScaleTransition(Duration.millis(250), root);
            st.setToX(1.0); st.setToY(1.0);
            ft.play(); st.play();
        });
        return stage;
    }
}


