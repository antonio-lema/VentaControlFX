package com.mycompany.ventacontrolfx.application.service;

import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.model.Product;
import com.mycompany.ventacontrolfx.service.GlobalEventBus;
import javafx.concurrent.Task;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;

/**
 * Product Application Service (Use Cases).
 * Handles business orchestration, image persistence, and asynchronous
 * execution.
 */
public class ProductUseCase {
    private final IProductRepository repository;
    private final GlobalEventBus eventBus;
    private final ProductValidator validator;
    private static final String IMAGES_DIR = "data/images/products";

    public ProductUseCase(IProductRepository repository, GlobalEventBus eventBus) {
        this.repository = repository;
        this.eventBus = eventBus;
        this.validator = new ProductValidator();
        ensureImageDirectory();
    }

    private void ensureImageDirectory() {
        File dir = new File(IMAGES_DIR);
        if (!dir.exists())
            dir.mkdirs();
    }

    /**
     * Copy an external image to the local application data directory.
     * Returns the relative path to the copied image.
     */
    private String processProductImage(Product product) {
        String originalPath = product.getImagePath();
        if (originalPath == null || originalPath.isBlank())
            return null;

        File source = new File(originalPath);
        if (!source.exists())
            return originalPath; // Keep as is if path is invalid or already local

        // If it's already in our images directory, don't copy again
        if (originalPath.contains(IMAGES_DIR))
            return originalPath;

        try {
            String extension = "";
            int i = originalPath.lastIndexOf('.');
            if (i > 0)
                extension = originalPath.substring(i);

            String newFileName = "prod_" + System.currentTimeMillis() + "_" +
                    product.getName().replaceAll("[^a-zA-Z0-9]", "_") + extension;
            File target = new File(IMAGES_DIR, newFileName);

            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return target.getPath();
        } catch (Exception e) {
            System.err.println("Error copying product image: " + e.getMessage());
            return originalPath;
        }
    }

    public Task<List<Product>> getVisibleProductsTask() {
        return new Task<>() {
            @Override
            protected List<Product> call() throws Exception {
                return repository.findVisible();
            }
        };
    }

    public Task<List<Product>> getAllProductsTask() {
        return new Task<>() {
            @Override
            protected List<Product> call() throws Exception {
                return repository.findAll();
            }
        };
    }

    public Task<Void> saveOrUpdateTask(Product product) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                ProductValidator.ValidationResult result = validator.validate(product);
                if (!result.isValid()) {
                    throw new IllegalArgumentException(String.join("\n", result.getErrors()));
                }

                // Persistence: ensure image is local before saving to DB
                String localImagePath = processProductImage(product);
                product.setImagePath(localImagePath);

                if (product.getId() == 0) {
                    repository.save(product);
                } else {
                    repository.update(product);
                }

                eventBus.publishDataChange();
                return null;
            }
        };
    }

    public Task<Void> deleteTask(int productId) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                repository.delete(productId);
                eventBus.publishDataChange();
                return null;
            }
        };
    }

    public Task<Integer> getCountTask() {
        return new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return repository.countTotal();
            }
        };
    }

    public void notifyChange() {
        eventBus.publishDataChange();
    }
}
