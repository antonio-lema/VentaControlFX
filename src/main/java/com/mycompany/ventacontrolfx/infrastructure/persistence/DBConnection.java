package com.mycompany.ventacontrolfx.infrastructure.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DBConnection {
    private static String URL;
    private static String USER;
    private static String PASS;
    private static final int MAX_POOL_SIZE = 10;
    private static final BlockingQueue<Connection> pool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);

    static {
        try (java.io.InputStream input = DBConnection.class.getClassLoader()
                .getResourceAsStream("config/db.properties")) {
            java.util.Properties prop = new java.util.Properties();
            if (input == null) {
                System.err.println("CRITICAL: unable to find config/db.properties");
            } else {
                prop.load(input);
                URL = prop.getProperty("db.url");
                USER = prop.getProperty("db.user");
                PASS = prop.getProperty("db.password");

                // Initialize driver
                Class.forName("com.mysql.cj.jdbc.Driver");
            }
        } catch (Exception ex) {
            System.err.println("Error initializing DBConnection: " + ex.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = pool.poll();
        if (conn != null) {
            try {
                if (!conn.isClosed() && conn.isValid(2)) {
                    return new PooledConnection(conn);
                }
            } catch (SQLException e) {
                // Connection invalid, silently ignore and create new
            }
        }
        return new PooledConnection(DriverManager.getConnection(URL, USER, PASS));
    }

    private static void releaseConnection(Connection conn) {
        if (pool.size() < MAX_POOL_SIZE) {
            pool.offer(conn);
        } else {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }

    // A simple wrapper to intercept close() calls and return to pool
    private static class PooledConnection extends com.mycompany.ventacontrolfx.util.ConnectionWrapper {
        public PooledConnection(Connection delegate) {
            super(delegate);
        }

        @Override
        public void close() throws SQLException {
            DBConnection.releaseConnection(getDelegate());
        }
    }
}
