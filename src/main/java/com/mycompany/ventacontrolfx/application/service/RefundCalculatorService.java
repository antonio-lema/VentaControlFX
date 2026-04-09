package com.mycompany.ventacontrolfx.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Servicio puro de cÃ¡lculo para reembolsos y prorrateos.
 */
public class RefundCalculatorService {

    public BigDecimal[] calculateRefundAmounts(BigDecimal totalRefund, BigDecimal cashPaidOnSale,
            BigDecimal grossTotalOnSale) {
        // Si el total de la venta es 0 (caso raro), devolvemos todo en la moneda por
        // defecto (efectivo)
        if (grossTotalOnSale.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal[] { totalRefund.setScale(2, RoundingMode.HALF_UP), BigDecimal.ZERO };
        }

        // Ratio de efectivo pagado respecto al total bruto
        BigDecimal cashRatio = cashPaidOnSale.divide(grossTotalOnSale, 4, RoundingMode.HALF_UP);

        BigDecimal cashToRefund = totalRefund.multiply(cashRatio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cardToRefund = totalRefund.subtract(cashToRefund).setScale(2, RoundingMode.HALF_UP);

        return new BigDecimal[] { cashToRefund, cardToRefund };
    }
}
