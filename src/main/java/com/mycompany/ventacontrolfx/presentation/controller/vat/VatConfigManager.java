package com.mycompany.ventacontrolfx.presentation.controller.vat;

import com.mycompany.ventacontrolfx.application.usecase.CategoryUseCase;
import com.mycompany.ventacontrolfx.application.usecase.TaxManagementUseCase;
import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.model.TaxGroup;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import javafx.scene.control.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestiona la configuración global de IVA y la asignación masiva por categorías.
 */
public class VatConfigManager {

    private final ServiceContainer container;
    private final TaxManagementUseCase taxManagementUseCase;
    private final CategoryUseCase categoryUseCase;
    private final AsyncManager asyncManager;

    // UI References
    private final ComboBox<TaxGroup> cmbGlobalTaxGroup;
    private final Label lblCurrentGlobalTaxGroup;
    private final TextField txtGlobalReason;
    private final MenuButton cmbCategory;
    private final ComboBox<TaxGroup> cmbCategoryTaxGroup;
    private final TextField txtCategoryReason;

    public VatConfigManager(
            ServiceContainer container,
            TaxManagementUseCase taxManagementUseCase,
            CategoryUseCase categoryUseCase,
            AsyncManager asyncManager,
            ComboBox<TaxGroup> cmbGlobalTaxGroup,
            Label lblCurrentGlobalTaxGroup,
            TextField txtGlobalReason,
            MenuButton cmbCategory,
            ComboBox<TaxGroup> cmbCategoryTaxGroup,
            TextField txtCategoryReason) {
        this.container = container;
        this.taxManagementUseCase = taxManagementUseCase;
        this.categoryUseCase = categoryUseCase;
        this.asyncManager = asyncManager;
        this.cmbGlobalTaxGroup = cmbGlobalTaxGroup;
        this.lblCurrentGlobalTaxGroup = lblCurrentGlobalTaxGroup;
        this.txtGlobalReason = txtGlobalReason;
        this.cmbCategory = cmbCategory;
        this.cmbCategoryTaxGroup = cmbCategoryTaxGroup;
        this.txtCategoryReason = txtCategoryReason;
    }

    public void setupCategorySelectors(List<Category> categories) {
        if (cmbCategory == null) return;
        cmbCategory.getItems().clear();
        for (Category cat : categories) {
            CheckBox cb = new CheckBox(cat.getName());
            cb.getStyleClass().add("permission-checkbox");
            cb.setMaxWidth(Double.MAX_VALUE);
            cb.setUserData(cat);

            cb.selectedProperty().addListener((obs, old, nv) -> {
                long count = cmbCategory.getItems().stream()
                        .map(m -> ((CustomMenuItem) m).getContent())
                        .filter(n -> n instanceof CheckBox && ((CheckBox) n).isSelected())
                        .count();
                cmbCategory.setText(count == 0 ? container.getBundle().getString("vat.group.choosing")
                        : String.format(container.getBundle().getString("vat.group.selected_count"), count));
            });

            CustomMenuItem item = new CustomMenuItem(cb, false);
            cmbCategory.getItems().add(item);
        }
    }

    public List<Category> getSelectedCategories() {
        return cmbCategory.getItems().stream()
                .map(m -> ((CustomMenuItem) m).getContent())
                .filter(n -> n instanceof CheckBox && ((CheckBox) n).isSelected())
                .map(n -> (Category) n.getUserData())
                .collect(Collectors.toList());
    }

    public void handleUpdateGlobalTaxGroup(Runnable onSuccess) {
        TaxGroup taxGroup = cmbGlobalTaxGroup.getSelectionModel().getSelectedItem();
        if (taxGroup == null) {
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"), container.getBundle().getString("vat.group.error.select_group"));
            return;
        }

        String reason = txtGlobalReason != null ? txtGlobalReason.getText() : container.getBundle().getString("vat.reason.global_manual");
        if (reason.isBlank()) {
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"), container.getBundle().getString("vat.error.reason_required"));
            return;
        }

        asyncManager.runAsyncTask(() -> {
            taxManagementUseCase.setDefaultTaxGroup(taxGroup.getTaxGroupId(), reason);
            return null;
        }, (res) -> {
            lblCurrentGlobalTaxGroup.setText(taxGroup.getName());
            if (txtGlobalReason != null) txtGlobalReason.clear();
            AlertUtil.showInfo(container.getBundle().getString("alert.success"), container.getBundle().getString("vat.group.success.global"));
            if (onSuccess != null) onSuccess.run();
        }, (err) -> AlertUtil.showError(container.getBundle().getString("alert.error"), err.getMessage()));
    }

    public void handleUpdateCategoryTaxGroup(Runnable onSuccess) {
        TaxGroup taxGroup = cmbCategoryTaxGroup.getSelectionModel().getSelectedItem();
        List<Category> selectedCategories = getSelectedCategories();
        
        if (taxGroup == null) {
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"), container.getBundle().getString("vat.group.error.select_group"));
            return;
        }
        if (selectedCategories.isEmpty()) {
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"), container.getBundle().getString("vat.group.error.select_cat"));
            return;
        }

        String reason = txtCategoryReason.getText();
        if (reason == null || reason.isBlank()) {
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"), container.getBundle().getString("vat.error.reason_required"));
            return;
        }

        try {
            int updatedCount = 0;
            for (Category cat : selectedCategories) {
                cat.setTaxGroupId(taxGroup.getTaxGroupId());
                categoryUseCase.update(cat);
                taxManagementUseCase.logCategoryTaxChange(cat.getId(), taxGroup.getTaxGroupId(), txtCategoryReason.getText());
                updatedCount++;
            }

            container.getTaxRepository().syncMirroredValues();
            AlertUtil.showInfo(container.getBundle().getString("alert.success"), 
                    String.format(container.getBundle().getString("vat.group.success.category"), updatedCount));
            
            if (onSuccess != null) onSuccess.run();

        } catch (Exception e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"), e.getMessage());
        }
    }
}

