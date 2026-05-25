package com.mycompany.local;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class HiddenAuctionRepository {
  public void hideAuction(String userId, String auctionId) {
    if (isBlank(userId) || isBlank(auctionId)) return;

    String sql = """
        INSERT OR REPLACE INTO hidden_auction (user_id, auction_id, hidden_at)
        VALUES (?, ?, ?)
        """;
    try (var connection = ClientLocalDatabase.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, userId);
      statement.setString(2, auctionId);
      statement.setString(3, LocalDateTime.now().toString());
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("Cannot hide auction locally: " + auctionId, e);
    }
  }

  public Set<String> findHiddenAuctionIds(String userId) {
    Set<String> result = new HashSet<>();
    if (isBlank(userId)) return result;

    String sql = "SELECT auction_id FROM hidden_auction WHERE user_id = ?";
    try (var connection = ClientLocalDatabase.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, userId);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          result.add(rs.getString("auction_id"));
        }
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Cannot read hidden auctions for user: " + userId, e);
    }
    return result;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
