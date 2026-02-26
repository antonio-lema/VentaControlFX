package com.mycompany.ventacontrolfx.service;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Enterprise Event Bus.
 * Uses WeakReferences to prevent memory leaks in JavaFX Controllers.
 * If a Controller is closed and not referenced anymore, the GC will collect it,
 * and this bus will automatically remove its listener.
 */
public class GlobalEventBus {

    public interface DataChangeListener {
        void onDataChanged();
    }

    private final List<WeakReference<DataChangeListener>> listeners = new ArrayList<>();

    public void subscribe(DataChangeListener listener) {
        listeners.add(new WeakReference<>(listener));
    }

    public void publishDataChange() {
        Iterator<WeakReference<DataChangeListener>> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            DataChangeListener listener = iterator.next().get();
            if (listener == null) {
                iterator.remove(); // Auto-cleanup of dead references
            } else {
                listener.onDataChanged();
            }
        }
    }
}
