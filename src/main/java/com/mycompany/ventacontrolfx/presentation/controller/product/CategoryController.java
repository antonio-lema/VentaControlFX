package com.mycompany.ventacontrolfx.presentation.controller.product;

import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.application.usecase.CategoryUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import com.mycompany.ventacontrolfx.presentation.navigation.ModalService;
import com.mycompany.ventacontrolfx.presentation.component.ToggleSwitch;
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

public class CategoryController implements Injectable, com.mycompany.ventacontrolfx.presentation.util.Searchable {

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
    private ComboBox<Integer> cmbRowLimit;
    @FXML
    private Label lblCount;
    @FXML
    private Pagination pagination;

    private CategoryUseCase categoryUseCase;
    private ServiceContainer container;
    private com.mycompany.ventacontrolfx.shared.util.ServerPaginationHelper<Category> paginationHelper;
    private String currentSearchQuery = "";

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.categoryUseCase = container.getCategoryUseCase();

        setupColumns();
        
        paginationHelper = new com.mycompany.ventacontrolfx.shared.util.ServerPaginationHelper<>(
                categoriesTable, cmbRowLimit, lblCount, pagination,
                container.getBundle().getString("categories.entity_plural"),
                this::fetchCategoriesPage, container.getBundle());

        searchField.textProperty().addListener((obs, old, nv) -> handleSearch(nv));
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
                            AlertUtil.showError(container.getBundle().getString("alert.error"),
                                    container.getBundle().getString("category.error.update_status"));
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

                // Botón Editar Circular
                btnEdit.getStyleClass().add("btn-action-edit");
                FontAwesomeIconView editIcon = new FontAwesomeIconView(FontAwesomeIcon.PENCIL);
                editIcon.setSize("16");
                btnEdit.setGraphic(editIcon);

                // Botón Eliminar Circular (Premium)
                btnDelete.getStyleClass().add("btn-action-delete");
                FontAwesomeIconView trashIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH_ALT);
                trashIcon.setSize("16");
                btnDelete.setGraphic(trashIcon);

                btnEdit.setOnAction(e -> handleEditCategory(getTableRow().getItem()));
                btnDelete.setOnAction(e -> handleDeleteCategory(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    btnEdit.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PENCIL, "16"));
                    btnDelete.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH, "16"));
                    setGraphic(pane);
                }
            }
        });
    }

    private FontAwesomeIconView createIcon(FontAwesomeIcon icon, String color) {
        FontAwesomeIconView view = new FontAwesomeIconView(icon);
        view.setSize("16");
        view.setFill(Color.web(color));
        return view;
    }

    private void fetchCategoriesPage(int offset, int limit) {
        try {
            int total = categoryUseCase.getCount(currentSearchQuery);
            List<Category> items = categoryUseCase.getPaginated(currentSearchQuery, limit, offset);
            paginationHelper.applyDataTarget(items, total);
        } catch (SQLException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("category.error.load"));
        }
    }

    @Override
    public void handleSearch(String query) {
        this.currentSearchQuery = query == null ? "" : query.trim();
        paginationHelper.refresh();
    }

    @FXML
    private void handleAddCategory() {
        if (!container.getUserSession().hasPermission("producto.crear")) {
            AlertUtil.showError(container.getBundle().getString("alert.access_denied"),
                    container.getBundle().getString("error.no_permission"));
            return;
        }
        openCategoryDialog(null);
    }

    private void handleEditCategory(Category category) {
        if (!container.getUserSession().hasPermission("producto.editar")) {
            AlertUtil.showError(container.getBundle().getString("alert.access_denied"),
                    container.getBundle().getString("error.no_permission"));
            return;
        }
        openCategoryDialog(category);
    }

    private void openCategoryDialog(Category category) {
        ModalService.showTransparentModal("/view/product/add_category.fxml",
                category == null ? container.getBundle().getString("category.dialog.new")
                        : container.getBundle().getString("category.dialog.edit"),
                container,
                (AddCategoryController controller) -> {
                    controller.setCategory(category);
                });
        paginationHelper.refresh();
    }

    private void handleDeleteCategory(Category category) {
        if (!container.getUserSession().hasPermission("producto.eliminar")) {
            AlertUtil.showError(container.getBundle().getString("alert.access_denied"),
                    container.getBundle().getString("error.no_permission"));
            return;
        }
        if (AlertUtil.showConfirmation(container.getBundle().getString("btn.delete"),
                container.getBundle().getString("category.confirm.delete") + " '" + category.getName() + "'?", "")) {
            try {
                categoryUseCase.deleteCategory(category.getId());
                paginationHelper.refresh();
            } catch (SQLException e) {
                AlertUtil.showError(container.getBundle().getString("alert.error"),
                        container.getBundle().getString("category.error.delete"));
            }
        }
    }
}





