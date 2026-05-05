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

    public interface VerifactuIncidentListener {
        void onVerifactuIncidentDetected(java.util.List<Integer> affectedSaleIds, java.util.List<Integer> affectedReturnIds);
    }

    public interface VerifactuSyncListener {
        void onSyncStarted();
        void onSyncFinished(String result);
    }

    private final List<WeakReference<DataChangeListener>> dataListeners = new ArrayList<>();
    private final List<WeakReference<LocaleChangeListener>> localeListeners = new ArrayList<>();
    private final List<WeakReference<VerifactuIncidentListener>> verifactuListeners = new ArrayList<>();
    private final List<WeakReference<VerifactuSyncListener>> syncListeners = new ArrayList<>();

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

    public void subscribeVerifactu(VerifactuIncidentListener listener) {
        verifactuListeners.add(new WeakReference<>(listener));
    }

    public void publishVerifactuIncident(java.util.List<Integer> saleIds, java.util.List<Integer> returnIds) {
        Iterator<WeakReference<VerifactuIncidentListener>> iterator = verifactuListeners.iterator();
        while (iterator.hasNext()) {
            VerifactuIncidentListener listener = iterator.next().get();
            if (listener == null) {
                iterator.remove();
            } else {
                listener.onVerifactuIncidentDetected(saleIds, returnIds);
            }
        }
    }

    public void subscribeVerifactuSync(VerifactuSyncListener listener) {
        syncListeners.add(new WeakReference<>(listener));
    }

    public void publishVerifactuSyncStarted() {
        Iterator<WeakReference<VerifactuSyncListener>> iterator = syncListeners.iterator();
        while (iterator.hasNext()) {
            VerifactuSyncListener listener = iterator.next().get();
            if (listener == null) {
                iterator.remove();
            } else {
                listener.onSyncStarted();
            }
        }
    }

    public void publishVerifactuSyncFinished(String result) {
        Iterator<WeakReference<VerifactuSyncListener>> iterator = syncListeners.iterator();
        while (iterator.hasNext()) {
            VerifactuSyncListener listener = iterator.next().get();
            if (listener == null) {
                iterator.remove();
            } else {
                listener.onSyncFinished(result);
            }
        }
    }
}
