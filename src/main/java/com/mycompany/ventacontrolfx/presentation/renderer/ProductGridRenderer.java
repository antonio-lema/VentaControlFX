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

        if (products.isEmpty()) {
            Label placeholder = new Label(bundle.getString("product.renderer.no_results"));
            placeholder.setStyle("-fx-font-size: 16px; -fx-text-fill: -fx-text-custom-muted; -fx-padding: 40 0;");
            
            // Animaci\u00f3n simple para el placeholder
            placeholder.setOpacity(0);
            productsPane.getChildren().add(placeholder);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), placeholder);
            ft.setToValue(1.0);
            ft.play();
        } else {
            for (Product p : products) {
                addProductToGrid(p);
            }
        }
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
        
        // Estado inicial para la animaci\u00f3n (desplazado hacia abajo y transparente)
        box.setOpacity(0);
        box.setTranslateY(30);
        
        productsPane.getChildren().add(box);
        
        // Animaci\u00f3n de entrada (Slide up + Fade in)
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(500), box);
        ft.setToValue(1.0);
        
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(500), box);
        tt.setToY(0);
        
        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(ft, tt);
        
        // Retraso escalonado (staggered) para un efecto m\u00e1s org\u00e1nico
        int index = productsPane.getChildren().size() - 1;
        // Solo aplicar retraso escalonado a los primeros 24 elementos por rendimiento
        if (index < 24) {
            pt.setDelay(javafx.util.Duration.millis(index * 40));
        }
        
        pt.play();
    }
}
