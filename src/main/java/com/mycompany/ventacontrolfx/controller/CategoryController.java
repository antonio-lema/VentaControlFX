package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.Category;
import com.mycompany.ventacontrolfx.service.CategoryService;
import java.sql.SQLException;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.shape.SVGPath;
import javafx.scene.control.ContentDisplay;

import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Modality;
import java.io.IOException;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.control.TableCell;
import com.mycompany.ventacontrolfx.control.ToggleSwitch;

public class CategoryController implements com.mycompany.ventacontrolfx.util.Injectable {

    @FXML
    private TableView<Category> categoriesTable;
    @FXML
    private TableColumn<Category, String> colName;
    @FXML
    private TableColumn<Category, Boolean> colVisible;
    @FXML
    private TableColumn<Category, Boolean> colFavorite;
    @FXML
    private TableColumn<Category, Void> colActions;
    @FXML
    private TextField searchField;
    @FXML
    private Button btnAdd;
    @FXML
    private TextField rowsPerPageField;

    private CategoryService categoryService;
    private ObservableList<Category> categoryList;
    private com.mycompany.ventacontrolfx.service.ServiceContainer container;

    @Override
    public void inject(com.mycompany.ventacontrolfx.service.ServiceContainer container) {
        this.container = container;
        this.categoryService = container.getCategoryService();
        this.categoryList = FXCollections.observableArrayList();

        setupColumns();
        loadCategories();

        // Search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterCategories(newValue, rowsPerPageField.getText());
        });

        // Rows per page functionality
        rowsPerPageField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterCategories(searchField.getText(), newValue);
        });
    }

    public void initialize() {
        // Initialization handled in inject()
    }

    private void setupColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        // Visible Column
        colVisible.setCellValueFactory(new PropertyValueFactory<>("visible"));
        colVisible.setCellFactory(column -> new TableCell<Category, Boolean>() {
            private final ToggleSwitch toggle = new ToggleSwitch();

            {
                toggle.setOnMouseClicked(event -> {
                    boolean newState = !toggle.isSwitchedOn();
                    toggle.setSwitchedOn(newState);

                    Category category = getTableView().getItems().get(getIndex());
                    category.setVisible(newState);
                    try {
                        categoryService.updateCategory(category);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        toggle.setSwitchedOn(!newState);
                        category.setVisible(!newState);
                        showAlert("Error", "No se pudo actualizar la visibilidad: " + e.getMessage());
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    toggle.setState(item);
                    setGraphic(toggle);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Favorite Column
        colFavorite.setCellValueFactory(new PropertyValueFactory<>("favorite"));
        colFavorite.setCellFactory(column -> new TableCell<Category, Boolean>() {
            private final ToggleSwitch toggle = new ToggleSwitch();

            {
                toggle.setOnMouseClicked(event -> {
                    boolean newState = !toggle.isSwitchedOn();
                    toggle.setSwitchedOn(newState);

                    Category category = getTableView().getItems().get(getIndex());
                    category.setFavorite(newState);
                    try {
                        categoryService.updateCategory(category);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        toggle.setSwitchedOn(!newState);
                        category.setFavorite(!newState);
                        showAlert("Error", "No se pudo actualizar favorito: " + e.getMessage());
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    toggle.setState(item);
                    setGraphic(toggle);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Actions Column
        colActions.setCellFactory(param -> new TableCell<Category, Void>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final HBox pane = new HBox(8, btnEdit, btnDelete);

            {
                pane.setAlignment(Pos.CENTER);

                // Edit Icon
                de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView editIcon = new de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView(
                        de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PENCIL);
                editIcon.setSize("16");
                editIcon.setFill(javafx.scene.paint.Color.web("#1e88e5"));

                btnEdit.setGraphic(editIcon);
                btnEdit.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                btnEdit.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5;");
                btnEdit.setTooltip(new javafx.scene.control.Tooltip("Editar Categoría"));

                // Delete Icon
                de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView deleteIcon = new de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView(
                        de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TRASH);
                deleteIcon.setSize("16");
                deleteIcon.setFill(javafx.scene.paint.Color.web("#e53935"));

                btnDelete.setGraphic(deleteIcon);
                btnDelete.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                btnDelete.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5;");
                btnDelete.setTooltip(new javafx.scene.control.Tooltip("Eliminar Categoría"));

                btnEdit.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    handleEditCategory(category);
                });

                btnDelete.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    handleDeleteCategory(category);
                });

                // Hover effects
                btnEdit.setOnMouseEntered(e -> editIcon.setFill(javafx.scene.paint.Color.web("#1565c0")));
                btnEdit.setOnMouseExited(e -> editIcon.setFill(javafx.scene.paint.Color.web("#1e88e5")));
                btnDelete.setOnMouseEntered(e -> deleteIcon.setFill(javafx.scene.paint.Color.web("#c62828")));
                btnDelete.setOnMouseExited(e -> deleteIcon.setFill(javafx.scene.paint.Color.web("#e53935")));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryService.getAllCategories();
            categoryList.setAll(categories);

            // Initial filter to respect default limit
            filterCategories(searchField.getText(), rowsPerPageField.getText());

            // Note: StatusBar update is now handled via GlobalEventBus if applicable
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudieron cargar las categorías: " + e.getMessage());
        }
    }

    private void filterCategories(String query, String limitStr) {
        int limit = Integer.MAX_VALUE;
        try {
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                limit = Integer.parseInt(limitStr.trim());
            }
        } catch (NumberFormatException e) {
            // Ignore invalid input, use default (all)
        }

        ObservableList<Category> filtered = FXCollections.observableArrayList();
        String lowerCaseQuery = (query != null) ? query.toLowerCase() : "";

        for (Category c : categoryList) {
            if (filtered.size() >= limit) {
                break;
            }

            boolean matches = false;
            // Search by Name or ID
            if (lowerCaseQuery.isEmpty()) {
                matches = true;
            } else {
                if (c.getName().toLowerCase().contains(lowerCaseQuery)) {
                    matches = true;
                } else if (String.valueOf(c.getId()).contains(lowerCaseQuery)) {
                    matches = true;
                }
            }

            if (matches) {
                filtered.add(c);
            }
        }
        categoriesTable.setItems(filtered);
    }

    @FXML
    private void handleAddCategory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_category.fxml"));
            Parent root = loader.load();

            AddCategoryController controller = loader.getController();
            controller.inject(container); // CRÍTICO: inyectar para que categoryService no sea null

            Stage stage = new Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            scene.setFill(null);
            scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());

            stage.setScene(scene);
            stage.showAndWait();
            loadCategories();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir la ventana de añadir categoría: " + e.getMessage());
        }
    }

    private void handleEditCategory(Category category) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_category.fxml"));
            Parent root = loader.load();

            AddCategoryController controller = loader.getController();
            controller.inject(container); // CRÍTICO: inyectar antes de setCategory()
            controller.setCategory(category);

            Stage stage = new Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            scene.setFill(null);
            scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());

            stage.setScene(scene);
            stage.showAndWait();
            loadCategories();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir la ventana de editar categoría: " + e.getMessage());
        }
    }

    private void handleDeleteCategory(Category category) {
        boolean confirmed = AlertUtil.showConfirmation("Confirmar Eliminación",
                "¿Está seguro de que desea eliminar la categoría '" + category.getName() + "'?",
                "Esta acción no se puede deshacer.");
        if (confirmed) {
            try {
                categoryService.deleteCategory(category.getId());
                loadCategories();
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "No se pudo eliminar la categoría: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String content) {
        AlertUtil.showInfo(title, content);
    }
}
