package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Promotion;
import com.mycompany.ventacontrolfx.domain.model.PromotionScope;
import com.mycompany.ventacontrolfx.domain.model.PromotionType;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import javafx.util.StringConverter;
import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.application.usecase.ProductUseCase;
import com.mycompany.ventacontrolfx.application.usecase.CategoryUseCase;

public class PromotionFormController implements Injectable {

    @FXML
    private Label lblTitle;
    @FXML
    private TextField txtName;
    @FXML
    private ComboBox<PromotionType> cmbType;
    @FXML
    private TextField txtValue;
    @FXML
    private VBox panelValue;
    @FXML
    private ComboBox<PromotionScope> cmbScope;
    @FXML
    private CheckBox chkActive;
    @FXML
    private DatePicker dpStartDate;
    @FXML
    private DatePicker dpEndDate;
    @FXML
    private ListView<AffectedItem> listAffected;
    @FXML
    private TextField txtAffectedSearch;
    @FXML
    private VBox panelAffected;
    @FXML
    private VBox panelVolume;
    @FXML
    private TextField txtBuyQty;
    @FXML
    private TextField txtFreeQty;

    private ContextMenu searchContextMenu;
    private Promotion promotion;
    private boolean saved = false;
    private final Set<Integer> affectedIds = new HashSet<>();

    private ProductUseCase productUseCase;
    private CategoryUseCase categoryUseCase;

    @Override
    public void inject(ServiceContainer container) {
        this.productUseCase = container.getProductUseCase();
        this.categoryUseCase = container.getCategoryUseCase();
        setupForm();
    }

