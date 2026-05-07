package com.mycompany.ventacontrolfx.application.service;

import com.mycompany.ventacontrolfx.application.usecase.MassivePriceUpdateUseCase;
import com.mycompany.ventacontrolfx.domain.exception.BusinessException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio encargado de despachar las peticiones de actualización masiva de precios
 * hacia el caso de uso correspondiente. 
 * Libera al controlador de la lógica de bifurcación (switch statements).
 */
public class PriceUpdateService {

    private final MassivePriceUpdateUseCase useCase;

    public PriceUpdateService(MassivePriceUpdateUseCase useCase) {
        this.useCase = useCase;
    }

    public enum Grouping {
        ALL, CATEGORY, PRODUCTS, CLONE, TOP, BOTTOM, SLOW, RANGE, FAVORITES
    }

    public enum Operation {
        PERCENTAGE, FIXED, ROUNDING
    }

    public static class Request {
        public Grouping grouping;
        public Operation operation;
        public int priceListId;
        public double value;
        public String reason;
        public LocalDateTime startDate;
        public Object extra; // IDs de productos, ID de categoría, rangos, etc.
    }

    public int execute(Request req) throws Exception {
        if (req.grouping == Grouping.CLONE) {
            Integer sourceId = (Integer) req.extra;
            if (sourceId == null) throw new BusinessException("No se seleccionó tarifa de origen.");
            useCase.cloneAndAdjustPrices(sourceId, req.priceListId, req.value, req.reason, req.startDate);
            return -1;
        }

        return switch (req.operation) {
            case PERCENTAGE -> handlePercentage(req);
            case FIXED -> handleFixed(req);
            case ROUNDING -> handleRounding(req);
        };
    }

    private int handlePercentage(Request req) throws SQLException {
        return switch (req.grouping) {
            case ALL -> useCase.applyPercentageIncreaseToAll(req.priceListId, req.value, req.reason, req.startDate);
            case CATEGORY -> useCase.applyPercentageIncreaseToCategory(req.priceListId, (Integer) req.extra, req.value, req.reason, req.startDate);
            case PRODUCTS -> useCase.applyPercentageIncreaseToProducts(req.priceListId, (List<Integer>) req.extra, req.value, req.reason, req.startDate);
            case TOP -> {
                int[] params = (int[]) req.extra;
                yield useCase.applyToTopSellers(req.priceListId, params[0], params[1], req.value, req.reason, true, req.startDate);
            }
            case BOTTOM -> {
                int[] params = (int[]) req.extra;
                yield useCase.applyToBottomSellers(req.priceListId, params[0], params[1], req.value, req.reason, true, req.startDate);
            }
            case SLOW -> useCase.applyToSlowMovers(req.priceListId, (Integer) req.extra, req.value, req.reason, true, req.startDate);
            case RANGE -> {
                double[] range = (double[]) req.extra;
                yield useCase.applyToPriceRange(req.priceListId, range[0], range[1], req.value, req.reason, true, req.startDate);
            }
            case FAVORITES -> useCase.applyToFavorites(req.priceListId, req.value, req.reason, true, req.startDate);
            default -> throw new UnsupportedOperationException("Agrupación no soportada para porcentaje: " + req.grouping);
        };
    }

    private int handleFixed(Request req) throws SQLException {
        return switch (req.grouping) {
            case ALL -> useCase.applyFixedAmountIncreaseToAll(req.priceListId, req.value, req.reason, req.startDate);
            case CATEGORY -> useCase.applyFixedAmountIncreaseToCategory(req.priceListId, (Integer) req.extra, req.value, req.reason, req.startDate);
            case PRODUCTS -> useCase.applyFixedAmountIncreaseToProducts(req.priceListId, (List<Integer>) req.extra, req.value, req.reason, req.startDate);
            case TOP -> {
                int[] params = (int[]) req.extra;
                yield useCase.applyToTopSellers(req.priceListId, params[0], params[1], req.value, req.reason, false, req.startDate);
            }
            case BOTTOM -> {
                int[] params = (int[]) req.extra;
                yield useCase.applyToBottomSellers(req.priceListId, params[0], params[1], req.value, req.reason, false, req.startDate);
            }
            case SLOW -> useCase.applyToSlowMovers(req.priceListId, (Integer) req.extra, req.value, req.reason, false, req.startDate);
            case RANGE -> {
                double[] range = (double[]) req.extra;
                yield useCase.applyToPriceRange(req.priceListId, range[0], range[1], req.value, req.reason, false, req.startDate);
            }
            case FAVORITES -> useCase.applyToFavorites(req.priceListId, req.value, req.reason, false, req.startDate);
            default -> throw new UnsupportedOperationException("Agrupación no soportada para importe fijo: " + req.grouping);
        };
    }

    private int handleRounding(Request req) throws SQLException {
        return switch (req.grouping) {
            case ALL -> useCase.applyRoundingToAll(req.priceListId, req.value, req.reason, req.startDate);
            case CATEGORY -> useCase.applyRoundingToCategory(req.priceListId, (Integer) req.extra, req.value, req.reason, req.startDate);
            case PRODUCTS -> useCase.applyBulkRoundingToProducts(req.priceListId, (List<Integer>) req.extra, req.value, req.reason, req.startDate);
            case TOP -> {
                int[] params = (int[]) req.extra;
                yield useCase.applyRoundingToTopSellers(req.priceListId, params[0], params[1], req.value, req.reason, req.startDate);
            }
            case BOTTOM -> {
                int[] params = (int[]) req.extra;
                yield useCase.applyRoundingToBottomSellers(req.priceListId, params[0], params[1], req.value, req.reason, req.startDate);
            }
            case SLOW -> useCase.applyRoundingToSlowMovers(req.priceListId, (Integer) req.extra, req.value, req.reason, req.startDate);
            case RANGE -> {
                double[] range = (double[]) req.extra;
                yield useCase.applyRoundingToPriceRange(req.priceListId, range[0], range[1], req.value, req.reason, req.startDate);
            }
            case FAVORITES -> useCase.applyRoundingToFavorites(req.priceListId, req.value, req.reason, req.startDate);
            default -> throw new UnsupportedOperationException("Agrupación no soportada para redondeo: " + req.grouping);
        };
    }
}

