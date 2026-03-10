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

    public ProductGridRenderer(TilePane productsPane, Label labelCountProducts, double globalTaxRate, boolean pricesIncludeTax, Consumer<Product> onAddToCart) {
        this.productsPane = productsPane;
        this.labelCountProducts = labelCountProducts;
        this.globalTaxRate = globalTaxRate;
        this.pricesIncludeTax = pricesIncludeTax;
        this.onAddToCart = onAddToCart;
    }

    public void render(List<Product> products) {
        productsPane.getChildren().clear();
        if (labelCountProducts != null)
            labelCountProducts.setText(String.valueOf(products.size()));

        for (Product p : products) {
            ProductBox box = new ProductBox(p, globalTaxRate, pricesIncludeTax, onAddToCart);
            productsPane.getChildren().add(box);
        }
    }
}
