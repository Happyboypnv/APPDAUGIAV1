# Refactor Notes

## Module boundaries

- `shared`: DTO, models, enums, and exceptions used by both client and server.
- `server`: HTTP/WebSocket server, auction/payment/bid business logic, schedulers, and SQLite repositories for the main system database.
- `client`: JavaFX/FXML UI, API client, local session state, and client-only WebSocket listener.

## Dependency direction

```text
client -> shared
server -> shared
shared -> no app module
```

The client must not depend on `server` and must not import server repositories such as:

- `DatabaseConnection`
- `AuctionRepositorySQLite`
- `UserRepositorySQLite`
- `TransactionRepositorySQLite`

## Auction deletion rule

There are two different operations:

- Hide/delete locally: the client removes the auction from the current user's local view only.
- Delete/cancel system-wide: the server updates the main DB and broadcasts the change.

The old `DELETE /api/auctions/{id}` currently deletes from the main DB. Do not call it for a normal user "remove from my list" action. Implement a separate client-local table or endpoint semantics before wiring that UI action again.

## Remaining integration work

- Client local DB has been added at runtime under the current user's home directory as `.appdaugia/client-local.db`.
- `HomeController` now hides ended/cancelled auctions through `HiddenAuctionRepository` instead of calling server delete.
- `GET /api/transactions/me` has been added so `TransactionHistoryController` loads history through `ApiClient`.
- Move server DTO names out of the `com.mycompany.server.dto` package later if the team wants a cleaner package name such as `com.mycompany.shared.dto`.

## Guardrail

`ApiClient.deleteAuction(...)` should not be used by normal user UI. It is intentionally not called by `HomeController` anymore. If the team needs a real system-wide delete, expose it later through an admin-only flow.
