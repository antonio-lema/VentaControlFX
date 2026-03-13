package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.application.usecase.CategoryUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.component.ToggleSwitch;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.sql.SQLException;
import java.util.List;

public class CategoryController implements Injectable, com.mycompany.ventacontrolfx.util.Searchable {

    @FXML
    private TableView<Category> categoriesTable;
    @FXML
    private TableColumn<Category, String> colName, colTaxGroup;
    @FXML
    private TableColumn<Category, Double> colIva;
    @FXML
    private TableColumn<Category, Boolean> colVisible, colFavorite;
    @FXML
    private TableColumn<Category, Void> colActions;
    @FXML
    private TextField searchField;
    @FXML
    private Label lblCount;

    private CategoryUseCase categoryUseCase;
    private ServiceContainer container;
    private ObservableList<Category> categoryList = FXCollections.observableArrayList();

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.categoryUseCase = container.getCategoryUseCase();

        setupColumns();
        loadCategories();

        searchField.textProperty().addListener((obs, old, nv) -> filterCategories(nv));
    }

    @Override
    public void handleSearch(String text) {
        if (searchField != null) {
            searchField.setText(text);
        }
        filterCategories(text);
    }

    private void setupColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colTaxGroup.setCellValueFactory(new PropertyValueFactory<>("taxGroupName"));
        colIva.setCellValueFactory(new PropertyValueFactory<>("defaultIva"));

        setupToggleColumn(colVisible, "visible");
        setupToggleColumn(colFavorite, "favorite");
        setupActionColumn();
    }

    private void setupToggleColumn(TableColumn<Category, Boolean> col, String property) {
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setCellFactory(column -> new TableCell<>() {
            private final ToggleSwitch toggle = new ToggleSwitch();
            {
                toggle.setOnMouseClicked(e -> {
                    Category c = getTableRow().getItem();
                    if (c != null) {
                        try {
                            boolean newState = !toggle.isSwitchedOn();
                            toggle.setSwitchedOn(newState);
                            if ("favorite".equals(property)) {
                                c.setFavorite(newState);
                            } else {
                                c.setVisible(newState);
                            }
                            categoryUseCase.updateCategory(c);
                        } catch (SQLException ex) {
                            toggle.setSwitchedOn(!toggle.isSwitchedOn());
                            AlertUtil.showError("Error", "No se pudo actualizar el estado.");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null)
                    setGraphic(null);
                else {
                    toggle.setState(item);
                    setGraphic(toggle);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void setupActionColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final HBox pane = new HBox(8, btnEdit, btnDelete);
            {
                pane.setAlignment(Pos.CENTER);
                btnEdit.setGraphic(createIcon(FontAwesomeIcon.PENCIL, "#1e88e5"));
                FontAwesomeIconView trashIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
                trashIcon.setSize("16");
                btnDelete.setGraphic(trashIcon);
                btnDelete.getStyleClass().add("btn-trash-small");
                btnEdit.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

                btnEdit.setOnAction(e -> handleEditCategory(getTableRow().getItem()));
                btnDelete.setOnAction(e -> handleDeleteCategory(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private FontAwesomeIconView createIcon(FontAwesomeIcon icon, String color) {
        FontAwesomeIconView view = new FontAwesomeIconView(icon);
        view.setSize("16");
        view.setFill(Color.web(color));
        return view;
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryUseCase.getAll();
            categoryList.setAll(categories);
            filterCategories(searchField.getText());
        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudieron cargar las categorías.");
        }
    }

    private void filterCategories(String query) {
        String q = query == null ? "" : query.toLowerCase().trim();
        ObservableList<Category> filtered = categoryList.filtered(
                c -> q.isEmpty() || c.getName().toLowerCase().contains(q) || String.valueOf(c.getId()).contains(q));
        categoriesTable.setItems(filtered);
        if (lblCount != null)
            lblCount.setText(filtered.size() + " categorías");
    }

    @FXML
    private void handleAddCategory() {
        if (!container.getUserSession().hasPermission("producto.crear")) {
            AlertUtil.showError("Acceso Denegado", "No tiene permiso para crear categorías.");
            return;
        }
        openCategoryDialog(null);
    }

    private void handleEditCategory(Category category) {
        if (!container.getUserSession().hasPermission("producto.editar")) {
            AlertUtil.showError("Acceso Denegado", "No tiene permiso para editar categorías.");
            return;
        }
        openCategoryDialog(category);
    }

    private void openCategoryDialog(Category category) {
        ModalService.showTransparentModal("/view/add_category.fxml",
                category == null ? "Nueva Categoría" : "Editar Categoría", container,
                (AddCategoryController controller) -> {
                    controller.setCategory(category);
                });
        loadCategories();
    }

    private void handleDeleteCategory(Category category) {
        if (!container.getUserSession().hasPermission("producto.eliminar")) {
            AlertUtil.showError("Acceso Denegado", "No tiene permiso para eliminar categorías.");
            return;
        }
        if (AlertUtil.showConfirmation("Eliminar",
                "¿Seguro que desea eliminar la categoría '" + category.getName() + "'?", "")) {
            try {
                categoryUseCase.deleteCategory(category.getId());
                loadCategories();
            } catch (SQLException e) {
                AlertUtil.showError("Error", "No se pudo eliminar la categoría.");
            }
        }
    }
}
