package com.mycompany.ventacontrolfx;

public class Launcher {
    public static void main(String[] args) {
        // 1. Silenciar loggers internos
        java.util.logging.Logger.getLogger("javafx.scene.CssStyleHelper").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("javafx.css").setLevel(java.util.logging.Level.OFF);
        
        // 2. Filtro maestro de flujo de error (System.err)
        // Esto captura y descarta mensajes que los loggers no pueden frenar
        java.io.PrintStream originalErr = System.err;
        System.setErr(new java.io.PrintStream(new java.io.OutputStream() {
            private StringBuilder line = new StringBuilder();
            @Override
            public void write(int b) {
                if (b == '\n') {
                    String msg = line.toString();
                    // Si NO es un aviso de CSS o de casteo de Double, lo mostramos
                    if (!msg.contains("javafx.scene.CssStyleHelper") && 
                        !msg.contains("javafx.css.Size") &&
                        !msg.contains("java.lang.ClassCastException: class java.lang.Double")) {
                        originalErr.println(msg);
                    }
                    line.setLength(0);
                } else if (b != '\r') {
                    line.append((char) b);
                }
            }
        }));
        
        App.main(args);
    }
}
