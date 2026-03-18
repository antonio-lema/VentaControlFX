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

    public ProductGridRenderer(TilePane productsPane, Label labelCountProducts, double globalTaxRate,
            boolean pricesIncludeTax, java.util.function.Function<Product, String> promotionResolver,
            Consumer<Product> onAddToCart) {
        this.productsPane = productsPane;
        this.labelCountProducts = labelCountProducts;
        this.globalTaxRate = globalTaxRate;
        this.pricesIncludeTax = pricesIncludeTax;
        this.promotionResolver = promotionResolver;
        this.onAddToCart = onAddToCart;
    }

    public void render(List<Product> products) {
        productsPane.getChildren().clear();
        if (labelCountProducts != null)
            labelCountProducts.setText(String.valueOf(products.size()));

        for (Product p : products) {
            String discountDesc = promotionResolver != null ? promotionResolver.apply(p) : null;
            ProductBox box = new ProductBox(p, globalTaxRate, pricesIncludeTax, discountDesc, onAddToCart);
            productsPane.getChildren().add(box);
        }
    }
}
