package com.mycompany.ventacontrolfx.shared.bus;

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

    public interface LocaleChangeListener {
        void onLocaleChanged();
    }

    private final List<WeakReference<DataChangeListener>> dataListeners = new ArrayList<>();
    private final List<WeakReference<LocaleChangeListener>> localeListeners = new ArrayList<>();

    public void subscribe(DataChangeListener listener) {
        dataListeners.add(new WeakReference<>(listener));
    }

    public void subscribeLocale(LocaleChangeListener listener) {
        localeListeners.add(new WeakReference<>(listener));
    }

    public void publishDataChange() {
        Iterator<WeakReference<DataChangeListener>> iterator = dataListeners.iterator();
        while (iterator.hasNext()) {
            DataChangeListener listener = iterator.next().get();
            if (listener == null) {
                iterator.remove();
            } else {
                listener.onDataChanged();
            }
        }
    }

    public void publishLocaleChange() {
        Iterator<WeakReference<LocaleChangeListener>> iterator = localeListeners.iterator();
        while (iterator.hasNext()) {
            LocaleChangeListener listener = iterator.next().get();
            if (listener == null) {
                iterator.remove();
            } else {
                listener.onLocaleChanged();
            }
        }
    }
}
