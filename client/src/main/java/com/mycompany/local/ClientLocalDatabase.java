package com.mycompany.local;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class ClientLocalDatabase {
  private static final Path DB_DIR = Path.of(System.getProperty("user.home"), ".appdaugia");
  private static final String URL = "jdbc:sqlite:" + DB_DIR.resolve("client-local.db");

  private ClientLocalDatabase() {}

  public static Connection getConnection() throws SQLException {
    try {
      Files.createDirectories(DB_DIR);
    } catch (Exception e) {
      throw new SQLException("Cannot create client local DB directory: " + DB_DIR, e);
    }

    Connection connection = DriverManager.getConnection(URL);
    try (Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys=ON");
      statement.execute("PRAGMA busy_timeout=5000");
      statement.execute("""
          CREATE TABLE IF NOT EXISTS hidden_auction (
            user_id TEXT NOT NULL,
            auction_id TEXT NOT NULL,
            hidden_at TEXT NOT NULL,
            PRIMARY KEY (user_id, auction_id)
          )
          """);
    }
    return connection;
  }
}
