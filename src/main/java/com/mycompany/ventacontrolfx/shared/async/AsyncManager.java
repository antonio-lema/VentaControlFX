package com.mycompany.ventacontrolfx.shared.async;

import javafx.concurrent.Task;
import javafx.application.Platform;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class AsyncManager {

    public static <T> void execute(Task<T> task, Consumer<T> onSuccess) {
        task.setOnSucceeded(e -> {
            if (onSuccess != null)
                onSuccess.accept(task.getValue());
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> {
                // Here we could handle different exception types
                System.err.println("Async Error: " + ex.getMessage());
            });
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public <T> void runAsyncTask(Callable<T> callable, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<T>() {
            @Override
            protected T call() throws Exception {
                return callable.call();
            }
        };

        task.setOnSucceeded(e -> {
            if (onSuccess != null)
                onSuccess.accept(task.getValue());
        });

        task.setOnFailed(e -> {
            if (onError != null)
                onError.accept(task.getException());
            else
                task.getException().printStackTrace();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
