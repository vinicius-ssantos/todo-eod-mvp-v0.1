package com.todo.eod.devinfra;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class EmbeddedPostgresRunner {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("EMBEDDED_PG_PORT", "5432"));
        String dbName = System.getenv().getOrDefault("EMBEDDED_PG_DB", "todoeod");
        String user = System.getenv().getOrDefault("EMBEDDED_PG_USER", "todo");
        String password = System.getenv().getOrDefault("EMBEDDED_PG_PASSWORD", "todo");

        EmbeddedPostgres postgres = EmbeddedPostgres.builder()
            .setPort(port)
            .setServerConfig("listen_addresses", "127.0.0.1")
            .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                postgres.close();
            } catch (Exception ignored) {
            }
        }));

        initializeDatabase(postgres.getPostgresDatabase(), dbName, user, password);

        System.out.printf("Embedded Postgres running on port %d for database %s (%s)\n", port, dbName, user);
        Thread.currentThread().join();
    }

    private static void initializeDatabase(DataSource dataSource, String dbName, String user, String password) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement stmt = connection.createStatement()) {
            stmt.execute(String.format(
                "DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '%s') THEN "
                    + "CREATE ROLE \"%s\" WITH LOGIN PASSWORD '%s'; END IF; END $$;",
                escapeLiteral(user), escapeIdentifier(user), escapeLiteral(password)));
        }

        try (Connection connection = dataSource.getConnection(); Statement stmt = connection.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(String.format(
                "SELECT 1 FROM pg_database WHERE datname = '%s'",
                escapeLiteral(dbName)))) {
                if (!rs.next()) {
                    stmt.execute(String.format(
                        "CREATE DATABASE \"%s\" OWNER \"%s\"",
                        escapeIdentifier(dbName), escapeIdentifier(user)));
                }
            }
        }
    }

    private static String escapeLiteral(String value) {
        return value.replace("'", "''");
    }

    private static String escapeIdentifier(String value) {
        return value.replace("\"", "\"\"");
    }

    private EmbeddedPostgresRunner() {
    }
}
