package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.SaleDetail;
import com.mycompany.ventacontrolfx.domain.repository.ISaleRepository;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class JdbcSaleRepository implements ISaleRepository {

    private static final String INSERT_SALE = "INSERT INTO sales (user_id, client_id, total, payment_method, iva, sale_datetime, is_return, doc_type, doc_series, doc_number, doc_status, control_hash, total_net, total_tax, customer_name_snapshot, customer_nif_snapshot, discount_amount, discount_reason, cash_amount, card_amount, observations, promo_code, reward_promo_code, prev_hash, signature, fiscal_status, fiscal_msg, aeat_submission_id, gen_timestamp, is_correction, correction_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String INSERT_DETAIL = "INSERT INTO sale_details (sale_id, product_id, quantity, unit_price, line_total, iva_rate, iva_amount, product_name_snapshot, net_unit_price, tax_basis, tax_amount, gross_total, applied_tax_group, sku_snapshot, category_name_snapshot, observations) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String INSERT_TAX_SUMMARY = "INSERT INTO sale_tax_summary (sale_id, tax_rate_id, tax_name, tax_rate, tax_basis, tax_amount) VALUES (?, ?, ?, ?, ?, ?)";

    public JdbcSaleRepository() {
    }

    @Override
    public int saveSale(Sale sale) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return saveSale(sale, conn);
        }
    }

    @Override
    public int saveSale(Sale sale, Connection conn) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_SALE, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, sale.getUserId());
            if (sale.getClientId() != null && sale.getClientId() > 0) {
                pstmt.setInt(2, sale.getClientId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setDouble(3, sale.getTotal());
            pstmt.setString(4, sale.getPaymentMethod());
            pstmt.setDouble(5, sale.getIva());
            pstmt.setTimestamp(6, Timestamp.valueOf(sale.getSaleDateTime()));
            pstmt.setBoolean(7, sale.isReturn());
            pstmt.setString(8, sale.getDocType() != null ? sale.getDocType() : "TICKET");
            pstmt.setString(9, sale.getDocSeries());
            if (sale.getDocNumber() != null) {
                pstmt.setInt(10, sale.getDocNumber());
            } else {
                pstmt.setNull(10, Types.INTEGER);
            }
            pstmt.setString(11, sale.getDocStatus() != null ? sale.getDocStatus() : "ISSUED");
            pstmt.setString(12, sale.getControlHash());
            pstmt.setDouble(13, sale.getTotalNet());
            pstmt.setDouble(14, sale.getTotalTax());
            pstmt.setString(15, sale.getCustomerNameSnapshot());
            pstmt.setString(16, sale.getCustomerNifSnapshot());
            pstmt.setDouble(17, sale.getDiscountAmount());
            pstmt.setString(18, sale.getDiscountReason());
            pstmt.setDouble(19, sale.getCashAmount());
            pstmt.setDouble(20, sale.getCardAmount());
            pstmt.setString(21, sale.getObservations());
            pstmt.setString(22, sale.getPromoCode());
            pstmt.setString(23, sale.getRewardPromoCode());

            // VeriFactu Fields
            pstmt.setString(24, sale.getPrevHash());
            pstmt.setString(25, sale.getSignature());
            pstmt.setString(26, sale.getFiscalStatus() != null ? sale.getFiscalStatus() : "PENDING");
            pstmt.setString(27, sale.getFiscalMsg());
            pstmt.setString(28, sale.getAeatSubmissionId());
            pstmt.setString(29, sale.getGenTimestamp());
            pstmt.setBoolean(30, sale.isCorrection());
            pstmt.setString(31, sale.getCorrectionType());
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    sale.setSaleId(id);
                    return id;
                } else {
                    throw new SQLException("ERROR_SALE_ID_NOT_GENERATED");
                }
            }
        }
    }

    @Override
    public void saveSaleDetails(List<SaleDetail> details, int saleId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            saveSaleDetails(details, saleId, conn);
        }
    }

    @Override
    public void saveSaleDetails(List<SaleDetail> details, int saleId, Connection conn) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_DETAIL)) {
            for (SaleDetail detail : details) {
                pstmt.setInt(1, saleId);
                pstmt.setInt(2, detail.getProductId());
                pstmt.setInt(3, detail.getQuantity());
                pstmt.setDouble(4, detail.getUnitPrice());
                pstmt.setDouble(5, detail.getLineTotal());
                pstmt.setDouble(6, detail.getIvaRate());
                pstmt.setDouble(7, detail.getIvaAmount());
                pstmt.setString(8, detail.getProductName());
                pstmt.setDouble(9, detail.getNetUnitPrice());
                pstmt.setDouble(10, detail.getTaxBasis());
                pstmt.setDouble(11, detail.getTaxAmount());
                pstmt.setDouble(12, detail.getGrossTotal());
                pstmt.setString(13, detail.getAppliedTaxGroup());
                pstmt.setString(14, detail.getSkuSnapshot());
                pstmt.setString(15, detail.getCategoryNameSnapshot());
                pstmt.setString(16, detail.getObservations());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    @Override
    public void saveSaleTaxSummaries(List<com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary> summaries, int saleId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            saveSaleTaxSummaries(summaries, saleId, conn);
        }
    }

    @Override
    public void saveSaleTaxSummaries(List<com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary> summaries, int saleId, Connection conn) throws SQLException {
        if (summaries == null || summaries.isEmpty()) return;
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_TAX_SUMMARY)) {
            for (com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary summary : summaries) {
                pstmt.setInt(1, saleId);
                pstmt.setInt(2, summary.getTaxRateId());
                pstmt.setString(3, summary.getTaxName());
                pstmt.setDouble(4, summary.getTaxRate());
                pstmt.setDouble(5, summary.getTaxBasis());
                pstmt.setDouble(6, summary.getTaxAmount());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    @Override
    public List<SaleDetail> getDetailsBySaleId(int saleId) throws SQLException {
        List<SaleDetail> details = new ArrayList<>();
        String sql = "SELECT * FROM sale_details WHERE sale_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, saleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    details.add(mapResultSetToDetail(rs));
                }
            }
        }
        return details;
    }

    @Override
    public List<com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary> getTaxSummariesBySaleId(int saleId) throws SQLException {
        List<com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary> summaries = new ArrayList<>();
        String sql = "SELECT * FROM sale_tax_summary WHERE sale_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, saleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    summaries.add(mapResultSetToTaxSummary(rs));
                }
            }
        }
        return summaries;
    }

    @Override
    public Sale getById(int saleId) throws SQLException {
        String sql = "SELECT s.*, u.username FROM sales s LEFT JOIN users u ON s.user_id = u.user_id WHERE s.sale_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, saleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Sale sale = mapResultSetToSale(rs);
                    sale.setDetails(getDetailsBySaleId(sale.getSaleId()));
                    sale.setTaxSummaries(getTaxSummariesBySaleId(sale.getSaleId()));
                    return sale;
                }
            }
        }
        return null;
    }

    @Override
    public List<Sale> getByRange(LocalDate start, LocalDate end) throws SQLException {
        return getByRange(start, end, Integer.MAX_VALUE);
    }

    @Override
    public List<Sale> getByRange(LocalDate start, LocalDate end, int limit) throws SQLException {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT s.*, u.username FROM sales s LEFT JOIN users u ON s.user_id = u.user_id WHERE s.sale_datetime >= ? AND s.sale_datetime <= ? ORDER BY s.sale_datetime DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, java.sql.Timestamp.valueOf(start.atStartOfDay()));
            pstmt.setTimestamp(2, java.sql.Timestamp.valueOf(end.atTime(java.time.LocalTime.MAX)));
            pstmt.setInt(3, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapResultSetToSale(rs));
                }
            }
        }
        return sales;
    }

    @Override
    public List<Sale> getByClosureId(int closureId) throws SQLException {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT s.*, u.username FROM sales s LEFT JOIN users u ON s.user_id = u.user_id WHERE s.closure_id = ? ORDER BY s.sale_datetime ASC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, closureId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapResultSetToSale(rs));
                }
            }
        }
        return sales;
    }

    @Override
    public List<Sale> getByUserAndRange(int userId, LocalDateTime start, LocalDateTime end) throws SQLException {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT s.*, u.username FROM sales s LEFT JOIN users u ON s.user_id = u.user_id WHERE s.user_id = ? AND s.sale_datetime >= ? AND s.sale_datetime <= ? ORDER BY s.sale_datetime ASC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setTimestamp(2, java.sql.Timestamp.valueOf(start));
            pstmt.setTimestamp(3, java.sql.Timestamp.valueOf(end));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapResultSetToSale(rs));
                }
            }
        }
        return sales;
    }

    @Override
    public com.mycompany.ventacontrolfx.domain.model.HistoryStats getStatsByRange(LocalDate start, LocalDate end) throws SQLException {
        String sql = "SELECT COUNT(*) as total_count, SUM(total - COALESCE(returned_amount, 0)) as net_total, SUM(CASE WHEN is_return = 1 THEN 0 ELSE cash_amount END) as raw_cash, SUM(CASE WHEN is_return = 1 THEN 0 ELSE card_amount END) as raw_card FROM sales WHERE sale_datetime >= ? AND sale_datetime <= ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, java.sql.Timestamp.valueOf(start.atStartOfDay()));
            pstmt.setTimestamp(2, java.sql.Timestamp.valueOf(end.atTime(java.time.LocalTime.MAX)));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new com.mycompany.ventacontrolfx.domain.model.HistoryStats(
                            rs.getInt("total_count"),
                            rs.getDouble("net_total"),
                            rs.getDouble("raw_cash"),
                            rs.getDouble("raw_card"));
                }
            }
        }
        return new com.mycompany.ventacontrolfx.domain.model.HistoryStats(0, 0, 0, 0);
    }

    @Override
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM sales";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    @Override
    public List<com.mycompany.ventacontrolfx.domain.model.ProductSummary> getTopProductsByClient(int clientId, int limit) throws SQLException {
        List<com.mycompany.ventacontrolfx.domain.model.ProductSummary> list = new ArrayList<>();
        String sql = "SELECT sd.product_name_snapshot as name, SUM(sd.quantity) as total_qty, SUM(sd.line_total) as total_amount FROM sale_details sd JOIN sales s ON sd.sale_id = s.sale_id WHERE s.client_id = ? AND (s.is_return = 0 OR s.is_return IS NULL) GROUP BY sd.product_name_snapshot ORDER BY total_qty DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, clientId);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new com.mycompany.ventacontrolfx.domain.model.ProductSummary(rs.getString("name"), rs.getInt("total_qty"), rs.getDouble("total_amount")));
                }
            }
        }
        return list;
    }

    @Override
    public List<com.mycompany.ventacontrolfx.domain.model.ClientSaleSummary> getClientSalesSummary(LocalDate start, LocalDate end) throws SQLException {
        List<com.mycompany.ventacontrolfx.domain.model.ClientSaleSummary> list = new ArrayList<>();
        String sql = "SELECT client_id, COUNT(sale_id) as orders, SUM(total) as spent, MAX(sale_datetime) as last_date FROM sales WHERE sale_datetime >= ? AND sale_datetime <= ? AND (is_return = 0 OR is_return IS NULL) AND client_id IS NOT NULL GROUP BY client_id";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, java.sql.Timestamp.valueOf(start.atStartOfDay()));
            pstmt.setTimestamp(2, java.sql.Timestamp.valueOf(end.atTime(java.time.LocalTime.MAX)));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("last_date");
                    java.math.BigDecimal spentBD = rs.getBigDecimal("spent");
                    double spent = (spentBD != null) ? spentBD.doubleValue() : 0.0;
                    list.add(new com.mycompany.ventacontrolfx.domain.model.ClientSaleSummary(rs.getInt("client_id"), rs.getInt("orders"), spent, ts != null ? ts.toLocalDateTime() : null));
                }
            }
        }
        return list;
    }

    @Override
    public List<Sale> getByClient(int clientId) throws SQLException {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT s.*, u.username FROM sales s LEFT JOIN users u ON s.user_id = u.user_id WHERE s.client_id = ? AND (s.is_return = 0 OR s.is_return IS NULL) ORDER BY s.sale_datetime DESC LIMIT 200";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, clientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapResultSetToSale(rs));
                }
            }
        }
        return sales;
    }

    @Override
    public Map<String, Double> getCategoryDistribution(LocalDate start, LocalDate end) throws SQLException {
        Map<String, Double> dist = new HashMap<>();
        String sql = "SELECT sd.category_name_snapshot, SUM(sd.line_total) FROM sale_details sd JOIN sales s ON sd.sale_id = s.sale_id WHERE s.sale_datetime >= ? AND s.sale_datetime <= ? GROUP BY sd.category_name_snapshot";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, java.sql.Timestamp.valueOf(start.atStartOfDay()));
            pstmt.setTimestamp(2, java.sql.Timestamp.valueOf(end.atTime(java.time.LocalTime.MAX)));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    dist.put(rs.getString(1), rs.getDouble(2));
                }
            }
        }
        return dist;
    }

    @Override
    public Map<Integer, Integer> getHourlyDistribution(LocalDate start, LocalDate end) throws SQLException {
        Map<Integer, Integer> dist = new HashMap<>();
        String sql = "SELECT HOUR(sale_datetime), COUNT(*) FROM sales WHERE sale_datetime >= ? AND sale_datetime <= ? GROUP BY HOUR(sale_datetime)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, java.sql.Timestamp.valueOf(start.atStartOfDay()));
            pstmt.setTimestamp(2, java.sql.Timestamp.valueOf(end.atTime(java.time.LocalTime.MAX)));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    dist.put(rs.getInt(1), rs.getInt(2));
                }
            }
        }
        return dist;
    }

    @Override
    public String getLastControlHash(String docSeries) throws SQLException {
        // Unificamos la cadena Verifactu: buscamos el \u00faltimo hash de CUALQUIER serie
        // para evitar errores [2007] al iniciar series nuevas (como la R de devoluciones)
        String sql = "SELECT control_hash FROM (SELECT control_hash, sale_datetime as dt FROM sales UNION ALL SELECT control_hash, return_datetime as dt FROM returns) combined ORDER BY dt DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return null;
    }

    @Override
    public void updateCorrectionData(int saleId, String newName, String newNif, boolean isCorrection, String correctionType, Connection conn) throws SQLException {
        String sql = "UPDATE sales SET customer_name_snapshot = ?, customer_nif_snapshot = ?, is_correction = ?, correction_type = ? WHERE sale_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName);
            pstmt.setString(2, newNif);
            pstmt.setBoolean(3, isCorrection);
            pstmt.setString(4, correctionType);
            pstmt.setInt(5, saleId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateFiscalStatus(int saleId, String status, String message, Connection conn) throws SQLException {
        String sql = "UPDATE sales SET fiscal_status = ?, fiscal_msg = ? WHERE sale_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, message);
            pstmt.setInt(3, saleId);
            pstmt.executeUpdate();
        }
    }

    private Sale mapResultSetToSale(ResultSet rs) throws SQLException {
        Sale sale = new Sale();
        sale.setSaleId(rs.getInt("sale_id"));
        Timestamp ts = rs.getTimestamp("sale_datetime");
        if (ts != null) sale.setSaleDateTime(ts.toLocalDateTime());
        sale.setUserId(rs.getInt("user_id"));
        sale.setUserName(rs.getString("username"));
        sale.setClientId((Integer) rs.getObject("client_id"));
        sale.setTotal(rs.getDouble("total"));
        sale.setPaymentMethod(rs.getString("payment_method"));
        sale.setIva(rs.getDouble("iva"));
        sale.setReturn(rs.getBoolean("is_return"));
        sale.setDocType(rs.getString("doc_type"));
        sale.setDocSeries(rs.getString("doc_series"));
        sale.setDocNumber((Integer) rs.getObject("doc_number"));
        sale.setDocStatus(rs.getString("doc_status"));
        sale.setControlHash(rs.getString("control_hash"));
        sale.setTotalNet(rs.getDouble("total_net"));
        sale.setTotalTax(rs.getDouble("total_tax"));
        sale.setCustomerNameSnapshot(rs.getString("customer_name_snapshot"));
        sale.setCustomerNifSnapshot(rs.getString("customer_nif_snapshot"));
        sale.setDiscountAmount(rs.getDouble("discount_amount"));
        sale.setDiscountReason(rs.getString("discount_reason"));
        sale.setCashAmount(rs.getDouble("cash_amount"));
        sale.setCardAmount(rs.getDouble("card_amount"));
        sale.setObservations(rs.getString("observations"));
        sale.setPromoCode(rs.getString("promo_code"));
        sale.setRewardPromoCode(rs.getString("reward_promo_code"));
        sale.setPrevHash(rs.getString("prev_hash"));
        sale.setSignature(rs.getString("signature"));
        sale.setFiscalStatus(rs.getString("fiscal_status"));
        sale.setFiscalMsg(rs.getString("fiscal_msg"));
        sale.setAeatSubmissionId(rs.getString("aeat_submission_id"));
        sale.setGenTimestamp(rs.getString("gen_timestamp"));
        sale.setCorrection(rs.getBoolean("is_correction"));
        sale.setCorrectionType(rs.getString("correction_type"));
        return sale;
    }

    private SaleDetail mapResultSetToDetail(ResultSet rs) throws SQLException {
        SaleDetail d = new SaleDetail();
        d.setDetailId(rs.getInt("detail_id"));
        d.setSaleId(rs.getInt("sale_id"));
        d.setProductId(rs.getInt("product_id"));
        d.setQuantity(rs.getInt("quantity"));
        d.setUnitPrice(rs.getDouble("unit_price"));
        d.setLineTotal(rs.getDouble("line_total"));
        d.setIvaRate(rs.getDouble("iva_rate"));
        d.setIvaAmount(rs.getDouble("iva_amount"));
        d.setProductName(rs.getString("product_name_snapshot"));
        d.setReturnedQuantity(rs.getInt("returned_quantity"));
        d.setNetUnitPrice(rs.getDouble("net_unit_price"));
        d.setTaxBasis(rs.getDouble("tax_basis"));
        d.setTaxAmount(rs.getDouble("tax_amount"));
        d.setGrossTotal(rs.getDouble("gross_total"));
        d.setAppliedTaxGroup(rs.getString("applied_tax_group"));
        d.setSkuSnapshot(rs.getString("sku_snapshot"));
        d.setCategoryNameSnapshot(rs.getString("category_name_snapshot"));
        d.setObservations(rs.getString("observations"));
        return d;
    }
    private com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary mapResultSetToTaxSummary(ResultSet rs) throws SQLException {
        com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary s = new com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary();
        s.setId(rs.getInt("summary_id"));
        s.setSaleId(rs.getInt("sale_id"));
        s.setTaxRateId(rs.getInt("tax_rate_id"));
        s.setTaxName(rs.getString("tax_name"));
        s.setTaxRate(rs.getDouble("tax_rate"));
        s.setTaxBasis(rs.getDouble("tax_basis"));
        s.setTaxAmount(rs.getDouble("tax_amount"));
        return s;
    }
}
