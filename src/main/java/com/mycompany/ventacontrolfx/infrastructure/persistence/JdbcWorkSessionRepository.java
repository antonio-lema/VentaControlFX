package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.WorkSession;
import com.mycompany.ventacontrolfx.domain.repository.IWorkSessionRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcWorkSessionRepository implements IWorkSessionRepository {

    @Override
    public void save(WorkSession session) {
        String sql = "INSERT INTO work_sessions (user_id, type, start_time, status) VALUES (?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, session.getUserId());
            pstmt.setString(2, session.getType().name());
            pstmt.setTimestamp(3, Timestamp.valueOf(session.getStartTime()));
            pstmt.setString(4, session.getStatus().name());
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    session.setSessionId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving work session", e);
        }
    }

    @Override
    public void update(WorkSession session) {
        String sql = "UPDATE work_sessions SET end_time = ?, status = ?, notes = ? WHERE session_id = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setTimestamp(1, session.getEndTime() != null ? Timestamp.valueOf(session.getEndTime()) : null);
            pstmt.setString(2, session.getStatus().name());
            pstmt.setString(3, session.getNotes());
            pstmt.setInt(4, session.getSessionId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating work session", e);
        }
    }

    @Override
    public Optional<WorkSession> getActiveSession(Integer userId) {
        String sql = "SELECT * FROM work_sessions WHERE user_id = ? AND status = 'ACTIVE' LIMIT 1";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToWorkSession(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting active work session", e);
        }
        return Optional.empty();
    }

    @Override
    public List<WorkSession> getHistory(Integer userId) {
        List<WorkSession> history = new ArrayList<>();
        String sql = "SELECT * FROM work_sessions WHERE user_id = ? ORDER BY start_time DESC";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    history.add(mapResultSetToWorkSession(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting work session history", e);
        }
        return history;
    }

    @Override
    public List<WorkSession> getAllActiveSessions() {
        List<WorkSession> sessions = new ArrayList<>();
        // Unimos con usuarios para tener el nombre en el panel de control
        String sql = "SELECT s.*, u.full_name as user_name FROM work_sessions s " +
                "JOIN users u ON s.user_id = u.user_id " +
                "WHERE s.status = 'ACTIVE'";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                WorkSession session = mapResultSetToWorkSession(rs);
                // Podemos usar el campo notes temporalmente o a\u00f1adir un campo transient en el
                // modelo
                // Pero por ahora lo dejamos as\u00ed y lo manejaremos en el ViewModel
                sessions.add(session);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting active sessions", e);
        }
        return sessions;
    }

    @Override
    public List<WorkSession> getHistoryByDate(java.time.LocalDate date) {
        List<WorkSession> history = new ArrayList<>();
        String sql = "SELECT * FROM work_sessions WHERE DATE(start_time) = ? ORDER BY start_time DESC";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    history.add(mapResultSetToWorkSession(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting work session history by date", e);
        }
        return history;
    }

    private WorkSession mapResultSetToWorkSession(ResultSet rs) throws SQLException {
        WorkSession session = new WorkSession();
        session.setSessionId(rs.getInt("session_id"));
        session.setUserId(rs.getInt("user_id"));
        session.setType(WorkSession.SessionType.valueOf(rs.getString("type")));
        session.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        if (rs.getTimestamp("end_time") != null) {
            session.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        }
        session.setStatus(WorkSession.SessionStatus.valueOf(rs.getString("status")));
        session.setNotes(rs.getString("notes"));

        // Cargar nombre de usuario si se incluy\u00f3 en el JOIN (usado en panel operativo)
        try {
            String uname = rs.getString("user_name");
            if (uname != null)
                session.setUserName(uname);
        } catch (SQLException e) {
            // Ignorar si la columna no existe en este ResultSet espec\u00edfico
        }

        return session;
    }
}
