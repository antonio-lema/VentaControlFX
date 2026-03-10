package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.repository.ISaleRepository;
import java.sql.SQLException;

/**
 * Caso de Uso para obtener la información completa de un ticket de venta.
 * Sigue los principios de Clean Architecture al desacoplar la lógica de
 * obtención
 * de datos de la capa de presentación.
 */
public class GetSaleTicketUseCase {
    private final ISaleRepository saleRepository;

    public GetSaleTicketUseCase(ISaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    /**
     * Obtiene los detalles de una venta junto con sus líneas de detalle
     * formateados para su visualización.
     * 
     * @param saleId Identificador único de la venta
     * @return Objeto Sale con todos sus detalles cargados
     * @throws SQLException Si ocurre un error en la persistencia
     */
    public Sale execute(int saleId) throws SQLException {
        Sale sale = saleRepository.getById(saleId);
        if (sale != null) {
            sale.setDetails(saleRepository.getDetailsBySaleId(saleId));
        }
        return sale;
    }
}
