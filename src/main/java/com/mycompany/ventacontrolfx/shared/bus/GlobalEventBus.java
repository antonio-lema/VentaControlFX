package com.mycompany.ventacontrolfx.shared.bus;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Enterprise Event Bus.
 * Uses WeakReferences to prevent memory leaks in JavaFX Controllers.
 * Refactored to use a more generic and thread-safe internal architecture.
 */
public class GlobalEventBus {

    // --- Legacy Interfaces (maintained for compatibility) ---
    public interface DataChangeListener { void onDataChanged(); }
    public interface LocaleChangeListener { void onLocaleChanged(); }
    public interface VerifactuIncidentListener {
        void onVerifactuIncidentDetected(List<Integer> affectedSaleIds, List<Integer> affectedReturnIds);
    }
    public interface VerifactuSyncListener {
        void onSyncStarted();
        void onSyncFinished(String result);
    }

    // --- Generic Event System ---
    private final Map<Class<?>, List<WeakReference<Object>>> listenersMap = new ConcurrentHashMap<>();

    /**
     * Generic subscription using an interface class and a listener instance.
     */
    private <T> void addListener(Class<T> eventType, T listener) {
        listenersMap.computeIfAbsent(eventType, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new WeakReference<>(listener));
    }

    /**
     * Generic publishing logic.
     */
    @SuppressWarnings("unchecked")
    private <T> void notifyListeners(Class<T> eventType, Consumer<T> action) {
        List<WeakReference<Object>> refs = listenersMap.get(eventType);
        if (refs == null) return;

        synchronized (refs) {
            Iterator<WeakReference<Object>> iterator = refs.iterator();
            while (iterator.hasNext()) {
                T listener = (T) iterator.next().get();
                if (listener == null) {
                    iterator.remove();
                } else {
                    action.accept(listener);
                }
            }
        }
    }

    // --- Specific Subscriptions (Simplified) ---
    public void subscribe(DataChangeListener listener) {
        addListener(DataChangeListener.class, listener);
    }

    public void subscribeLocale(LocaleChangeListener listener) {
        addListener(LocaleChangeListener.class, listener);
    }

    public void subscribeVerifactu(VerifactuIncidentListener listener) {
        addListener(VerifactuIncidentListener.class, listener);
    }

    public void subscribeVerifactuSync(VerifactuSyncListener listener) {
        addListener(VerifactuSyncListener.class, listener);
    }

    // --- Publishing ---
    public void publishDataChange() {
        notifyListeners(DataChangeListener.class, DataChangeListener::onDataChanged);
    }

    public void publishLocaleChange() {
        notifyListeners(LocaleChangeListener.class, LocaleChangeListener::onLocaleChanged);
    }

    public void publishVerifactuIncident(List<Integer> saleIds, List<Integer> returnIds) {
        notifyListeners(VerifactuIncidentListener.class, l -> l.onVerifactuIncidentDetected(saleIds, returnIds));
    }

    public void publishVerifactuSyncStarted() {
        notifyListeners(VerifactuSyncListener.class, VerifactuSyncListener::onSyncStarted);
    }

    public void publishVerifactuSyncFinished(String result) {
        notifyListeners(VerifactuSyncListener.class, l -> l.onSyncFinished(result));
    }
}
