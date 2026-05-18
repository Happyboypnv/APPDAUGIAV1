# 🚀 WebSocket Integration - Quick Start Guide

## Step-by-Step Integration

### Step 1: Start the WebSocket Server (ServerApp)

**File:** `src/main/java/com/mycompany/server/ServerApp.java`

```java
import com.mycompany.websocket.AuctionWebSocketServerStarter;

public class ServerApp {
    public static void main(String[] args) {
        System.out.println("🚀 Starting server...");

        // Start HTTP server (port 8080)
        // ... existing HTTP server code ...

        // ✅ ADD THIS: Start WebSocket server (port 8081)
        AuctionWebSocketServerStarter.startServer();

        System.out.println("✅ All servers started");
    }
}
```

---

### Step 2: Connect Client in HomeController

**File:** `src/main/java/com/mycompany/Controller/HomeController.java`

```java
import com.mycompany.server.websocket.AuctionWebSocketClient;
import com.mycompany.server.controller.AuctionWebSocketControllerAdapter;
import javafx.fxml.Initializable;

public class HomeController implements Initializable {

    @FXML
    private Label priceLabel;
    @FXML
    private Button bidButton;
    @FXML
    private TextField bidAmountField;

    private AuctionWebSocketClient wsClient;
    private AuctionWebSocketControllerAdapter adapter;

    @Override
    public void initialize(URL url, ResourceBundle resources) {
        try {
            // ✅ Initialize WebSocket connection
            wsClient = AuctionWebSocketClient.getInstance();
            adapter = new AuctionWebSocketControllerAdapter(this, priceLabel);
            wsClient.setListener(adapter);

            // Block until connected (5s timeout)
            wsClient.connect();
            System.out.println("✅ WebSocket connected");

            // ✅ Join auction room
            String phienId = "PHIEN001";  // Get from current session
            String userId = SessionManager.getInstance().getUserId();
            wsClient.sendJoin(phienId, userId);

        } catch (InterruptedException e) {
            System.err.println("❌ Connection timeout: " + e.getMessage());
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR,
                    "Connection Error",
                    "Failed to connect to auction server"
            );
        }
    }

    @FXML
    public void onClickedBid() {
        try {
            double giaRa = Double.parseDouble(bidAmountField.getText());
            String phienId = "PHIEN001";

            // Send bid via WebSocket
            adapter.sendBid(phienId, giaRa);

        } catch (NumberFormatException e) {
            HandleNavigationAndAlert.getInstance().showAlert(
                    Alert.AlertType.ERROR,
                    "Invalid Input",
                    "Please enter a valid amount"
            );
        }
    }

    // ✅ ADD THIS: Cleanup on window close
    public void cleanup() {
        try {
            if (wsClient != null) {
                wsClient.disconnect();
            }
        } catch (InterruptedException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }
}
```

---

### Step 3: Update App.java to Call Cleanup

**File:** `src/main/java/com/mycompany/App.java`

```java
@Override
public void stop() throws Exception {
    super.stop();
    
    // ✅ Cleanup WebSocket connection
    // Get current controller and call cleanup()
    System.out.println("🛑 Shutting down application...");
}
```

---

## 🔧 Thread Safety Checklist

✅ **Server-Side:**
- [ ] Using `ConcurrentHashMap` for rooms management
- [ ] Using `synchronized` blocks for multi-step operations
- [ ] Broadcasting with proper error handling
- [ ] Graceful disconnect handling

✅ **Client-Side:**
- [ ] Using `volatile` fields for `isConnected`
- [ ] Using `Platform.runLater()` in `onMessage()` callback
- [ ] Using `CountDownLatch` for connection sync
- [ ] Handling thread-safety in `sendBid()`

✅ **UI-Controller:**
- [ ] All UI updates happen in `onMessage()` callbacks (via Platform.runLater())
- [ ] No blocking operations on JavaFX thread
- [ ] Proper error handling with try-catch

---

## 📊 Testing Scenarios

### Scenario 1: Single User Places Bid
```
1. Start Server
2. Start Client
3. Connect successfully
4. User enters bid amount: 100,000
5. Click "Đặt giá"
6. Expected: Price updates to 100,000
```

