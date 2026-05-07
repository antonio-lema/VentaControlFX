package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.repository.IAiIntentRepository;
import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class JdbcAiIntentRepository implements IAiIntentRepository {
    private final Gson gson = new Gson();

    @Override
    public void logIntent(String intent, Map<String, Object> payload, Map<String, Object> result, Integer userId,
            Integer cashierId, String intentId) {
        String sql = "INSERT INTO ai_intent_logs (intent, payload, result, user_id, cashier_id, intent_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, intent);
            pstmt.setString(2, gson.toJson(payload));
            pstmt.setString(3, gson.toJson(result));
            if (userId != null)
                pstmt.setInt(4, userId);
            else
                pstmt.setNull(4, java.sql.Types.INTEGER);
            if (cashierId != null)
                pstmt.setInt(5, cashierId);
            else
                pstmt.setNull(5, java.sql.Types.INTEGER);
            pstmt.setString(6, intentId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error logging AI intent: " + e.getMessage());
        }
    }
}

