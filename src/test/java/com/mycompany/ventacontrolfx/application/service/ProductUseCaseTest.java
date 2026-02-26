package com.mycompany.ventacontrolfx.application.service;

import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.model.Product;
import com.mycompany.ventacontrolfx.service.GlobalEventBus;
import javafx.concurrent.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for ProductUseCase using a Mock Repository.
 * Comprehensive coverage: creation, update, deletion, and event notification.
 */
public class ProductUseCaseTest {

    private ProductUseCase useCase;
    private MockProductRepository mockRepo;
    private GlobalEventBus mockBus;
    private int eventCount = 0;

    @BeforeEach
    public void setUp() {
        mockRepo = new MockProductRepository();
        mockBus = new GlobalEventBus();
        useCase = new ProductUseCase(mockRepo, mockBus);
        eventCount = 0;

        // Listen to data changes on the event bus
        mockBus.subscribe(new GlobalEventBus.DataChangeListener() {
            @Override
            public void onDataChanged() {
                eventCount++;
            }
        });
    }

    @Test
    public void testGetVisibleProducts() throws InterruptedException, ExecutionException, TimeoutException {
        Product p = new Product(1, "Visible Product", 10.0, true);
        mockRepo.products.add(p);

        Task<List<Product>> task = useCase.getVisibleProductsTask();
        runTask(task);

        List<Product> result = task.get(1, TimeUnit.SECONDS);
        assertEquals(1, result.size());
        assertEquals("Visible Product", result.get(0).getName());
    }

    @Test
    public void testCreateNewProduct() throws Exception {
        Product p = new Product(0, "New Product", 50.0, false);
        p.setCategoryId(1);

        Task<Void> task = useCase.saveOrUpdateTask(p);
        runTask(task);
        task.get(1, TimeUnit.SECONDS);

        assertEquals(1, mockRepo.products.size());
        assertEquals("New Product", mockRepo.products.get(0).getName());
        assertEquals(1, eventCount, "Should publish event after creation");
    }

    @Test
    public void testUpdateExistingProduct() throws Exception {
        Product existing = new Product(10, "Old Name", 10.0, false);
        existing.setCategoryId(1);
        mockRepo.products.add(existing);

        existing.setName("Updated Name");
        Task<Void> task = useCase.saveOrUpdateTask(existing);
        runTask(task);
        task.get(1, TimeUnit.SECONDS);

        assertEquals("Updated Name", mockRepo.products.get(0).getName());
        assertEquals(1, eventCount, "Should publish event after update");
    }

    @Test
    public void testDeleteProduct() throws Exception {
        Product p = new Product(10, "ToDelete", 10.0, false);
        mockRepo.products.add(p);

        Task<Void> task = useCase.deleteTask(10);
        runTask(task);
        task.get(1, TimeUnit.SECONDS);

        assertTrue(mockRepo.products.isEmpty());
        assertEquals(1, eventCount, "Should publish event after deletion");
    }

    @Test
    public void testSaveInvalidProduct() throws InterruptedException {
        Product invalid = new Product(0, "", -10.0, false);
        Task<Void> task = useCase.saveOrUpdateTask(invalid);
        runTask(task);

        try {
            task.get(1, TimeUnit.SECONDS);
            fail("Should have thrown IllegalArgumentException due to validation");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        } catch (Exception e) {
            fail("Execution failed unexpectedly: " + e.getMessage());
        }
    }

    private void runTask(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private static class MockProductRepository implements IProductRepository {
        public List<Product> products = new java.util.ArrayList<>();

        @Override
        public List<Product> findVisible() throws SQLException {
            return products;
        }

        @Override
        public List<Product> findAll() throws SQLException {
            return products;
        }

        @Override
        public int countTotal() throws SQLException {
            return products.size();
        }

        @Override
        public void save(Product p) throws SQLException {
            products.add(p);
        }

        @Override
        public void update(Product p) throws SQLException {
            for (int i = 0; i < products.size(); i++) {
                if (products.get(i).getId() == p.getId()) {
                    products.set(i, p);
                    break;
                }
            }
        }

        @Override
        public void delete(int id) throws SQLException {
            products.removeIf(p -> p.getId() == id);
        }
    }
}
