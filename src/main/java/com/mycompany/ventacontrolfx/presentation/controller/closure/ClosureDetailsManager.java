package com.mycompany.ventacontrolfx.presentation.controller.closure;

import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.domain.model.UserSession;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Gestor del Panel de Detalles de Cierre.
 * Centraliza la actualización de la UI cuando se selecciona un arqueo.
 */
public class ClosureDetailsManager {

    private final ServiceContainer container;
    private final UserSession userSession;
    private final VBox detailsPanel;
    private final Label lblId, lblInitial, lblSales, lblIn, lblOut, lblExpected, lblActual, lblNotes;
    private final Button btnMarkReviewed, btnEditClosure;

    public ClosureDetailsManager(ServiceContainer container, UserSession session, VBox detailsPanel, 
                                 Label lblId, Label lblInitial, Label lblSales, Label lblIn, Label lblOut, 
                                 Label lblExpected, Label lblActual, Label lblNotes, 
                                 Button btnMarkReviewed, Button btnEditClosure) {
        this.container = container;
        this.userSession = session;
        this.detailsPanel = detailsPanel;
        this.lblId = lblId;
        this.lblInitial = lblInitial;
        this.lblSales = lblSales;
        this.lblIn = lblIn;
        this.lblOut = lblOut;
        this.lblExpected = lblExpected;
        this.lblActual = lblActual;
        this.lblNotes = lblNotes;
        this.btnMarkReviewed = btnMarkReviewed;
        this.btnEditClosure = btnEditClosure;
    }

    public void show(CashClosure closure, ClosureTableManager tableManager) {
        detailsPanel.setVisible(true);
        detailsPanel.setManaged(true);

        lblId.setText("#" + closure.getClosureId());
        lblInitial.setText(String.format("%.2f €", closure.getInitialFund()));
        lblSales.setText(String.format("%.2f €", closure.getTotalCash()));
        lblIn.setText(String.format("%.2f €", closure.getCashIn()));
        lblOut.setText(String.format("%.2f €", closure.getCashOut()));
        lblExpected.setText(String.format("%.2f €", closure.getExpectedCash()));
        lblActual.setText(String.format("%.2f €", closure.getActualCash()));
        lblNotes.setText(closure.getNotes() != null ? closure.getNotes() : "—");

        boolean canReview = userSession.hasPermission("caja.revisar") && closure.getReviewedBy() == null;
        btnMarkReviewed.setDisable(!canReview);
        btnEditClosure.setDisable(!userSession.hasPermission("caja.editar"));

        tableManager.loadMovements(closure.getClosureId());
    }

    public void hide() {
        detailsPanel.setVisible(false);
        detailsPanel.setManaged(false);
    }
}

