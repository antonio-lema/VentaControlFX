package com.mycompany.ventacontrolfx.presentation.controller.vat;

import com.mycompany.ventacontrolfx.application.usecase.TaxManagementUseCase;
import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.TaxGroup;
import com.mycompany.ventacontrolfx.domain.model.VisibilityFilter;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestiona la asignación individual de grupos de impuestos a productos específicos.
 */
public class VatProductManager {

    private final ServiceContainer container;
    private final TaxManagementUseCase taxManagementUseCase;
    private final IProductRepository productRepository;
    private final AsyncManager asyncManager;

    // UI References
    private final ListView<Product> listVatSelectedProducts;
    private final TextField txtVatProductSearch;
    private final MenuButton btnVatProductExplorer;
    private final ComboBox<TaxGroup> cmbProductTaxGroup;

    private final ObservableList<Product> vatSelectedProducts = FXCollections.observableArrayList();

    public VatProductManager(
            ServiceContainer container,
            TaxManagementUseCase taxManagementUseCase,
            IProductRepository productRepository,
            AsyncManager asyncManager,
            ListView<Product> listVatSelectedProducts,
            TextField txtVatProductSearch,
            MenuButton btnVatProductExplorer,
            ComboBox<TaxGroup> cmbProductTaxGroup) {
        this.container = container;
        this.taxManagementUseCase = taxManagementUseCase;
        this.productRepository = productRepository;
        this.asyncManager = asyncManager;
        this.listVatSelectedProducts = listVatSelectedProducts;
        this.txtVatProductSearch = txtVatProductSearch;
        this.btnVatProductExplorer = btnVatProductExplorer;
        this.cmbProductTaxGroup = cmbProductTaxGroup;
    }

    public void init() {
        setupVatProductCard();
    }

    private void setupVatProductCard() {
        listVatSelectedProducts.setItems(vatSelectedProducts);
        listVatSelectedProducts.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String sku = item.getSku();
                    String displayText = item.getName() + (sku != null && !sku.trim().isEmpty() ? " (" + sku + ")" : "");
                    setText(displayText);
                    Button btnRemove = new Button();
                    btnRemove.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
                    btnRemove.getStyleClass().add("estetica-btn-icon-danger");
                    btnRemove.setOnAction(e -> vatSelectedProducts.remove(item));
                    setGraphic(btnRemove);
                    setContentDisplay(ContentDisplay.RIGHT);
                }
            }
        });

        txtVatProductSearch.setOnAction(e -> {
            String query = txtVatProductSearch.getText().trim();
            if (query.isEmpty()) return;

            asyncManager.runAsyncTask(() -> {
                try {
                    return productRepository.searchPaginated(query, 1, 0, -1, VisibilityFilter.VISIBLE);
                } catch (SQLException ex) {
                    return null;
                }
            }, (res) -> {
                @SuppressWarnings("unchecked")
                List<Product> foundList = (List<Product>) res;
                if (foundList != null && !foundList.isEmpty()) {
                    Product found = foundList.get(0);
                    if (!vatSelectedProducts.contains(found)) {
                        vatSelectedProducts.add(found);
                        txtVatProductSearch.clear();
                    }
                } else {
                    AlertUtil.showWarning(container.getBundle().getString("alert.warning"),
                            container.getBundle().getString("vat.error.not_found"));
                }
            }, null);
        });
    }

    public void setupVatProductExplorer(List<Category> categories) {
        if (btnVatProductExplorer == null) return;
        btnVatProductExplorer.setVisible(true);
        btnVatProductExplorer.setManaged(true);
        btnVatProductExplorer.getItems().clear();

        if (categories == null) return;

        for (Category cat : categories) {
            Menu catMenu = new Menu(cat.getName());
            catMenu.getItems().add(new MenuItem("Cargando..."));

            catMenu.setOnShowing(e -> {
                if (catMenu.getItems().size() > 1 || !catMenu.getItems().get(0).getText().equals("Cargando...")) {
                    return;
                }

                asyncManager.runAsyncTask(() -> {
                    try {
                        return productRepository.getByCategory(cat.getId(), -1);
                    } catch (SQLException ex) {
                        return null;
                    }
                }, (res) -> {
                    catMenu.getItems().clear();
                    @SuppressWarnings("unchecked")
                    List<Product> products = (List<Product>) res;
                    if (products != null) {
                        for (Product p : products) {
                            MenuItem pItem = new MenuItem(p.getName() + (p.getSku() != null ? " (" + p.getSku() + ")" : ""));
                            pItem.setOnAction(ae -> {
                                if (!vatSelectedProducts.contains(p)) {
                                    vatSelectedProducts.add(p);
                                }
                            });
                            catMenu.getItems().add(pItem);
                        }
                    }
                }, null);
            });
            btnVatProductExplorer.getItems().add(catMenu);
        }
    }

    public void handleUpdateProductTaxGroup(Runnable onSuccess) {
        if (vatSelectedProducts.isEmpty()) {
            AlertUtil.showWarning(container.getBundle().getString("vat.error.no_selection"),
                    container.getBundle().getString("vat.error.select_one"));
            return;
        }

        TaxGroup selectedGroup = cmbProductTaxGroup.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            AlertUtil.showWarning(container.getBundle().getString("vat.error.group_not_selected"),
                    container.getBundle().getString("vat.error.select_new_group"));
            return;
        }

        List<Integer> ids = vatSelectedProducts.stream().map(Product::getId).collect(Collectors.toList());

        asyncManager.runAsyncTask(() -> {
            taxManagementUseCase.updateTaxGroupForProducts(ids, selectedGroup.getTaxGroupId(),
                    container.getBundle().getString("vat.log.manual_change"));
            return ids.size();
        }, (res) -> {
            AlertUtil.showInfo(container.getBundle().getString("alert.success"),
                    String.format(container.getBundle().getString("vat.group.success.products"), res));
            vatSelectedProducts.clear();
            if (onSuccess != null) onSuccess.run();
        }, (err) -> AlertUtil.showError(container.getBundle().getString("vat.error.update_failed"), err.getMessage()));
    }

    public void handleClearVatSelection() {
        vatSelectedProducts.clear();
        AlertUtil.showToast(container.getBundle().getString("vat.selection.cleared"));
    }
}

