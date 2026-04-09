package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.DocumentSeries;
import com.mycompany.ventacontrolfx.domain.repository.IDocumentSeriesRepository;
import java.sql.*;

/**
 * Adaptador JDBC para la gestiÃ³n de series de numeraciÃ³n correlativa.
 * Clean Architecture â€” Capa de Infraestructura.
 *
 * GARANTÃA DE ATOMICIDAD:
 * getAndIncrement usa SELECT ... FOR UPDATE dentro de la transacciÃ³n
 * del llamador, lo que bloquea la fila en el motor InnoDB hasta el COMMIT
 * y evita que dos instancias simultÃ¡neas obtengan el mismo nÃºmero.
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

        // 2. Incrementar el contador en la misma transacciÃ³n
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
