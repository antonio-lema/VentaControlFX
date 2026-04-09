package com.mycompany.ventacontrolfx.presentation.renderer;

import com.mycompany.ventacontrolfx.component.ProductBox;
import com.mycompany.ventacontrolfx.domain.model.Product;
import javafx.application.Platform;
import javafx.scene.layout.TilePane;
import javafx.scene.control.Label;
import java.util.List;
import java.util.function.Consumer;

public class ProductGridRenderer {
    private final TilePane productsPane;
    private final Label labelCountProducts;
    private final Consumer<Product> onAddToCart;
    private final double globalTaxRate;
    private final boolean pricesIncludeTax;
    private final java.util.function.Function<Product, String> promotionResolver;
    private final java.util.ResourceBundle bundle;

    public ProductGridRenderer(TilePane productsPane, Label labelCountProducts, double globalTaxRate,
            boolean pricesIncludeTax, java.util.function.Function<Product, String> promotionResolver,
            java.util.ResourceBundle bundle,
            Consumer<Product> onAddToCart) {
        this.productsPane = productsPane;
        this.labelCountProducts = labelCountProducts;
        this.globalTaxRate = globalTaxRate;
        this.pricesIncludeTax = pricesIncludeTax;
        this.promotionResolver = promotionResolver;
        this.bundle = bundle;
        this.onAddToCart = onAddToCart;
    }

    public void render(List<Product> products) {
        productsPane.getChildren().clear();
        if (labelCountProducts != null)
            labelCountProducts.setText(String.valueOf(products.size()));

        for (Product p : products) {
            addProductToGrid(p);
        }

        // Animation de entrada
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), productsPane);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    public void showSkeleton(int count) {
        productsPane.getChildren().clear();
        for (int i = 0; i < count; i++) {
            productsPane.getChildren().add(new com.mycompany.ventacontrolfx.component.SkeletonProductBox());
        }
    }

    public void append(List<Product> products) {
        if (products == null || products.isEmpty())
            return;

        for (Product p : products) {
            addProductToGrid(p);
        }

        if (labelCountProducts != null) {
            int currentCount = productsPane.getChildren().size();
            labelCountProducts.setText(String.valueOf(currentCount));
        }
    }

    private void addProductToGrid(Product p) {
        String discountDesc = promotionResolver != null ? promotionResolver.apply(p) : null;
        ProductBox box = new ProductBox(p, globalTaxRate, pricesIncludeTax, discountDesc, bundle, onAddToCart);
        productsPane.getChildren().add(box);
    }
}
