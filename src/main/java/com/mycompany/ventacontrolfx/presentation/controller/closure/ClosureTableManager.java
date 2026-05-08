package com.mycompany.ventacontrolfx.presentation.controller.closure;

import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.domain.repository.ICashClosureRepository.CashMovement;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gestor de Tablas para el Historial de Cierres.
 * Configura el renderizado de celdas y mapeo de datos.
 */
public class ClosureTableManager {

    private final ServiceContainer container;
    private final TableView<CashClosure> tableClosures;
    private final TableView<CashMovement> tableMovements;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ClosureTableManager(ServiceContainer container, TableView<CashClosure> tableClosures, TableView<CashMovement> tableMovements) {
        this.container = container;
        this.tableClosures = tableClosures;
        this.tableMovements = tableMovements;
    }

    public void setup(TableColumn<CashClosure, String> colStatus, TableColumn<CashClosure, String> colCreated, 
                      TableColumn<CashClosure, String> colUser, TableColumn<CashClosure, Double> colInitialFund, 
                      TableColumn<CashClosure, Double> colExpected, TableColumn<CashClosure, Double> colActual, 
                      TableColumn<CashClosure, Double> colDifference) {
        
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colCreated.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCreatedAt().format(fmt)));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colInitialFund.setCellValueFactory(new PropertyValueFactory<>("initialFund"));
        colExpected.setCellValueFactory(new PropertyValueFactory<>("expectedCash"));
        colActual.setCellValueFactory(new PropertyValueFactory<>("actualCash"));
        colDifference.setCellValueFactory(new PropertyValueFactory<>("difference"));

        // Renderizado de estado con colores y traducción dinámica
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    String translated;
                    if (item.contains("DISCREPANCY") || item.contains("DESCUADRE")) {
                        translated = container.getBundle().getString("closure.history.status.offset");
                        setText(translated);
                        setStyle("-fx-text-fill: #e11d48; -fx-font-weight: bold;");
                    } else if (item.contains("REVIEWED") || item.contains("REVISADO")) {
                        translated = container.getBundle().getString("closure.history.status.reviewed");
                        setText(translated);
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    } else if (item.contains("SQUARED") || item.contains("CUADRADO")) {
                        translated = container.getBundle().getString("closure.history.status.squared");
                        setText(translated);
                        setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: #2563eb;");
                    }
                }
            }
        });
    }

    public void setupMovements(TableColumn<CashMovement, String> colType, TableColumn<CashMovement, String> colReason, 
                               TableColumn<CashMovement, String> colUser, TableColumn<CashMovement, LocalDateTime> colCreated, 
                               TableColumn<CashMovement, Double> colAmount) {
        
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colCreated.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));

        colCreated.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(fmt));
            }
        });

        colAmount.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(String.format("%.2f \u20ac", item));
                    setStyle(item < 0 ? "-fx-text-fill: #dc2626; -fx-font-weight: bold;" : "-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                }
            }
        });

        colType.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    if (item.equalsIgnoreCase("SALE") || item.equalsIgnoreCase("IN")) {
                        setText(container.getBundle().getString("closure.history.movement.in"));
                        setStyle("-fx-text-fill: #16a34a; -fx-background-color: #dcfce7; -fx-padding: 2 6; -fx-background-radius: 4;");
                    } else if (item.equalsIgnoreCase("OUT") || item.equalsIgnoreCase("RETURN")) {
                        setText(container.getBundle().getString("closure.history.movement.out"));
                        setStyle("-fx-text-fill: #dc2626; -fx-background-color: #fee2e2; -fx-padding: 2 6; -fx-background-radius: 4;");
                    } else {
                        setText(item);
                        setStyle("");
                    }
                }
            }
        });
    }

    public void loadMovements(int closureId) {
        try {
            var movements = container.getClosureUseCase().getMovementsByClosure(closureId);
            tableMovements.getItems().setAll(movements);
            
            // Ajustar altura dinámica para evitar scroll interno doble
            // Cabecera (~45px) + (n_filas * 40px aprox por fila con fuente 13px)
            double rowHeight = 40.0;
            double headerHeight = 45.0;
            double calculatedHeight = headerHeight + (movements.size() * rowHeight);
            
            // Establecer un mínimo y un máximo razonable para el layout
            calculatedHeight = Math.max(120.0, Math.min(800.0, calculatedHeight));
            
            tableMovements.setPrefHeight(calculatedHeight);
            tableMovements.setMinHeight(calculatedHeight);
            tableMovements.setMaxHeight(calculatedHeight);
            
        } catch (Exception e) { e.printStackTrace(); }
    }
}

