package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.component.ProductBox;
import com.mycompany.ventacontrolfx.model.Product;
import javafx.application.Platform;
import javafx.scene.layout.TilePane;
import javafx.scene.control.Label;
import java.util.List;
import java.util.function.Consumer;

/**
 * Enterprise Product Grid Renderer.
 * Supports incremental rendering to prevent UI lag with large product lists.
 * Thread-safe: Cancels previous rendering if a new one is requested.
 */
public class ProductGridRenderer {
    private static final int BATCH_SIZE = 20;

    private final TilePane productsPane;
    private final Label labelCountProducts;
    private final Consumer<Product> onAddToCart;

    private long currentRenderId = 0;

    public ProductGridRenderer(TilePane productsPane, Label labelCountProducts, Consumer<Product> onAddToCart) {
        this.productsPane = productsPane;
        this.labelCountProducts = labelCountProducts;
        this.onAddToCart = onAddToCart;
    }

    /**
     * Renders products incrementally in batches.
     * Cancels any ongoing rendering task.
     */
    public void render(List<Product> products) {
        currentRenderId++;
        final long renderId = currentRenderId;

        productsPane.getChildren().clear();
        if (labelCountProducts != null) {
            labelCountProducts.setText(String.valueOf(products.size()));
        }

        if (products.isEmpty())
            return;

        renderBatch(products, 0, renderId);
    }

    private void renderBatch(List<Product> products, int startIndex, long renderId) {
        // If a new render has been requested, stop this one
        if (renderId != currentRenderId)
            return;

        int endIndex = Math.min(startIndex + BATCH_SIZE, products.size());

        for (int i = startIndex; i < endIndex; i++) {
            Product p = products.get(i);
            ProductBox box = new ProductBox(p, onAddToCart);
            productsPane.getChildren().add(box);
        }

        if (endIndex < products.size()) {
            Platform.runLater(() -> renderBatch(products, endIndex, renderId));
        }
    }
}