### Scenario 2: Multiple Users Real-Time Sync
```
Terminal 1: Start Server
Terminal 2: Start Client 1
Terminal 3: Start Client 2

Test:
1. Both clients connect to same auction
2. Client 1 places bid: 100,000
   - Expected: Both see price = 100,000
3. Client 2 places bid: 150,000
   - Expected: Both see price = 150,000
4. Close Client 1
   - Expected: Client 2 sees "USER_LEFT" event
```

### Scenario 3: Connection Loss Recovery
```
1. Client connected and bidding
2. Disconnect network (kill router)
3. Expected:
   - onDisconnected() called
   - Bid button disabled
   - Status shows "Disconnected"
4. Restore network
   - User must reload (automatic reconnect not implemented yet)
```

---

## 🛠️ Troubleshooting

### Problem: "Connection refused" error
```
❌ Error: java.net.ConnectException: Connection refused

Possible causes:
1. Server not started
2. Wrong port (check 8081)
3. Server crashed

Solution:
1. Start server: java -cp AppDauGia.jar com.mycompany.server.ServerApp
2. Check logs for errors
3. Verify port 8081 is not blocked by firewall
```

### Problem: "Platform.runLater() throwing error"
```
❌ Error: ClassCastException in onMessage()

Possible cause:
- UI update not in Platform.runLater()

Solution:
✅ Always wrap UI updates:
Platform.runLater(() -> {
    label.setText("Update");
});
```

### Problem: "Bid not syncing to other clients"
```
❌ Bid sent but other clients don't see update

Possible causes:
1. PhienDauGiaService.datGia() throwing error
2. broadcastToRoom() not working
3. Client not in room

Solution:
1. Check server logs for errors
2. Verify client received JOIN response
3. Add debug logging in broadcastToRoom()
```

---

## 📚 Files Created

```
src/main/java/com/mycompany/
├── websocket/
│   ├── AuctionWebSocketClient.java          (Client handler)
│   └── AuctionWebSocketListener.java        (Callback interface)
├── controller/websocket/
│   └── AuctionWebSocketControllerAdapter.java  (Controller adapter)
└── server/websocket/
    ├── AuctionWebSocketServer.java          (Server handler)
    └── AuctionWebSocketServerStarter.java   (Server starter)

D:\Git\APPDAUGIAV1\
├── WEBSOCKET_IMPLEMENTATION_GUIDE.md        (Detailed documentation)
└── WEBSOCKET_QUICKSTART.md                  (This file)
```

---

## 🔐 Thread Safety Summary

| Component | Thread-Safety Mechanism | Details |
|-----------|------------------------|---------|
| **rooms Map** | ConcurrentHashMap | Multiple threads access room data safely |
| **clientRooms Map** | ConcurrentHashMap | Track client-to-room associations |
| **Room operations** | synchronized blocks | Multi-step operations (add+update) are atomic |
| **Connection status** | volatile field | All threads see boolean changes immediately |
| **UI updates** | Platform.runLater() | Switch from WebSocket thread to JavaFX thread |
| **Client singleton** | synchronized getInstance() | Only 1 client instance created |
| **Message sync** | CountDownLatch | Block connect() until onOpen() called |

---

## 🎯 Next Implementation Steps

1. ✅ WebSocket Server & Client created
2. ✅ Thread-safe data structures implemented
3. ⏳ **TODO:** Implement `PhienDauGiaService.datGia()` (must be thread-safe)
4. ⏳ **TODO:** Integrate adapter into HomeController
5. ⏳ **TODO:** Add automatic reconnection on disconnect
6. ⏳ **TODO:** Add connection status indicator UI
7. ⏳ **TODO:** Test with multiple concurrent users

---

## 📖 Reference

- **Java-WebSocket:** https://github.com/TooTallNate/Java-WebSocket
- **JavaFX Platform:** https://docs.oracle.com/javase/8/javafx/api/javafx/application/Platform.html
- **Concurrency Utilities:** https://docs.oracle.com/javase/10/docs/api/java/util/concurrent/package-summary.html

