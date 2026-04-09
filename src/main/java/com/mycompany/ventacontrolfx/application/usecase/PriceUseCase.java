package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.application.dto.PriceInfoDTO;
import com.mycompany.ventacontrolfx.domain.exception.BusinessException;
import com.mycompany.ventacontrolfx.domain.model.Price;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.domain.repository.IPriceRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

public class PriceUseCase {

    private final IPriceRepository priceRepository;

    public PriceUseCase(IPriceRepository priceRepository) {
        this.priceRepository = priceRepository;
    }

    /**
     * Recupera el precio vigente para un producto.
     * Si no se especifica fecha, usa la actual.
     */
    public PriceInfoDTO getActivePriceInfo(int productId, int priceListId, LocalDateTime targetDate)
            throws SQLException {
        LocalDateTime date = (targetDate != null) ? targetDate : LocalDateTime.now();

        Optional<Price> priceOpt = priceRepository.getActivePrice(productId, priceListId);

        if (priceOpt.isEmpty()) {
            throw new BusinessException("No se encontr\u00c3\u00b3 un precio activo para el producto ID: " + productId);
        }

        Price p = priceOpt.get();
        // Nota: En una implementaci\u00c3\u00b3n m\u00c3\u00a1s completa, buscar\u00c3\u00adamos el nombre de la lista.
        // Por ahora simplificamos o usamos un valor fijo si solo hay una lista inicial.
        return new PriceInfoDTO(
                p.getValue(),
                "Lista #" + priceListId, // Podr\u00c3\u00adamos cargar el nombre real desde el repo
                p.getStartDate(),
                p.getReason(),
                p.isActiveAt(date));
    }

    /**
     * Recupera el precio actualmente vigente (entidad completa).
     */
    public Optional<Price> getActivePrice(int productId, int priceListId) throws SQLException {
        return priceRepository.getActivePrice(productId, priceListId);
    }

    /**
     * Actualiza el precio de un producto.
     * Cierra el actual y abre uno nuevo de forma at\u00c3\u00b3mica.
     */
    public void updateProductPrice(int productId, int priceListId, double newValue, String reason,
            LocalDateTime effectiveFrom) throws SQLException {
        // 1. Validaciones b\u00c3\u00a1sicas
        if (newValue < 0) {
            throw new BusinessException("El precio no puede ser negativo.");
        }

        LocalDateTime startDate = (effectiveFrom != null) ? effectiveFrom : LocalDateTime.now();

        // 2. Crear nueva entidad de precio
        Price newPrice = new Price(productId, priceListId, newValue, reason);
        newPrice.setStartDate(startDate);

        // 3. Persistencia at\u00c3\u00b3mica (Cerrar anterior + Insertar nuevo)
        priceRepository.updateCurrentAndSave(newPrice);

        // 4. MANTENIMIENTO LACY: Actualizar campo denormalizado en la tabla products
        // Esto mantiene la lectura dual funcionando hasta que eliminemos el campo
        // antiguo.
        updateLegacyProductPrice(productId, newValue);
    }

    /**
     * Sincroniza el precio con la columna antigua de la tabla products.
     * Esto es temporal seg\u00c3\u00ban la estrategia Expand & Contract.
     */
    private void updateLegacyProductPrice(int productId, double price) throws SQLException {
        // En una arquitectura ideal, esto lo har\u00c3\u00ada un Event Handler,
        // pero por simplicidad de migraci\u00c3\u00b3n lo ejecutamos aqu\u00c3\u00ad.
        // Nota: Requerir\u00c3\u00ada acceso al ProductRepository.
        // Si no queremos inyectar otro repo, podemos usar una query directa sencilla.
        try (java.sql.Connection conn = com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection
                .getConnection()) {
            String sql = "UPDATE products SET price = ? WHERE product_id = ?";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, price);
                ps.setInt(2, productId);
                try {
                    ps.executeUpdate();
                } catch (java.sql.SQLException e) {
                    // Ignore, column price no longer exists in newer versions
                }
            }
        }
    }
}
