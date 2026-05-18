package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.application.usecase.WorkSessionUseCase;
import com.mycompany.ventacontrolfx.domain.model.WorkSession;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.domain.model.UserSession;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.mycompany.ventacontrolfx.application.usecase.CashClosureUseCase;
import com.mycompany.ventacontrolfx.presentation.controller.closure.CashOpeningController;
import com.mycompany.ventacontrolfx.presentation.controller.closure.CashClosingController;
import com.mycompany.ventacontrolfx.presentation.navigation.ModalService;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.File;
import java.io.FileOutputStream;
import javafx.stage.FileChooser;
import java.awt.Color;

public class WorkSessionController implements Injectable {

    @FXML
    private StackPane statusIconContainer;
    @FXML
    private FontAwesomeIconView statusIcon;
    @FXML
    private Label lblCurrentStatus;
    @FXML
    private Label lblSessionTimer;
    @FXML
    private Label lblShiftStart;
    @FXML
    private Label lblTotalWorked;
    @FXML
    private Label lblCashInDrawer;
    @FXML
    private Label lblTodaySales;
    @FXML
    private Label lblCashStatus;

    // --- Audit Details Panel ---
    @FXML
    private VBox panelAuditDetails;
    @FXML
    private Label lblAuditInitial;
    @FXML
    private Label lblAuditExpected;
    @FXML
    private Label lblAuditCounted;
    @FXML
    private Label lblAuditDiff;
    @FXML
    private TableView<AuditMovementViewModel> tableAuditMovements;
    @FXML
    private TableColumn<AuditMovementViewModel, String> colMovTime;
    @FXML
    private TableColumn<AuditMovementViewModel, String> colMovType;
    @FXML
    private TableColumn<AuditMovementViewModel, String> colMovUser;
    @FXML
    private TableColumn<AuditMovementViewModel, String> colMovAmount;

    @FXML
    private Button btnStartShift;
    @FXML
    private Button btnStartBreak;
    @FXML
    private Button btnEndSession;

    @FXML
    private TableView<WorkSessionViewModel> historyTable;
    @FXML
    private TableColumn<WorkSessionViewModel, String> colType;
    @FXML
    private TableColumn<WorkSessionViewModel, String> colStart;
    @FXML
    private TableColumn<WorkSessionViewModel, String> colEnd;
    @FXML
    private TableColumn<WorkSessionViewModel, String> colDuration;
    @FXML
    private TableColumn<WorkSessionViewModel, String> colStatus;

    private WorkSessionUseCase useCase;
    private CashClosureUseCase closureUseCase;
    private com.mycompany.ventacontrolfx.application.usecase.SaleUseCase saleUseCase;
    private com.mycompany.ventacontrolfx.application.usecase.ReturnUseCase returnUseCase;
    private UserSession userSession;
    private ServiceContainer container;
    private Timeline timer;
    private WorkSession activeSession;

    private final ObservableList<WorkSessionViewModel> historyList = FXCollections.observableArrayList();
    private final ObservableList<AuditMovementViewModel> movementList = FXCollections.observableArrayList();
    private List<WorkSession> todaySessionsCache = new java.util.ArrayList<>();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.useCase = container.getWorkSessionUseCase();
        this.closureUseCase = container.getClosureUseCase();
        this.saleUseCase = container.getSaleUseCase();
        this.returnUseCase = container.getReturnUseCase();
        this.userSession = container.getUserSession();
    
