package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.application.usecase.ProductUseCase;
import com.mycompany.ventacontrolfx.application.usecase.CategoryUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.controller.AddProductController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class AddProductUITest extends ApplicationTest {

    private ServiceContainer mockContainer;
    private ProductUseCase mockProductUseCase;
    private CategoryUseCase mockCategoryUseCase;

    @Override
    public void start(Stage stage) throws Exception {
        // Simulamos el contenedor de servicios y los casos de uso para no tocar la BD
        // real
        mockContainer = mock(ServiceContainer.class);
        mockProductUseCase = mock(ProductUseCase.class);
        mockCategoryUseCase = mock(CategoryUseCase.class);

        when(mockContainer.getProductUseCase()).thenReturn(mockProductUseCase);
        when(mockContainer.getCategoryUseCase()).thenReturn(mockCategoryUseCase);

        // Preparamos una categoría simulada que aparecerá en el ComboBox
        Category cat = new Category(1, "Postres", true, false);
        when(mockCategoryUseCase.getAll()).thenReturn(Arrays.asList(cat));

        // Cargamos la vista directamente
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_product.fxml"));
        Parent root = loader.load();

        // Inyectamos nuestro contenedor simulado al controlador
        AddProductController controller = loader.getController();
        controller.inject(mockContainer);

        // Mostramos la ventana
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    public void testLlenadoDeFormularioYGuardadoExitoso() throws Exception {
        // 1. Escribe en el campo de Nombre
        clickOn("#txtName").write("Tarta de Queso");

        // 2. Escribe en el campo de Precio
        clickOn("#txtPrice").write("15.50");

        // 3. Selecciona la categoría en el ComboBox abriéndolo y pulsando Abajo + Enter
        clickOn("#cmbCategory");
        type(KeyCode.DOWN);
        type(KeyCode.ENTER);

        // 4. Marca el checkbox de favorito
        clickOn("#chkFavorite");

        // 5. Clic en el botón Guardar
        clickOn("Guardar");

        // 6. Verificamos que se llamó al caso de uso de guardar con los datos
        // interceptados
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(mockProductUseCase).saveProduct(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();

        // 7. Aseguramos que los datos recogidos de la UI son los correctos
        assertEquals("Tarta de Queso", savedProduct.getName());
        assertEquals(15.50, savedProduct.getPrice());
        assertEquals(1, savedProduct.getCategoryId());
        assertEquals(true, savedProduct.isFavorite());
    }
}
