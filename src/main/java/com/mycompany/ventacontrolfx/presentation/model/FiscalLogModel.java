package com.mycompany.ventacontrolfx.presentation.model;

import javafx.beans.property.*;

public class FiscalLogModel {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty message = new SimpleStringProperty();
    private final StringProperty date = new SimpleStringProperty();
    private final StringProperty xmlSent = new SimpleStringProperty();
    private final StringProperty xmlReceived = new SimpleStringProperty();

    public FiscalLogModel(int id, String status, String message, String date, String xmlSent, String xmlReceived) {
        this.id.set(id);
        this.status.set(status);
        this.message.set(message);
        this.date.set(date);
        this.xmlSent.set(xmlSent);
        this.xmlReceived.set(xmlReceived);
    }

    public int getId() { return id.get(); }
    public String getStatus() { return status.get(); }
    public String getMessage() { return message.get(); }
    public String getDate() { return date.get(); }
    public String getXmlSent() { return xmlSent.get(); }
    public String getXmlReceived() { return xmlReceived.get(); }

    public StringProperty statusProperty() { return status; }
    public StringProperty messageProperty() { return message; }
    public StringProperty dateProperty() { return date; }
}
