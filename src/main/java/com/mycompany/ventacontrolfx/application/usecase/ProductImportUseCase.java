package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.domain.repository.ICategoryRepository;
import com.mycompany.ventacontrolfx.util.AuthorizationService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductImportUseCase {
    private final IProductRepository productRepository;
    private final ICategoryRepository categoryRepository;
    private final AuthorizationService authService;

    public ProductImportUseCase(IProductRepository productRepository,
            ICategoryRepository categoryRepository,
            AuthorizationService authService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.authService = authService;
    }

    public int importFromCsv(File file) throws Exception {
        authService.checkPermission("PRODUCTOS");

        // 1. Cargar categorÃ­as para mapeo por nombre
        List<Category> allCategories = categoryRepository.getAll();
        Map<String, Integer> categoryMap = new HashMap<>();
        int defaultCategoryId = 0;

        for (Category cat : allCategories) {
            categoryMap.put(cat.getName().toLowerCase(), cat.getId());
            if (defaultCategoryId == 0)
                defaultCategoryId = cat.getId();
        }

        List<Product> productsToImport = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    // Opcional: Validar cabecera
                    continue;
                }

                if (line.trim().isEmpty())
                    continue;

                // Soportar coma o punto y coma
                String[] values = line.split("[,;]");
                if (values.length < 2)
                    continue;

                try {
                    Product p = new Product();
                    // Formato esperado: Nombre, Precio, Stock, Categoria, SKU, IVA
                    p.setName(clean(values[0]));
                    p.setPrice(parseDouble(values.length > 1 ? values[1] : "0"));
                    p.setStockQuantity(parseInt(values.length > 2 ? values[2] : "0"));

                    String catName = values.length > 3 ? clean(values[3]) : "";
                    Integer catId = categoryMap.get(catName.toLowerCase());
                    p.setCategoryId(catId != null ? catId : defaultCategoryId);

                    p.setSku(values.length > 4 ? clean(values[4]) : "");

                    Double iva = values.length > 5 ? parseDouble(values[5]) : 21.0;
                    p.setIva(iva);

                    p.setVisible(true);
                    p.setActive(true);

                    productsToImport.add(p);
                } catch (Exception e) {
                    System.err.println("Error parseando lÃ­nea: " + line + " -> " + e.getMessage());
                }
            }
        }

        if (!productsToImport.isEmpty()) {
            productRepository.saveAll(productsToImport);
        }

        return productsToImport.size();
    }

    private String clean(String value) {
        if (value == null)
            return "";
        return value.trim().replace("\"", "");
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(clean(value).replace(",", "."));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(clean(value));
        } catch (Exception e) {
            return 0;
        }
    }
}
