package com.mycompany.ventacontrolfx;

import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.api.FxAssert;
import org.testfx.matcher.control.LabeledMatchers;

public class LoginUITest extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        App app = new App();
        app.start(stage);
    }

    @Test
    public void testLoginConCredencialesInvalidas() {
        // Simula al usuario haciendo clic en el campo de texto y escribiendo
        clickOn("#txtUsername").write("usuario_inventado");
        clickOn("#txtPassword").write("clave_falsa");

        // Hace clic en el botón de Iniciar Sesión por su ID
        clickOn("#btnLogin");

        // Verifica que el Label muestra el mensaje esperado de error
        FxAssert.verifyThat("#lblMessage", LabeledMatchers.hasText("Usuario o contraseña incorrectos ❌"));
    }
}
