package com.mycompany.ventacontrolfx.util;

import javafx.concurrent.Task;
import javafx.application.Platform;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

/**
 * Enterprise Task Executor.
 * Handles background execution, clean error reporting, and UI synchronization.
 * Specialized in distinguishing between business validation errors and system
 * failures.
 */
public class AsyncManager {
    private static final String TAG = "AsyncManager";

    /**
     * Executes a task and handles success/failure uniformly.
     */
    public static <T> void execute(Task<T> task, Consumer<T> onSuccess) {
        task.setOnSucceeded(e -> {
            if (onSuccess != null) {
                onSuccess.accept(task.getValue());
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();

            // Ignore cancellation exceptions (common in debounced/cancelled filters)
            if (ex instanceof CancellationException) {
                return;
            }

            // Handle Validation Errors separately (Business Logic)
            if (ex instanceof IllegalArgumentException) {
                AppLogger.warn(TAG, "Validation failed: " + ex.getMessage());
                Platform.runLater(() -> {
                    AlertUtil.showWarning("Validación de Datos", ex.getMessage());
                });
            } else {
                // System Errors (Infrastructure/DB)
                AppLogger.error(TAG, "System operation failed: " + ex.getMessage(), ex);
                Platform.runLater(() -> {
                    AlertUtil.showError("Error de Sistema",
                            "Ocurrió un problema técnico inesperado:\n" + ex.getMessage());
                });
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
