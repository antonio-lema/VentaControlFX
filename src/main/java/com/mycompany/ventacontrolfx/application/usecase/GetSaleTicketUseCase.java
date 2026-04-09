package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.repository.ISaleRepository;
import java.sql.SQLException;

/**
 * Caso de Uso para obtener la informaci\u00c3\u00b3n completa de un ticket de venta.
 * Sigue los principios de Clean Architecture al desacoplar la l\u00c3\u00b3gica de
 * obtenci\u00c3\u00b3n
 * de datos de la capa de presentaci\u00c3\u00b3n.
 */
public class GetSaleTicketUseCase {
    private final ISaleRepository saleRepository;

    public GetSaleTicketUseCase(ISaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    /**
     * Obtiene los detalles de una venta junto con sus l\u00c3\u00adneas de detalle
     * formateados para su visualizaci\u00c3\u00b3n.
     * 
     * @param saleId Identificador \u00c3\u00banico de la venta
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
