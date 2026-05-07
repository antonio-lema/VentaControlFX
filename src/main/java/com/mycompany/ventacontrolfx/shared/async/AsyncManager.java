package com.mycompany.ventacontrolfx.shared.async;

import javafx.concurrent.Task;
import javafx.application.Platform;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AsyncManager {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("AsyncManager-Thread");
                return t;
            });

    public static <T> void execute(Task<T> task, Consumer<T> onSuccess) {
        task.setOnSucceeded(e -> {
            if (onSuccess != null)
                onSuccess.accept(task.getValue());
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> {
                System.err.println("Async Error: " + ex.getMessage());
            });
        });

        EXECUTOR.submit(task);
    }

    public <T> void runAsyncTask(Callable<T> callable, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<T>() {
            @Override
            protected T call() throws Exception {
                return callable.call();
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                if (onSuccess != null)
                    onSuccess.accept(task.getValue());
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                if (onError != null)
                    onError.accept(task.getException());
                else
                    task.getException().printStackTrace();
            });
        });

        EXECUTOR.submit(task);
    }

    public void shutdown() {
        EXECUTOR.shutdown();
    }
}

