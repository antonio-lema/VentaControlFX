package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.SuspendedCart;
import com.mycompany.ventacontrolfx.application.usecase.SuspendedCartUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class SuspendedCartsDialogController implements Injectable {

    @FXML
    private TableView<SuspendedCart> tblCarts;
    @FXML
    private TableColumn<SuspendedCart, String> colAlias;
    @FXML
    private TableColumn<SuspendedCart, String> colDate;
    @FXML
    private TableColumn<SuspendedCart, String> colClient;
    @FXML
    private TableColumn<SuspendedCart, Double> colTotal;
    @FXML
    private TableColumn<SuspendedCart, Void> colActions;

    private SuspendedCartUseCase suspendedCartUseCase;
    private ServiceContainer container;
    private Consumer<SuspendedCart> onCartSelected;
    private final ObservableList<SuspendedCart> cartList = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.suspendedCartUseCase = container.getSuspendedCartUseCase();
        setupTable();
        loadCarts();
        applyEntranceAnimation();
    }

    private void applyEntranceAnimation() {
        if (tblCarts.getParent() == null)
            return;

        javafx.scene.Node root = tblCarts.getParent();
        root.setOpacity(0);
        root.setScaleX(0.95);
        root.setScaleY(0.95);

        FadeTransition ft = new FadeTransition(Duration.millis(250), root);
        ft.setToValue(1.0);

        ScaleTransition st = new ScaleTransition(Duration.millis(300), root);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);

        ft.play();
        st.play();
    }

    private void setupTable() {
        colAlias.setCellValueFactory(new PropertyValueFactory<>("alias"));

        colDate.setCellValueFactory(
                new Callback<TableColumn.CellDataFeatures<SuspendedCart, String>, javafx.beans.value.ObservableValue<String>>() {
                    @Override
                    public javafx.beans.value.ObservableValue<String> call(
                            TableColumn.CellDataFeatures<SuspendedCart, String> cellData) {
                        return new javafx.beans.property.SimpleStringProperty(
                                cellData.getValue().getSuspendedAt().format(formatter));
                    }
                });

        colClient.setCellValueFactory(
                new Callback<TableColumn.CellDataFeatures<SuspendedCart, String>, javafx.beans.value.ObservableValue<String>>() {
                    @Override
                    public javafx.beans.value.ObservableValue<String> call(
                            TableColumn.CellDataFeatures<SuspendedCart, String> cellData) {
                        String name = cellData.getValue().getClientName();
                        return new javafx.beans.property.SimpleStringProperty(name != null ? name : "-");
                    }
                });

        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(new Callback<TableColumn<SuspendedCart, Double>, TableCell<SuspendedCart, Double>>() {
            @Override
            public TableCell<SuspendedCart, Double> call(TableColumn<SuspendedCart, Double> param) {
                return new TableCell<SuspendedCart, Double>() {
                    @Override
                    protected void updateItem(Double price, boolean empty) {
                        super.updateItem(price, empty);
                        if (empty || price == null) {
                            setText(null);
                        } else {
                            setText(String.format("%.2f €", price));
                        }
                    }
                };
            }
        });

        setupActionColumn();
        tblCarts.setItems(cartList);
    }

    private void setupActionColumn() {
        colActions.setCellFactory(new Callback<>() {
            @Override
            public TableCell<SuspendedCart, Void> call(final TableColumn<SuspendedCart, Void> param) {
                return new TableCell<>() {
                    private final Button btnDelete = new Button();
                    {
                        btnDelete.getStyleClass().addAll("btn-remove-line", "btn-trash-small");
                        FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
                        icon.setSize("14");
                        btnDelete.setGraphic(icon);
                        btnDelete.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {
                                SuspendedCart cart = getTableView().getItems().get(getIndex());
                                handleDelete(cart);
                            }
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btnDelete);
                        }
                    }
                };
            }
        });
    }

    public void setOnCartSelected(Consumer<SuspendedCart> onCartSelected) {
        this.onCartSelected = onCartSelected;
    }

    private void loadCarts() {
        try {
            // Podríamos filtrar por usuario actual, pero solemos mostrar todos para que un
            // admin ayude
            List<SuspendedCart> carts = suspendedCartUseCase.listAllSuspended();
            cartList.setAll(carts);
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "No se pudieron cargar los carritos: " + e.getMessage());
        }
    }

    @FXML
    private void handleResume() {
        SuspendedCart selected = tblCarts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Selección requerida", "Por favor, seleccione un carrito para recuperar.");
            return;
        }

        try {
            // Obtenemos el carrito completo con items
            SuspendedCart fullCart = suspendedCartUseCase.getById(selected.getId());
            if (onCartSelected != null) {
                onCartSelected.accept(fullCart);
            }
            handleClose();
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "No se pudo recuperar el carrito: " + e.getMessage());
        }
    }

    private void handleDelete(SuspendedCart cart) {
        if (AlertUtil.showConfirmation("Eliminar Carrito", "¿Seguro que desea eliminar esta venta aplazada?",
                cart.getAlias())) {
            try {
                suspendedCartUseCase.deleteCart(cart.getId());
                loadCarts();
            } catch (Exception e) {
                e.printStackTrace();
                AlertUtil.showError("Error", "No se pudo eliminar: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleClose() {
        javafx.scene.Node root = tblCarts.getParent();
        if (root == null) {
            if (tblCarts.getScene() != null && tblCarts.getScene().getWindow() != null) {
                ((Stage) tblCarts.getScene().getWindow()).close();
            }
            return;
        }

        FadeTransition ft = new FadeTransition(Duration.millis(200), root);
        ft.setToValue(0.0);
        ft.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                if (tblCarts.getScene() != null && tblCarts.getScene().getWindow() != null) {
                    ((Stage) tblCarts.getScene().getWindow()).close();
                }
            }
        });
        ft.play();
    }
}
