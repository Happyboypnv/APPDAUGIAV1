package com.mycompany.server.controller;

import com.google.gson.Gson;
import com.mycompany.models.AuctionSession;
import com.mycompany.models.Transaction;
import com.mycompany.models.User;
import com.mycompany.server.dto.TransactionHistoryItemDTO;
import com.mycompany.utils.ITransactionRepository;
import com.mycompany.utils.IUserRepository;
import com.mycompany.utils.TransactionRepositorySQLite;
import com.mycompany.utils.UserRepositorySQLite;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TransactionController {
  private final Gson gson = new Gson();
  private final IUserRepository userRepository = new UserRepositorySQLite();
  private final ITransactionRepository transactionRepository = new TransactionRepositorySQLite();

  public void route(HttpExchange exchange) throws IOException {
    String method = exchange.getRequestMethod().toUpperCase();
    String path = exchange.getRequestURI().getPath();

    if (method.equals("GET") && path.equals("/api/transactions/me")) {
      handleMyTransactions(exchange);
      return;
    }

    sendJson(exchange, 404, "{\"message\":\"Endpoint not found\"}");
  }

  private void handleMyTransactions(HttpExchange exchange) throws IOException {
    User currentUser = authenticate(exchange);
    if (currentUser == null) {
      sendJson(exchange, 401, "{\"message\":\"Unauthorized\"}");
      return;
    }

    List<TransactionHistoryItemDTO> response = new ArrayList<>();
    for (Transaction transaction : transactionRepository.findByUserId(currentUser.getUserId())) {
      AuctionSession auction = transaction.getAuctionSession();
      User seller = auction.getSeller();
      User winner = auction.getWinner();
      boolean isSeller = seller != null && currentUser.getUserId().equals(seller.getUserId());

      response.add(new TransactionHistoryItemDTO(
          transaction.getId(),
          auction.getSessionId(),
          auction.getProduct() != null ? auction.getProduct().getProductName() : "",
          isSeller ? "SELLER" : "BUYER",
          winner != null ? winner.getFullName() : "",
          seller != null ? seller.getFullName() : "",
          auction.getCurrentPrice(),
          transaction.getStatus().name(),
          transaction.getCreatedAt().toString()
      ));
    }

    sendJson(exchange, 200, gson.toJson(response));
  }

  private User authenticate(HttpExchange exchange) {
    String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;

    String token = authHeader.substring("Bearer ".length()).trim();
    if (!token.startsWith("USER_")) return null;

    String tokenBody = token.substring("USER_".length());
    int lastUnderscore = tokenBody.lastIndexOf('_');
    if (lastUnderscore < 0) return null;

    String email = tokenBody.substring(0, lastUnderscore);
    return userRepository.findByEmail(email);
  }

  private void sendJson(HttpExchange exchange, int statusCode, String jsonBody) throws IOException {
    byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    exchange.sendResponseHeaders(statusCode, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
    exchange.close();
  }
}
