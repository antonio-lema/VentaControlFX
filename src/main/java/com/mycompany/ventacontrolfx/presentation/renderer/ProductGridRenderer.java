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
        if (products.isEmpty()) {
            productsPane.getChildren().clear();
            if (labelCountProducts != null)
                labelCountProducts.setText("0");

            Label placeholder = new Label(bundle.getString("product.renderer.no_results"));
            placeholder.setStyle("-fx-font-size: 16px; -fx-text-fill: -fx-text-custom-muted; -fx-padding: 40 0;");
            placeholder.setOpacity(0);
            productsPane.getChildren().add(placeholder);

            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400),
                    placeholder);
            ft.setToValue(1.0);
            ft.play();
            return;
        }

        java.util.List<javafx.scene.Node> nodes = new java.util.ArrayList<>();
        int i = 0;
        for (Product p : products) {
            nodes.add(createProductNode(p, i < 20)); // Only animate first 20
            i++;
        }

        productsPane.getChildren().setAll(nodes);
        if (labelCountProducts != null)
            labelCountProducts.setText(String.valueOf(products.size()));

        // Trigger animations for those that need it
        for (javafx.scene.Node node : nodes) {
            if (node.getUserData() instanceof javafx.animation.Animation) {
                ((javafx.animation.Animation) node.getUserData()).play();
            }
        }
    }

    public void showSkeleton(int count) {
        java.util.List<javafx.scene.Node> skeletons = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            skeletons.add(new com.mycompany.ventacontrolfx.component.SkeletonProductBox());
        }
        productsPane.getChildren().setAll(skeletons);
    }

    public void append(List<Product> products) {
        if (products == null || products.isEmpty())
            return;

        java.util.List<javafx.scene.Node> nodes = new java.util.ArrayList<>();
        for (Product p : products) {
            nodes.add(createProductNode(p, false));
        }
        productsPane.getChildren().addAll(nodes);

        if (labelCountProducts != null) {
            int total = productsPane.getChildren().size();
            labelCountProducts.setText(String.valueOf(total));
        }
    }

    private javafx.scene.Node createProductNode(Product p, boolean animate) {
        String discountDesc = promotionResolver != null ? promotionResolver.apply(p) : null;
        ProductBox box = new ProductBox(p, globalTaxRate, pricesIncludeTax, discountDesc, bundle, onAddToCart);

        if (!animate)
            return box;

        box.setOpacity(0);
        box.setTranslateY(20);

        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), box);
        ft.setToValue(1.0);
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                javafx.util.Duration.millis(400), box);
        tt.setToY(0);

        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(ft, tt);
        int index = productsPane.getChildren().size(); // Approximate
        pt.setDelay(javafx.util.Duration.millis(Math.min(index, 12) * 30));

        box.setUserData(pt); // Store animation to play after batch add
        return box;
    }
}
