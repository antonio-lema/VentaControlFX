package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.repository.ISaleRepository;
import java.sql.SQLException;

/**
 * Caso de Uso para obtener la informaciÃ³n completa de un ticket de venta.
 * Sigue los principios de Clean Architecture al desacoplar la lÃ³gica de
 * obtenciÃ³n
 * de datos de la capa de presentaciÃ³n.
 */
public class GetSaleTicketUseCase {
    private final ISaleRepository saleRepository;

    public GetSaleTicketUseCase(ISaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    /**
     * Obtiene los detalles de una venta junto con sus lÃ­neas de detalle
     * formateados para su visualizaciÃ³n.
     * 
     * @param saleId Identificador Ãºnico de la venta
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