    private void setupForm() {
        cmbType.setItems(FXCollections.observableArrayList(PromotionType.values()));
        cmbType.setConverter(new StringConverter<PromotionType>() {
            @Override
            public String toString(PromotionType object) {
                return object == null ? "" : object.getDisplayName();
            }

            @Override
            public PromotionType fromString(String string) {
                return null;
            }
        });

        cmbScope.setItems(FXCollections.observableArrayList(PromotionScope.values()));
        cmbScope.setConverter(new StringConverter<PromotionScope>() {
            @Override
            public String toString(PromotionScope object) {
                return object == null ? "" : object.getDisplayName();
            }

            @Override
            public PromotionScope fromString(String string) {
                return null;
            }
        });

        cmbScope.valueProperty().addListener((obs, oldVal, newVal) -> {
            panelAffected.setVisible(newVal != PromotionScope.GLOBAL);
            panelAffected.setManaged(newVal != PromotionScope.GLOBAL);
        });

        cmbType.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isVolume = newVal == PromotionType.VOLUME_DISCOUNT;
            panelVolume.setVisible(isVolume);
            panelVolume.setManaged(isVolume);
            
            if (panelValue != null) {
                panelValue.setVisible(!isVolume);
                panelValue.setManaged(!isVolume);
            }
        });

        dpStartDate.setValue(LocalDate.now());
        dpEndDate.setValue(LocalDate.now().plusMonths(1));

        // Seleccionar por defecto para evitar valores nulos
        cmbType.getSelectionModel().selectFirst();
        cmbScope.getSelectionModel().select(PromotionScope.PRODUCT);

        // Sugerencias de b\u00fasqueda
        searchContextMenu = new ContextMenu();
        txtAffectedSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                searchContextMenu.hide();
                return;
            }
            updateSuggestions(newVal.trim());
        });

        // Cell Factory para poder eliminar de la lista
        listAffected.setCellFactory(lv -> new ListCell<AffectedItem>() {
            private final Button btnRemove = new Button();
            private final Label lbl = new Label();
            private final HBox container = new HBox(10, lbl, btnRemove);
            {
                HBox.setHgrow(lbl, javafx.scene.layout.Priority.ALWAYS);
                lbl.setMaxWidth(Double.MAX_VALUE);
                btnRemove.getStyleClass().addAll("btn-icon", "btn-delete-small");
                btnRemove.setGraphic(new FontAwesomeIconView(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TRASH));
                btnRemove.setOnAction(e -> {
                    AffectedItem item = getItem();
                    if (item != null) {
                        affectedIds.remove(item.id);
                        updateAffectedList();
                    }
                });
                container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(AffectedItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    lbl.setText(item.label);
                    setGraphic(container);
                }
            }
        });
    }

    public void setPromotion(Promotion p) {
        this.promotion = p;
        if (p != null) {
            lblTitle.setText("Editar Promoci\u00f3n");
            txtName.setText(p.getName());
            cmbType.setValue(p.getType());
            txtValue.setText(String.valueOf(p.getValue()));
            cmbScope.setValue(p.getScope());
            chkActive.setSelected(p.isActive());
            txtBuyQty.setText(String.valueOf(p.getBuyQty()));
            txtFreeQty.setText(String.valueOf(p.getFreeQty()));
            if (p.getStartDate() != null)
                dpStartDate.setValue(p.getStartDate().toLocalDate());
            if (p.getEndDate() != null)
                dpEndDate.setValue(p.getEndDate().toLocalDate());
            affectedIds.clear();
            affectedIds.addAll(p.getAffectedIds());
            updateAffectedList();
        }
    }

    private void updateAffectedList() {
        ObservableList<AffectedItem> items = FXCollections.observableArrayList();
        PromotionScope scope = cmbScope.getValue();

        try {
            for (Integer id : affectedIds) {
                String label = "ID: " + id;
                if (scope == PromotionScope.PRODUCT) {
                    Product p = productUseCase.getVisibleProducts(-1).stream()
                            .filter(prod -> prod.getId() == id).findFirst().orElse(null);
                    if (p != null)
                        label = p.getName();
                } else if (scope == PromotionScope.CATEGORY) {
                    Category c = categoryUseCase.getAll().stream()
                            .filter(cat -> cat.getId() == id).findFirst().orElse(null);
                    if (c != null)
                        label = c.getName();
                }
                items.add(new AffectedItem(id, label));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        listAffected.setItems(items);
    }

    @FXML
    private void handleAddAffected() {
        String search = txtAffectedSearch.getText().trim();
        if (search.isEmpty())
            return;

        PromotionScope scope = cmbScope.getValue();
        try {
            if (scope == PromotionScope.PRODUCT) {
                productUseCase.getVisibleProducts(-1).stream()
                        .filter(p -> p.getName().toLowerCase().contains(search.toLowerCase())
                                || String.valueOf(p.getId()).equals(search))
                        .findFirst()
                        .ifPresent(p -> {
                            affectedIds.add(p.getId());
                            updateAffectedList();
                            txtAffectedSearch.clear();
                        });
            } else if (scope == PromotionScope.CATEGORY) {
                categoryUseCase.getAll().stream()
                        .filter(c -> c.getName().toLowerCase().contains(search.toLowerCase())
                                || String.valueOf(c.getId()).equals(search))
                        .findFirst()
                        .ifPresent(c -> {
                            affectedIds.add(c.getId());
                            updateAffectedList();
                            txtAffectedSearch.clear();
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSave() {
        if (validate()) {
            if (promotion == null)
                promotion = new Promotion();
            promotion.setName(txtName.getText());
            promotion.setType(cmbType.getValue());
            double val = 0.0;
            if (cmbType.getValue() != PromotionType.VOLUME_DISCOUNT) {
                try { val = Double.parseDouble(txtValue.getText()); } catch (Exception e) {}
            }
            promotion.setValue(val);
            promotion.setScope(cmbScope.getValue());

            int buy = 0;
            int free = 0;
            try {
                buy = Integer.parseInt(txtBuyQty.getText());
                free = Integer.parseInt(txtFreeQty.getText());
            } catch (Exception ignored) {
            }

            promotion.setBuyQty(buy);
            promotion.setFreeQty(free);
            promotion.setActive(chkActive.isSelected());
            promotion.setStartDate(dpStartDate.getValue().atStartOfDay());
            promotion.setEndDate(dpEndDate.getValue().atStartOfDay());
            promotion.setAffectedIds(new ArrayList<>(affectedIds));

            saved = true;
            close();
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private boolean validate() {
        if (txtName.getText().isEmpty())
            return false;
        if (cmbType.getValue() == null)
            return false;
        if (cmbScope.getValue() == null)
            return false;
        if (cmbType.getValue() != PromotionType.VOLUME_DISCOUNT) {
            try {
                Double.parseDouble(txtValue.getText());
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private void close() {
        ((Stage) txtName.getScene().getWindow()).close();
    }

    public boolean isSaved() {
        return saved;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    private void updateSuggestions(String query) {
        searchContextMenu.hide(); // Ocultar para actualizar items
        searchContextMenu.getItems().clear();
        PromotionScope scope = cmbScope.getValue();
        if (scope == PromotionScope.GLOBAL) return;

        try {
            java.nio.file.Files.writeString(
                java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "search_log.txt"),
                "Query: " + query + " | Scope: " + scope + "\n",
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND
            );

            if (scope == PromotionScope.PRODUCT) {
                List<Product> matches = productUseCase.getVisibleProducts(-1).stream()
                        .filter(p -> p.getName().toLowerCase().contains(query.toLowerCase()))
                        .limit(8)
                        .collect(java.util.stream.Collectors.toList());

                for (Product p : matches) {
                    MenuItem item = new MenuItem(p.getName());
                    item.setOnAction(e -> {
                        affectedIds.add(p.getId());
                        updateAffectedList();
                        txtAffectedSearch.clear();
                        searchContextMenu.hide();
                    });
                    searchContextMenu.getItems().add(item);
                }
            } else if (scope == PromotionScope.CATEGORY) {
                List<Category> matches = categoryUseCase.getAll().stream()
                        .filter(c -> c.getName().toLowerCase().contains(query.toLowerCase()))
                        .limit(8)
                        .collect(java.util.stream.Collectors.toList());

                for (Category c : matches) {
                    MenuItem item = new MenuItem(c.getName());
                    item.setOnAction(e -> {
                        affectedIds.add(c.getId());
                        updateAffectedList();
                        txtAffectedSearch.clear();
                        searchContextMenu.hide();
                    });
                    searchContextMenu.getItems().add(item);
                }
            }

            if (!searchContextMenu.getItems().isEmpty()) {
                searchContextMenu.show(txtAffectedSearch, javafx.geometry.Side.BOTTOM, 0, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class AffectedItem {
        int id;
        String label;
        AffectedItem(int id, String l) { this.id = id; this.label = l; }
        @Override public String toString() { return label; }
    }
}
