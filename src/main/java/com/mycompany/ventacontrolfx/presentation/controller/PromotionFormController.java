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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
    private ComboBox<PromotionScope> cmbScope;
    @FXML
    private CheckBox chkActive;
    @FXML
    private DatePicker dpStartDate;
    @FXML
    private DatePicker dpEndDate;
    @FXML
    private ListView<String> listAffected;
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
        });

        dpStartDate.setValue(LocalDate.now());
        dpEndDate.setValue(LocalDate.now().plusMonths(1));
    }

    public void setPromotion(Promotion p) {
        this.promotion = p;
        if (p != null) {
            lblTitle.setText("Editar Promoción");
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
        ObservableList<String> items = FXCollections.observableArrayList();
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
                items.add(label);
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
            promotion.setValue(Double.parseDouble(txtValue.getText()));
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
        try {
            Double.parseDouble(txtValue.getText());
        } catch (Exception e) {
            return false;
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
}
