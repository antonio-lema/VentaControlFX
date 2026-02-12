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

import javafx.scene.control.Alert;
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
import java.util.Optional;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import com.mycompany.ventacontrolfx.control.ToggleSwitch;

public class CategoryController {

    @FXML
    private TableView<Category> categoriesTable;
    @FXML
    private TableColumn<Category, Integer> colId;
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

    private CategoryService categoryService;
    private ObservableList<Category> categoryList;

    public void initialize() {
        categoryService = new CategoryService();
        categoryList = FXCollections.observableArrayList();

        setupColumns();
        loadCategories();

        // Search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterCategories(newValue);
        });
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
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
                        // Revert if failed
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
                        // Revert if failed
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
                }
            }
        });

        // Actions Column
        colActions.setCellFactory(param -> new TableCell<Category, Void>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final HBox pane = new HBox(5, btnEdit, btnDelete);

            {
                pane.setAlignment(Pos.CENTER);

                // Edit Icon (Pencil)
                SVGPath editIcon = new SVGPath();
                editIcon.setContent(
                        "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z");
                editIcon.getStyleClass().add("svg-path");

                btnEdit.setGraphic(editIcon);
                btnEdit.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                btnEdit.getStyleClass().addAll("btn-icon", "btn-edit");
                btnEdit.setTooltip(new javafx.scene.control.Tooltip("Editar Categoría"));

                // Delete Icon (Trash)
                SVGPath deleteIcon = new SVGPath();
                deleteIcon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
                deleteIcon.getStyleClass().add("svg-path");

                btnDelete.setGraphic(deleteIcon);
                btnDelete.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                btnDelete.getStyleClass().addAll("btn-icon", "btn-delete");
                btnDelete.setTooltip(new javafx.scene.control.Tooltip("Eliminar Categoría"));

                btnEdit.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    handleEditCategory(category);
                });

                btnDelete.setOnAction(event -> {
                    Category category = getTableView().getItems().get(getIndex());
                    handleDeleteCategory(category);
                });
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
            categoriesTable.setItems(categoryList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudieron cargar las categorías: " + e.getMessage());
        }
    }

    private void filterCategories(String query) {
        if (query == null || query.isEmpty()) {
            categoriesTable.setItems(categoryList);
        } else {
            ObservableList<Category> filtered = FXCollections.observableArrayList();
            for (Category c : categoryList) {
                if (c.getName().toLowerCase().contains(query.toLowerCase())) {
                    filtered.add(c);
                }
            }
            categoriesTable.setItems(filtered);
        }
    }

    @FXML
    private void handleAddCategory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_category.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Añadir Categoría");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            stage.showAndWait();

            // Refresh table
            loadCategories();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir el diálogo: " + e.getMessage());
        }
    }

    private void handleEditCategory(Category category) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_category.fxml"));
            Parent root = loader.load();

            // Access controller to pass data
            AddCategoryController controller = loader.getController();
            controller.setCategory(category);

            Stage stage = new Stage();
            stage.setTitle("Editar Categoría");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            loadCategories();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir el diálogo de edición: " + e.getMessage());
        }
    }

    private void handleDeleteCategory(Category category) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación");
        alert.setHeaderText("Eliminar Categoría");
        alert.setContentText("¿Está seguro de que desea eliminar la categoría '" + category.getName() + "'?");

        // Add styling to dialog
        javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-alert");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
