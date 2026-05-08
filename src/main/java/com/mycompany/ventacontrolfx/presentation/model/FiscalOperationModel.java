package com.mycompany.ventacontrolfx.presentation.model;

import javafx.beans.property.*;

public class FiscalOperationModel {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty type = new SimpleStringProperty(); // ALTA or RECTIFICATIVA
    private final StringProperty document = new SimpleStringProperty();
    private final StringProperty date = new SimpleStringProperty();
    private final DoubleProperty total = new SimpleDoubleProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty error = new SimpleStringProperty();
    private final StringProperty docStatus = new SimpleStringProperty();
    private final StringProperty xmlSent = new SimpleStringProperty();
    private final StringProperty xmlReceived = new SimpleStringProperty();

    public FiscalOperationModel(int id, String type, String document, String date, double total, String status, String error, String xmlSent, String xmlReceived, String docStatus) {
        this.id.set(id);
        this.type.set(type);
        this.document.set(document);
        this.date.set(date);
        this.total.set(total);
        this.status.set(status);
        this.error.set(error);
        this.xmlSent.set(xmlSent);
        this.xmlReceived.set(xmlReceived);
        this.docStatus.set(docStatus);
    }

    public String getDocStatus() { return docStatus.get(); }
    public StringProperty docStatusProperty() { return docStatus; }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getType() { return type.get(); }
    public StringProperty typeProperty() { return type; }

    public String getDocument() { return document.get(); }
    public StringProperty documentProperty() { return document; }

    public String getDate() { return date.get(); }
    public StringProperty dateProperty() { return date; }

    public double getTotal() { return total.get(); }
    public DoubleProperty totalProperty() { return total; }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }

    public String getError() { return error.get(); }
    public StringProperty errorProperty() { return error; }

    public String getXmlSent() { return xmlSent.get(); }
    public StringProperty xmlSentProperty() { return xmlSent; }

    public String getXmlReceived() { return xmlReceived.get(); }
    public StringProperty xmlReceivedProperty() { return xmlReceived; }
}
