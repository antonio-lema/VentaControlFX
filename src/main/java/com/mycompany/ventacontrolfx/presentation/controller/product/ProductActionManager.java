package com.mycompany.ventacontrolfx.presentation.controller.product;

import com.mycompany.ventacontrolfx.application.usecase.ProductUseCase;
import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.ImportResultDialogController;
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import com.mycompany.ventacontrolfx.presentation.navigation.ModalService;
import javafx.stage.FileChooser;
import java.io.File;
import java.sql.SQLException;

/**
 * Gestiona las acciones de negocio del controlador de productos (CRUD, Importación).
 */
public class ProductActionManager {

    private final ServiceContainer container;
    private final ProductUseCase productUseCase;
    private final AsyncManager asyncManager;

    public ProductActionManager(ServiceContainer container, ProductUseCase productUseCase, AsyncManager asyncManager) {
        this.container = container;
        this.productUseCase = productUseCase;
        this.asyncManager = asyncManager;
    }

    public void handleImportCsv(Runnable onRefresh) {
        if (!container.getUserSession().hasPermission("producto.importar") && !container.getUserSession().hasPermission("PRODUCTOS")) {
            AlertUtil.showError(container.getBundle().getString("alert.access_denied"), container.getBundle().getString("error.no_permission"));
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo CSV de productos");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos CSV", "*.csv"));

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            asyncManager.runAsyncTask(() -> container.getProductImportUseCase().importFromCsv(selectedFile), (Integer count) -> {
                ModalService.showTransparentModal("/view/dialog/import_result_dialog.fxml",
                        container.getBundle().getString("product.import.success_title"), container,
                        (c) -> { if (c instanceof ImportResultDialogController) ((ImportResultDialogController) c).initData(count, true, ""); });
                onRefresh.run();
            }, (Throwable ex) -> {
                ModalService.showTransparentModal("/view/dialog/import_result_dialog.fxml",
                        container.getBundle().getString("product.import.error_title"), container,
                        (c) -> { if (c instanceof ImportResultDialogController) ((ImportResultDialogController) c).initData(0, false, ex.getMessage()); });
            });
        }
    }

    public void openProductDialog(Product p, Runnable onRefresh) {
        ModalService.showTransparentModal("/view/product/add_product.fxml",
                p == null ? container.getBundle().getString("product.dialog.new") : container.getBundle().getString("product.dialog.edit"),
                container, (AddProductController controller) -> { if (p != null) controller.setProduct(p); });
        onRefresh.run();
    }

    public void handleDeleteProduct(Product p, Runnable onRefresh) {
        if (!container.getUserSession().hasPermission("producto.eliminar")) {
            AlertUtil.showError(container.getBundle().getString("alert.access_denied"), container.getBundle().getString("error.no_permission"));
            return;
        }
        if (AlertUtil.showConfirmation(container.getBundle().getString("btn.delete"), container.getBundle().getString("product.confirm.delete") + " " + p.getName() + "?", "")) {
            try {
                productUseCase.deleteProduct(p.getId());
                onRefresh.run();
            } catch (SQLException e) {
                AlertUtil.showError(container.getBundle().getString("alert.error"), container.getBundle().getString("product.error.delete"));
            }
        }
    }
}