        if (historyTable != null) setupTable();
        refreshUI();
        startGlobalTimer();
    }

    private void setupTable() {
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStart.setCellValueFactory(new PropertyValueFactory<>("start"));
        colEnd.setCellValueFactory(new PropertyValueFactory<>("end"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        historyTable.setItems(historyList);

        // Setup selection listener for audit details
        historyTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showAuditDetails(newVal.getOriginalSession());
            } else {
                panelAuditDetails.setVisible(false);
                panelAuditDetails.setManaged(false);
            }
        });

        // Setup Audit Movements Table
        colMovTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colMovType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colMovUser.setCellValueFactory(new PropertyValueFactory<>("user"));
        colMovAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        tableAuditMovements.setItems(movementList);
    }

    private void refreshUI() {
        if (userSession.getCurrentUser() == null)
            return;

        loadHistory();
    }

    @FXML
    private void loadHistory() {
        if (userSession.getCurrentUser() == null)
            return;

        List<WorkSession> history = useCase.getHistory(userSession.getCurrentUser().getUserId());
        
        // Actualizar cache local para el timer
        LocalDate today = LocalDate.now();
        this.todaySessionsCache = history.stream()
                .filter(s -> s.getStartTime().toLocalDate().isEqual(today))
                .collect(Collectors.toList());

        if (historyTable != null) {
            historyList.setAll(history.stream()
                    .map(s -> new WorkSessionViewModel(s, dateTimeFormatter))
                    .collect(Collectors.toList()));
        }

        // Determinar sesión activa
        Optional<WorkSession> sessionOpt = useCase.getActiveSession(userSession.getCurrentUser().getUserId());
        if (sessionOpt.isPresent()) {
            this.activeSession = sessionOpt.get();
            updateActiveStatusUI();
        } else {
            this.activeSession = null;
            updateInactiveStatusUI();
        }
                
        updateDailySummaries();
    }

    private void updateActiveStatusUI() {
        boolean isShift = activeSession.getType() == WorkSession.SessionType.SHIFT;

        statusIconContainer.getStyleClass().removeAll("status-active", "status-break", "status-inactive");
        if (isShift) {
            lblCurrentStatus.setText("EN TURNO");
            statusIconContainer.getStyleClass().add("status-active");
            statusIcon.setGlyphName("USER");
            btnStartShift.setDisable(true);
            btnStartBreak.setDisable(false);
            btnEndSession.setText("FINALIZAR TURNO");
        } else {
            lblCurrentStatus.setText("EN DESCANSO");
            statusIconContainer.getStyleClass().add("status-break");
            statusIcon.setGlyphName("COFFEE");
            btnStartShift.setDisable(false); // Permite "Volver al turno"
            btnStartShift.setText("VOLVER AL TURNO");
            btnStartBreak.setDisable(true);
            btnEndSession.setText("FINALIZAR TODO");
        }

        btnEndSession.setDisable(false);
    }

    private void updateInactiveStatusUI() {
        lblCurrentStatus.setText("FUERA DE TURNO");
        lblSessionTimer.setText("00:00:00");
        statusIconContainer.getStyleClass().removeAll("status-active", "status-break", "status-inactive");
        statusIconContainer.getStyleClass().add("status-inactive");
        statusIcon.setGlyphName("USER_TIMES");

        btnStartShift.setDisable(false);
        btnStartShift.setText("INICIAR TURNO");
        btnStartBreak.setDisable(true);
        btnEndSession.setDisable(true);
        btnEndSession.setText("FINALIZAR");
    }

    private void startGlobalTimer() {
        if (timer != null)
            timer.stop();

        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateTimerLabel()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private int ticksSinceLastFullRefresh = 0;

    private void updateTimerLabel() {
        if (activeSession != null) {
            long totalSeconds = calculateCumulativeTimedSessionFromCache(activeSession.getType());
            lblSessionTimer.setText(formatLongDuration(totalSeconds));
            
            // Actualizar el resumen r\u00e1pido (total trabajado) sin ir a la DB
            updateWorkDurationSummaryFromCache();
            
            // Refrescar datos de caja cada 15 segundos
            ticksSinceLastFullRefresh++;
            if (ticksSinceLastFullRefresh >= 15) {
                updateCashInfo();
                ticksSinceLastFullRefresh = 0;
            }
        } else {
            lblSessionTimer.setText("00:00:00");
        }
    }

    private long calculateCumulativeTimedSessionFromCache(WorkSession.SessionType type) {
        return todaySessionsCache.stream()
                .filter(s -> s.getType() == type)
                .mapToLong(s -> {
                    LocalDateTime end = s.getEndTime() != null ? s.getEndTime() : LocalDateTime.now();
                    return java.time.temporal.ChronoUnit.SECONDS.between(s.getStartTime(), end);
                }).sum();
    }

    @FXML
    private void handleStartShift() {
        try {
            Integer userId = userSession.getCurrentUser().getUserId();
            
            // Si est\u00e1 en descanso, cerramos el descanso
            if (activeSession != null && activeSession.getType() == WorkSession.SessionType.BREAK) {
                useCase.endSession(userId);
            }
            
            // Iniciar turno de empleado
            useCase.startShift(userId);
            
            // L\u00d3GICA DE TPV: Si no hay fondo de caja abierto, preguntamos para abrirlo
            if (!closureUseCase.hasActiveFund()) {
                boolean wantsToOpen = AlertUtil.showConfirmation(
                    "Caja Cerrada", 
                    "\u00bfDeseas abrir la caja ahora?",
                    "No hay una sesi\u00f3n de caja activa. Es necesario abrir caja para registrar ventas en efectivo."
                );
                
                if (wantsToOpen) {
                    ModalService.showTransparentModal(
                        "/view/dialog/cash_opening_dialog.fxml",
                        "Apertura de Caja",
                        container,
                        (CashOpeningController ctrl) -> {
                            ctrl.init(closureUseCase, userSession);
                        });
                }
            }
            
            // Recargar todo tras las posibles ventanas emergentes
            loadHistory();
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML
    private void handleStartBreak() {
        try {
            useCase.startBreak(userSession.getCurrentUser().getUserId());
            refreshUI();
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML
    private void handleEndSession() {
        try {
            Integer userId = userSession.getCurrentUser().getUserId();
            useCase.endSession(userId);
            
            // L\u00d3GICA DE TPV: Si hay fondo de caja, preguntamos si quiere hacer el cierre
            if (closureUseCase.hasActiveFund()) {
                boolean wantsToClose = AlertUtil.showConfirmation(
                    "Cierre de Caja", 
                    "\u00bfDeseas realizar el cierre de caja al finalizar tu turno?",
                    "Hay una sesi\u00f3n de caja activa."
                );
                
                if (wantsToClose) {
                    CashClosingController ctrl = ModalService.showTransparentModal(
                        "/view/dialog/cash_closing_dialog.fxml",
                        "Cierre de Caja", 
                        container,
                        (CashClosingController controller) -> {
                            controller.init(closureUseCase, userSession);
                        });
                }
            }
            
            refreshUI();
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML
    private void handlePartialClosure() {
        WorkSessionViewModel selected = historyTable.getSelectionModel().getSelectedItem();
        
        if (selected != null) {
            WorkSession session = selected.getOriginalSession();
            if (session.getType() == WorkSession.SessionType.SHIFT) {
                generateShiftReport(session);
            } else {
                AlertUtil.showInfo("Informe", "No se pueden generar informes detallados para periodos de descanso.");
            }
        } else {
            generatePartialClosureReport();
        }
    }

    private void showAuditDetails(WorkSession session) {
        if (session.getType() != WorkSession.SessionType.SHIFT) {
            panelAuditDetails.setVisible(false);
            panelAuditDetails.setManaged(false);
            return;
        }

        panelAuditDetails.setVisible(true);
        panelAuditDetails.setManaged(true);

        container.getAsyncManager().runAsyncTask(() -> {
            try {
                LocalDateTime start = session.getStartTime();
                LocalDateTime end = session.getEndTime() != null ? session.getEndTime() : LocalDateTime.now();
                
                // 1. Fetch Sales and Returns for this shift
                List<com.mycompany.ventacontrolfx.domain.model.Sale> sales = saleUseCase.getSalesByUserAndRange(session.getUserId(), start, end);
                List<com.mycompany.ventacontrolfx.domain.model.Return> returns = returnUseCase.getReturnsByUserAndRange(session.getUserId(), start, end);
                
                // 2. Fetch manual movements
                List<com.mycompany.ventacontrolfx.domain.repository.ICashClosureRepository.CashMovement> manualMoves = closureUseCase.getMovementsByRange(start, end);
                
                // 3. Try to get closure data for financial summary
                // We'll look for a closure on the session's date
                List<com.mycompany.ventacontrolfx.domain.model.CashClosure> closures = closureUseCase.getHistory(start.toLocalDate(), start.toLocalDate());
                com.mycompany.ventacontrolfx.domain.model.CashClosure dayClosure = closures.isEmpty() ? null : closures.get(0);

                return new Object[] { sales, returns, manualMoves, dayClosure };
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, (res) -> {
            Object[] data = (Object[]) res;
            List<com.mycompany.ventacontrolfx.domain.model.Sale> sales = (List<com.mycompany.ventacontrolfx.domain.model.Sale>) data[0];
            List<com.mycompany.ventacontrolfx.domain.model.Return> returns = (List<com.mycompany.ventacontrolfx.domain.model.Return>) data[1];
            List<com.mycompany.ventacontrolfx.domain.repository.ICashClosureRepository.CashMovement> manualMoves = (List<com.mycompany.ventacontrolfx.domain.repository.ICashClosureRepository.CashMovement>) data[2];
            com.mycompany.ventacontrolfx.domain.model.CashClosure dayClosure = (com.mycompany.ventacontrolfx.domain.model.CashClosure) data[3];

            updateAuditUI(sales, returns, manualMoves, dayClosure);
        }, (e) -> AlertUtil.showError("Error", "No se pudieron cargar los detalles de auditoría: " + e.getMessage()));
    }

    private void updateAuditUI(List<com.mycompany.ventacontrolfx.domain.model.Sale> sales, 
                              List<com.mycompany.ventacontrolfx.domain.model.Return> returns,
                              List<com.mycompany.ventacontrolfx.domain.repository.ICashClosureRepository.CashMovement> manualMoves,
                              com.mycompany.ventacontrolfx.domain.model.CashClosure closure) {
        
        if (closure != null) {
            lblAuditInitial.setText(String.format("%.2f €", closure.getInitialFund()));
            lblAuditExpected.setText(String.format("%.2f €", closure.getExpectedCash()));
            lblAuditCounted.setText(String.format("%.2f €", closure.getActualCash()));
            
            double diff = closure.getDifference();
            lblAuditDiff.setText(String.format("%.2f €", diff));
            lblAuditDiff.getStyleClass().removeAll("text-success", "text-danger");
            lblAuditDiff.getStyleClass().add(Math.abs(diff) < 0.01 ? "text-success" : "text-danger");
        } else {
            lblAuditInitial.setText("--");
            lblAuditExpected.setText("--");
            lblAuditCounted.setText("--");
            lblAuditDiff.setText("--");
        }

        // Combine sales and returns into movements
        movementList.clear();
        
        for (com.mycompany.ventacontrolfx.domain.model.Sale s : sales) {
            movementList.add(new AuditMovementViewModel(
                s.getSaleDateTime().format(timeFormatter),
                "VENTA (" + s.getDocSeries() + "-" + s.getDocNumber() + ")",
                s.getUserName() != null ? s.getUserName() : "--", 
                String.format("%.2f €", s.getTotal())
            ));
        }

        for (com.mycompany.ventacontrolfx.domain.model.Return r : returns) {
            movementList.add(new AuditMovementViewModel(
                r.getReturnDatetime().format(timeFormatter),
                "DEVOLUCIÓN",
                r.getUserName() != null ? r.getUserName() : "--",
                String.format("-%.2f €", r.getTotalRefunded())
            ));
        }

        for (com.mycompany.ventacontrolfx.domain.repository.ICashClosureRepository.CashMovement m : manualMoves) {
            movementList.add(new AuditMovementViewModel(
                m.getCreatedAt().format(timeFormatter),
                m.getType() + (m.getReason() != null && !m.getReason().isEmpty() ? " (" + m.getReason() + ")" : ""),
                m.getUsername() != null ? m.getUsername() : "--",
                String.format("%.2f €", m.getAmount())
            ));
        }

        // Sort by time
        movementList.sort((a, b) -> a.getTime().compareTo(b.getTime()));
    }

    private void generateShiftReport(WorkSession session) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Informe de Turno");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF (*.pdf)", "*.pdf"));
        fileChooser.setInitialFileName("Informe_Turno_" + session.getStartTime().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        
        File file = fileChooser.showSaveDialog(null);
        if (file == null) return;

        container.getAsyncManager().runAsyncTask(() -> {
            try {
                LocalDateTime start = session.getStartTime();
                LocalDateTime end = session.getEndTime() != null ? session.getEndTime() : LocalDateTime.now();
                int userId = session.getUserId();

                List<com.mycompany.ventacontrolfx.domain.model.Sale> sales = saleUseCase.getSalesByUserAndRange(userId, start, end);
                List<com.mycompany.ventacontrolfx.domain.model.Return> returns = returnUseCase.getReturnsByUserAndRange(userId, start, end);
                List<com.mycompany.ventacontrolfx.domain.repository.ICashClosureRepository.CashMovement> manualMoves = closureUseCase.getMovementsByUserAndRange(userId, start, end);

                Document document = new Document(PageSize.A4);
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
                Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
                Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
                Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

                Paragraph title = new Paragraph("INFORME DE TURNO DE TRABAJO", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                document.add(title);
                document.add(new Paragraph("\n"));

                document.add(new Paragraph("Empleado: " + session.getUserName(), normalFont));
                document.add(new Paragraph("Inicio: " + start.format(dateTimeFormatter), normalFont));
                document.add(new Paragraph("Fin: " + (session.getEndTime() != null ? session.getEndTime().format(dateTimeFormatter) : "Activo"), normalFont));
                document.add(new Paragraph("\nRESUMEN DE OPERACIONES", subtitleFont));
                
                double totalSales = sales.stream().mapToDouble(s -> s.getTotal()).sum();
                double totalReturns = returns.stream().mapToDouble(r -> r.getTotalRefunded()).sum();
                double netTotal = totalSales - totalReturns;

                document.add(new Paragraph("Total Ventas (" + sales.size() + "): " + String.format("%.2f \u20ac", totalSales), normalFont));
                document.add(new Paragraph("Total Devoluciones (" + returns.size() + "): " + String.format("%.2f \u20ac", totalReturns), normalFont));
                document.add(new Paragraph("Total Neto: " + String.format("%.2f \u20ac", netTotal), boldFont));
                
                // DETALLE DE VENTAS
                document.add(new Paragraph("\nDETALLE DE VENTAS", subtitleFont));
                if (!sales.isEmpty()) {
                    PdfPTable table = new PdfPTable(4);
                    table.setWidthPercentage(100);
                    table.setSpacingBefore(10);
                    table.setWidths(new float[]{1.5f, 2f, 1.5f, 1.5f});

                    String[] headers = {"Hora", "Documento", "Met. Pago", "Total"};
                    for (String h : headers) {
                        PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
                        cell.setBackgroundColor(new Color(230, 230, 230));
                        cell.setPadding(5);
                        table.addCell(cell);
                    }

                    for (com.mycompany.ventacontrolfx.domain.model.Sale s : sales) {
                        table.addCell(new Phrase(s.getSaleDateTime().format(timeFormatter), normalFont));
                        table.addCell(new Phrase(s.getDocSeries() + "-" + s.getDocNumber(), normalFont));
                        table.addCell(new Phrase(s.getPaymentMethod(), normalFont));
                        PdfPCell amountCell = new PdfPCell(new Phrase(String.format("%.2f \u20ac", s.getTotal()), normalFont));
                        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        table.addCell(amountCell);
                    }
                    document.add(table);
                } else {
                    document.add(new Paragraph("No se registraron ventas en este periodo.", normalFont));
                }

                // DETALLE DE DEVOLUCIONES
                document.add(new Paragraph("\nDETALLE DE DEVOLUCIONES", subtitleFont));
                if (!returns.isEmpty()) {
                    PdfPTable table = new PdfPTable(4);
                    table.setWidthPercentage(100);
                    table.setSpacingBefore(10);
                    table.setWidths(new float[]{1.5f, 2f, 2.5f, 1.5f});

                    String[] headers = {"Hora", "Ticket Orig.", "Motivo", "Importe"};
                    for (String h : headers) {
                        PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
                        cell.setBackgroundColor(new Color(230, 230, 230));
                        cell.setPadding(5);
                        table.addCell(cell);
                    }

                    for (com.mycompany.ventacontrolfx.domain.model.Return r : returns) {
                        table.addCell(new Phrase(r.getReturnDatetime().format(timeFormatter), normalFont));
                        table.addCell(new Phrase("ID: " + r.getSaleId(), normalFont)); // Series-Number usually not in return model directly unless fetched
                        table.addCell(new Phrase(r.getReason() != null ? r.getReason() : "--", normalFont));
                        PdfPCell amountCell = new PdfPCell(new Phrase(String.format("-%.2f \u20ac", r.getTotalRefunded()), normalFont));
                        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        table.addCell(amountCell);
                    }
                    document.add(table);
                } else {
                    document.add(new Paragraph("No se registraron devoluciones en este periodo.", normalFont));
                }

                // MOVIMIENTOS DE CAJA
                document.add(new Paragraph("\nMOVIMIENTOS MANUALES DE CAJA", subtitleFont));
                if (manualMoves != null && !manualMoves.isEmpty()) {
                    PdfPTable table = new PdfPTable(3);
                    table.setWidthPercentage(100);
                    table.setSpacingBefore(10);
                    table.setWidths(new float[]{1.5f, 3.5f, 1.5f});

                    String[] headers = {"Hora", "Tipo / Motivo", "Importe"};
                    for (String h : headers) {
                        PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
                        cell.setBackgroundColor(new Color(230, 230, 230));
                        cell.setPadding(5);
                        table.addCell(cell);
                    }

                    for (com.mycompany.ventacontrolfx.domain.repository.ICashClosureRepository.CashMovement m : manualMoves) {
                        table.addCell(new Phrase(m.getCreatedAt().format(timeFormatter), normalFont));
                        table.addCell(new Phrase(m.getType() + (m.getReason() != null ? " (" + m.getReason() + ")" : ""), normalFont));
                        PdfPCell amountCell = new PdfPCell(new Phrase(String.format("%.2f \u20ac", m.getAmount()), normalFont));
                        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        table.addCell(amountCell);
                    }
                    document.add(table);
                } else {
                    document.add(new Paragraph("No se registraron movimientos manuales.", normalFont));
                }

                document.close();
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, (res) -> AlertUtil.showToast("Informe de turno guardado correctamente."),
           (e) -> AlertUtil.showError("Error", "No se pudo generar el informe: " + e.getMessage()));
    }

    private void generatePartialClosureReport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Informe X (Parcial)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF (*.pdf)", "*.pdf"));
        fileChooser.setInitialFileName("InformeX_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        
        File file = fileChooser.showSaveDialog(null);
        if (file == null) return;

        container.getAsyncManager().runAsyncTask(() -> {
            try {
                Map<String, Double> totals = closureUseCase.getTodayTotals();
                double currentCash = closureUseCase.getCurrentCashInDrawer();
                List<com.mycompany.ventacontrolfx.domain.model.ProductSummary> pendingProducts = closureUseCase.getPendingSummary();

                Document document = new Document(PageSize.A4);
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
                Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
                Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
                Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

                Paragraph title = new Paragraph("INFORME X - ESTADO ACTUAL DE CAJA", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                document.add(title);
                document.add(new Paragraph("\n"));

                document.add(new Paragraph("Generado: " + LocalDateTime.now().format(dateTimeFormatter), normalFont));
                document.add(new Paragraph("Usuario: " + userSession.getCurrentUser().getUsername(), normalFont));
                
                document.add(new Paragraph("\nSITUACIÓN FINANCIERA", subtitleFont));
                document.add(new Paragraph("Efectivo en Caj\u00f3n: " + String.format("%.2f \u20ac", currentCash), boldFont));
                document.add(new Paragraph("Ventas Totales (Bruto): " + String.format("%.2f \u20ac", totals.getOrDefault("sales_total", 0.0)), normalFont));
                document.add(new Paragraph("Desglose Efectivo: " + String.format("%.2f \u20ac", totals.getOrDefault("cash", 0.0)), normalFont));
                document.add(new Paragraph("Desglose Tarjeta: " + String.format("%.2f \u20ac", totals.getOrDefault("card", 0.0)), normalFont));
                
                document.add(new Paragraph("\nVENTAS POR PRODUCTO (PENDIENTES DE CIERRE)", subtitleFont));
                if (!pendingProducts.isEmpty()) {
                    PdfPTable table = new PdfPTable(3);
                    table.setWidthPercentage(100);
                    table.setSpacingBefore(10);
                    table.setWidths(new float[]{3f, 1f, 1.5f});

                    String[] headers = {"Producto", "Cantidad", "Total"};
                    for (String h : headers) {
                        PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
                        cell.setBackgroundColor(new Color(230, 230, 230));
                        cell.setPadding(5);
                        table.addCell(cell);
                    }

                    for (com.mycompany.ventacontrolfx.domain.model.ProductSummary p : pendingProducts) {
                        table.addCell(new Phrase(p.getName(), normalFont));
                        table.addCell(new Phrase(String.valueOf(p.getQuantity()), normalFont));
                        PdfPCell amountCell = new PdfPCell(new Phrase(String.format("%.2f \u20ac", p.getTotalAmount()), normalFont));
                        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        table.addCell(amountCell);
                    }
                    document.add(table);
                } else {
                    document.add(new Paragraph("No hay ventas registradas desde el \u00faltimo cierre.", normalFont));
                }

                document.close();
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, (res) -> AlertUtil.showToast("Informe X guardado correctamente."),
           (e) -> AlertUtil.showError("Error", "No se pudo generar el Informe X: " + e.getMessage()));
    }


    private void updateWorkDurationSummaryFromCache() {
        long totalWorkedSeconds = todaySessionsCache.stream()
                .filter(s -> s.getType() == WorkSession.SessionType.SHIFT)
                .mapToLong(s -> {
                    LocalDateTime end = s.getEndTime() != null ? s.getEndTime() : LocalDateTime.now();
                    return java.time.temporal.ChronoUnit.SECONDS.between(s.getStartTime(), end);
                }).sum();
        lblTotalWorked.setText(formatShortDuration(totalWorkedSeconds));
    }

    private void updateDailySummaries() {
        Optional<WorkSession> firstShift = todaySessionsCache.stream()
                .filter(s -> s.getType() == WorkSession.SessionType.SHIFT)
                .min((a, b) -> a.getStartTime().compareTo(b.getStartTime()));

        if (lblShiftStart != null) {
            lblShiftStart.setText(firstShift.map(s -> s.getStartTime().format(timeFormatter)).orElse("--"));
        }
        updateWorkDurationSummaryFromCache();
        updateCashInfo();
    }

    private void updateCashInfo() {
        try {
            boolean hasFund = closureUseCase.hasActiveFund();
            if (lblCashStatus != null) {
                if (hasFund) {
                    lblCashStatus.setText("CAJA ABIERTA");
                    lblCashStatus.getStyleClass().removeAll("status-label-closed");
                    lblCashStatus.getStyleClass().add("status-label-open");
                    lblCashInDrawer.setText(String.format("%.2f €", closureUseCase.getCurrentCashInDrawer()));
                } else {
                    lblCashStatus.setText("CAJA CERRADA");
                    lblCashStatus.getStyleClass().removeAll("status-label-open");
                    lblCashStatus.getStyleClass().add("status-label-closed");
                    lblCashInDrawer.setText("No iniciada");
                }
            }
            
            // Ventas Hoy: Total real del día
            if (lblTodaySales != null) {
                double totalSalesToday = closureUseCase.getTodayTotalSales();
                lblTodaySales.setText(String.format("%.2f €", totalSalesToday));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private long calculateTotalSeconds(List<WorkSession> sessions, WorkSession.SessionType type) {
        return sessions.stream()
                .filter(s -> s.getType() == type)
                .mapToLong(s -> {
                    LocalDateTime end = s.getEndTime() != null ? s.getEndTime() : LocalDateTime.now();
                    return ChronoUnit.SECONDS.between(s.getStartTime(), end);
                }).sum();
    }

    private String formatShortDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        return String.format("%dh %dm", h, m);
    }

    private String formatLongDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private void showAlert(String title, String content) {
        com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showError(title, content);
    }

    @FXML
    private void handleClose() {
        if (lblCurrentStatus.getScene() != null) {
            ((Stage) lblCurrentStatus.getScene().getWindow()).close();
        }
    }

    public static class WorkSessionViewModel {
        private String type;
        private String start;
        private String end;
        private String duration;
        private String status;
        private WorkSession originalSession;

        public WorkSessionViewModel(WorkSession session, DateTimeFormatter formatter) {
            this.originalSession = session;
            this.type = session.getType() == WorkSession.SessionType.SHIFT ? "TURNO" : "DESCANSO";
            this.start = session.getStartTime().format(formatter);
            this.end = session.getEndTime() != null ? session.getEndTime().format(formatter) : "-";

            LocalDateTime endDt = session.getEndTime() != null ? session.getEndTime() : LocalDateTime.now();
            long seconds = ChronoUnit.SECONDS.between(session.getStartTime(), endDt);
            long h = seconds / 3600;
            long m = (seconds % 3600) / 60;
            this.duration = String.format("%dh %dm", h, m);

            this.status = session.getStatus().name();
        }

        public String getType() {
            return type;
        }

        public String getStart() {
            return start;
        }

        public String getEnd() {
            return end;
        }

        public String getDuration() {
            return duration;
        }

        public String getStatus() {
            return status;
        }

        public WorkSession getOriginalSession() {
            return originalSession;
        }
    }

    public static class AuditMovementViewModel {
        private final String time;
        private final String type;
        private final String user;
        private final String amount;

        public AuditMovementViewModel(String time, String type, String user, String amount) {
            this.time = time;
            this.type = type;
            this.user = user;
            this.amount = amount;
        }

        public String getTime() { return time; }
        public String getType() { return type; }
        public String getUser() { return user; }
        public String getAmount() { return amount; }
    }
}


