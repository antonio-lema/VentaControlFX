package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.DocumentSeries;
import com.mycompany.ventacontrolfx.domain.repository.IDocumentSeriesRepository;
import java.sql.*;

/**
 * Adaptador JDBC para la gesti\u00f3n de series de numeraci\u00f3n correlativa.
 * Clean Architecture \u2014 Capa de Infraestructura.
 *
 * GARANT\u00cda DE ATOMICIDAD:
 * getAndIncrement usa SELECT ... FOR UPDATE dentro de la transacci\u00f3n
 * del llamador, lo que bloquea la fila en el motor InnoDB hasta el COMMIT
 * y evita que dos instancias simult\u00e1neas obtengan el mismo n\u00famero.
 */
public class JdbcDocumentSeriesRepository implements IDocumentSeriesRepository {

    @Override
    public int getAndIncrement(String seriesCode, Connection conn) throws SQLException {
        // 1. Bloquear la fila de la serie para lectura exclusiva
        String lockSql = "SELECT last_number FROM doc_series WHERE series_code = ? FOR UPDATE";
        int currentLast;
        try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
            ps.setString(1, seriesCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Serie de documentos no encontrada: " + seriesCode);
                }
                currentLast = rs.getInt("last_number");
            }
        }

        // 2. Incrementar el contador en la misma transacci\u00f3n
        int nextNumber = currentLast + 1;
        String updateSql = "UPDATE doc_series SET last_number = ? WHERE series_code = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setInt(1, nextNumber);
            ps.setString(2, seriesCode);
            ps.executeUpdate();
        }

        return nextNumber;
    }

    @Override
    public DocumentSeries findByCode(String seriesCode) throws SQLException {
        String sql = "SELECT * FROM doc_series WHERE series_code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seriesCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DocumentSeries ds = new DocumentSeries();
                    ds.setSeriesId(rs.getInt("series_id"));
                    ds.setSeriesCode(rs.getString("series_code"));
                    ds.setPrefix(rs.getString("prefix"));
                    ds.setLastNumber(rs.getInt("last_number"));
                    ds.setYear(rs.getInt("year"));
                    ds.setDescription(rs.getString("description"));
                    return ds;
                }
            }
        }
        throw new SQLException("Serie no encontrada: " + seriesCode);
    }
}

