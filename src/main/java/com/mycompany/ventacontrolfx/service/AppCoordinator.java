package com.mycompany.ventacontrolfx.service;

import com.mycompany.ventacontrolfx.model.Product;
import com.mycompany.ventacontrolfx.model.Category;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.sql.SQLException;

public class AppCoordinator {
    private static AppCoordinator instance;

    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private final CartService cartService = new CartService();
    private final ProductFilterService filterService = new ProductFilterService();
    private final SaleService saleService = new SaleService(new com.mycompany.ventacontrolfx.util.UserSession());
    private final UserService userService = new UserService();
    private final CashClosureService closureService = new CashClosureService(
            new com.mycompany.ventacontrolfx.util.UserSession());
    private final ClientService clientService = new ClientService();
    private final SaleConfigService configService = new SaleConfigService();

    private final ObjectProperty<List<Product>> allProducts = new SimpleObjectProperty<>();

    private AppCoordinator() {
    }

    public static synchronized AppCoordinator getInstance() {
        if (instance == null) {
            instance = new AppCoordinator();
        }
        return instance;
    }

    public ProductService getProductService() {
        return productService;
    }

    public CategoryService getCategoryService() {
        return categoryService;
    }

    public CartService getCartService() {
        return cartService;
    }

    public ProductFilterService getFilterService() {
        return filterService;
    }

    public SaleService getSaleService() {
        return saleService;
    }

    public UserService getUserService() {
        return userService;
    }

    public CashClosureService getClosureService() {
        return closureService;
    }

    public ClientService getClientService() {
        return clientService;
    }

    public SaleConfigService getConfigService() {
        return configService;
    }

    public void loadInitialData() throws SQLException {
        allProducts.set(productService.getAllVisibleProducts());
    }

    public List<Product> getAllProducts() {
        return allProducts.get();
    }

    public ObjectProperty<List<Product>> allProductsProperty() {
        return allProducts;
    }

    public List<Product> getFilteredProducts() {
        return filterService.filter(allProducts.get());
    }
}
