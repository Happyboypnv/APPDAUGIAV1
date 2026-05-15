# ✅ WebSocket Implementation - Integration Checklist

## 📋 Pre-Integration Review

### Files Created ✅

- [x] **Server Components**
  - [x] `src/main/java/com/mycompany/server/websocket/AuctionWebSocketServer.java`
  - [x] `src/main/java/com/mycompany/server/websocket/AuctionWebSocketServerStarter.java`

- [x] **Client Components**
  - [x] `src/main/java/com/mycompany/websocket/AuctionWebSocketClient.java`
  - [x] `src/main/java/com/mycompany/websocket/AuctionWebSocketListener.java`

- [x] **Integration Components**
  - [x] `src/main/java/com/mycompany/controller/websocket/AuctionWebSocketControllerAdapter.java`

- [x] **Documentation**
  - [x] `WEBSOCKET_IMPLEMENTATION_GUIDE.md`
  - [x] `WEBSOCKET_QUICKSTART.md`
  - [x] `WEBSOCKET_SUMMARY.md`
  - [x] `WEBSOCKET_ARCHITECTURE_DIAGRAMS.md`

### Dependencies Already Added ✅

- [x] Java-WebSocket (1.5.4) in pom.xml
- [x] Gson in pom.xml
- [x] JavaFX libraries
- [x] SQLite JDBC driver

---

## 🔧 Integration Steps

### Step 1: Verify Dependencies

- [ ] Open `pom.xml`
- [ ] Verify these dependencies exist:
  ```xml
  <dependency>
      <groupId>org.java-websocket</groupId>
      <artifactId>Java-WebSocket</artifactId>
      <version>1.5.4</version>
  </dependency>
  
  <dependency>
      <groupId>com.google.code.gson</groupId>
      <artifactId>gson</artifactId>
      <version>2.10.1</version>
  </dependency>
  ```
- [ ] Run: `mvn clean install` to download dependencies
- [ ] Build project: `mvn compile`

### Step 2: Implement PhienDauGiaService.datGia()

**File:** `src/main/java/com/mycompany/action/PhienDauGiaService.java`

**Add this method (must be thread-safe):**

```java
import java.util.HashMap;
import java.util.Map;

public synchronized Object datGia(String phienId, String userId, double giaRa) {
    // ⚠️ IMPORTANT: This method MUST be synchronized!
    // Why: Multiple threads (clients) may call simultaneously
    
    try {
        // 1. Get current auction
        PhienDauGia phien = findPhienById(phienId);
        if (phien == null) {
            return "Phiên đấu giá không tồn tại";
        }
        
        // 2. Check auction status
        if (!phien.getTrangThai().equals("DANG_DIEN_RA")) {
            return "Phiên đấu giá không đang diễn ra";
        }
        
        // 3. Validate bid
        if (giaRa <= phien.getGiaHienTai()) {
            return "Giá đặt phải cao hơn giá hiện tại: " + phien.getGiaHienTai();
        }
        
        if (giaRa - phien.getGiaHienTai() < phien.getBuocGia()) {
            return "Giá tăng phải >= " + phien.getBuocGia();
        }
        
        // 4. Use database transaction
        Connection conn = KetNoiCSDL.layKetNoi();
        try {
            conn.setAutoCommit(false);  // Start transaction
            
            // Update phien
            String updateSQL = "UPDATE phien_dau_gia SET gia_hien_tai = ?, ma_nguoi_thang_cuoc = ? WHERE ma_phien = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSQL)) {
                ps.setDouble(1, giaRa);
                ps.setString(2, userId);
                ps.setString(3, phienId);
                ps.executeUpdate();
            }
            
            // Record bid
            String bidSQL = "INSERT INTO nguoi_tra_gia (ma_phien, ma_nguoi_dung, gia_tra, thoi_gian) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(bidSQL)) {
                ps.setString(1, phienId);
                ps.setString(2, userId);
                ps.setDouble(3, giaRa);
                ps.setString(4, new BoChuyenDoiNgayGio().layNgayGioHienTai());
                ps.executeUpdate();
            }
            
            // Commit transaction
            conn.commit();
            conn.setAutoCommit(true);
            
            // 5. Return success with details
            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("gia_hien_tai", giaRa);
            result.put("userId", userId);
            return result;
            
        } catch (SQLException e) {
            conn.rollback();  // Rollback on error
            return "Lỗi database: " + e.getMessage();
        }
        
    } catch (Exception e) {
        System.err.println("❌ Error in datGia: " + e.getMessage());
        e.printStackTrace();
        return "Lỗi: " + e.getMessage();
    }
}

// Helper method
private PhienDauGia findPhienById(String phienId) {
    // Implementation to find auction by ID
    // ...
}
```

- [ ] Verify method is **synchronized** (critical!)
- [ ] Use database **transactions** (begin, commit, rollback)
- [ ] Return `Map<String, Object>` with status and current price
- [ ] Handle all exceptions gracefully

**Checklist:**
- [ ] Method signature is `synchronized`
- [ ] Database transaction: `setAutoCommit(false)`, `commit()`, `rollback()`
- [ ] Validates bid price
- [ ] Updates database atomically
- [ ] Returns success map with `gia_hien_tai`
- [ ] Tested locally

### Step 3: Start WebSocket Server in ServerApp

**File:** `src/main/java/com/mycompany/server/ServerApp.java`

```java
import com.mycompany.websocket.AuctionWebSocketServerStarter;

public class ServerApp {
  public static void main(String[] args) {
    System.out.println("🚀 Starting server...");

    // Start HTTP server (port 8080)
    // ... existing HTTP server code ...

    // ✅ ADD THIS:
    System.out.println("🔌 Starting WebSocket server...");
    AuctionWebSocketServerStarter.startServer();

    System.out.println("✅ All servers started successfully");
  }
}
```

- [ ] Added import statement
- [ ] Called `AuctionWebSocketServerStarter.startServer()`
- [ ] Server logs show "WebSocket server started on port 8081"

### Step 4: Integrate into HomeController

**File:** `src/main/java/com/mycompany/Controller/HomeController.java`

```java
import com.mycompany.server.websocket.AuctionWebSocketClient;
import com.mycompany.server.controller.AuctionWebSocketControllerAdapter;
import com.mycompany.utils.SessionManager;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

public class HomeController implements Initializable {

  @FXML
  private Label priceLabel;      // Update with current price
  @FXML
  private Button bidButton;      // Disable when disconnected
  @FXML
  private TextField bidAmountField;

  private AuctionWebSocketClient wsClient;
  private AuctionWebSocketControllerAdapter adapter;

  @Override
  public void initialize(URL url, ResourceBundle resources) {
    try {
      // Get current auction ID
      String phienId = "PHIEN_001";  // TODO: Get from actual session

      // Connect to WebSocket
      System.out.println("🔗 Connecting to WebSocket...");
      wsClient = AuctionWebSocketClient.getInstance();
      adapter = new AuctionWebSocketControllerAdapter(this, priceLabel);
      wsClient.setListener(adapter);
      ws client.connect();  // Blocks until connected

      System.out.println("✅ WebSocket connected");

      // Join auction room
      String userId = SessionManager.getInstance().getUserId();  // TODO: Verify method exists
      wsClient.sendJoin(phienId, userId);

    } catch (InterruptedException e) {
      System.err.println("❌ Connection failed: " + e.getMessage());
      HandleNavigationAndAlert.getInstance().showAlert(
              Alert.AlertType.ERROR,
              "Connection Error",
              "Failed to connect to auction server. Please refresh."
      );
      bidButton.setDisable(true);
    }
  }

  @FXML
  public void onClickedBid() {
    try {
      double giaRa = Double.parseDouble(bidAmountField.getText());
      String phienId = "PHIEN_001";  // TODO: Get from actual session

      adapter.sendBid(phienId, giaRa);

      bidAmountField.clear();

    } catch (NumberFormatException e) {
      HandleNavigationAndAlert.getInstance().showAlert(
              Alert.AlertType.ERROR,
              "Invalid Input",
              "Please enter a valid amount"
      );
    }
  }

  // TODO: Add cleanup on window close
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

**Checklist:**
- [ ] Added WebSocket imports
- [ ] Initialize client and adapter in `initialize()`
- [ ] Call `wsClient.connect()` (blocks until ready)
- [ ] Send JOIN message with phienId and userId
- [ ] Implement `onClickedBid()` to send bids
- [ ] Implement `cleanup()` for disconnect
- [ ] Handle `InterruptedException` gracefully
- [ ] Set up price label and bid button

### Step 5: Add Cleanup on App Close

**File:** `src/main/java/com/mycompany/App.java`

```java
@Override
public void stop() throws Exception {
    super.stop();
    
    // Get current controller and cleanup
    try {
        HomeController controller = (HomeController) currentController;  // TODO: Adjust
        if (controller != null) {
            controller.cleanup();
        }
    } catch (Exception e) {
        System.err.println("Error during cleanup: " + e.getMessage());
    }
    
    System.out.println("🛑 Application closed");
}
```

- [ ] Override `stop()` method
- [ ] Call `controller.cleanup()`
- [ ] Disconnect WebSocket before app closes

### Step 6: Test Server

- [ ] Open terminal
- [ ] Navigate to project root: `cd D:\Git\APPDAUGIAV1`
- [ ] Build project: `mvn clean package`
  - [ ] Build succeeds with no errors
  - [ ] JAR created in `target/`
- [ ] Start server
  ```bash
  java -cp target/AppDauGia.jar com.mycompany.server.ServerApp
  ```
  - [ ] See output: `🚀 AuctionWebSocketServer started on port 8081`
  - [ ] No errors

### Step 7: Test Client Connection (Single User)

- [ ] Start JavaFX application
- [ ] Navigate to auction room
- [ ] Check console for:
  - [ ] `🔗 Connecting to WebSocket...`
  - [ ] `✅ WebSocket connected successfully`
  - [ ] Price label shows initial price
- [ ] Click "Đặt giá" button
  - [ ] No error message
  - [ ] See console: `💰 Sent bid: X.XXX`
- [ ] Price updates automatically
  - [ ] Label shows new price
  - [ ] Color changes (green = success)

### Step 8: Test Real-Time Sync (Multiple Users)

**Terminal 1:** Start server
```bash
java -cp target/AppDauGia.jar com.mycompany.server.ServerApp
```

**Terminal 2 & 3:** Start two JavaFX clients
```bash
java -cp target/AppDauGia.jar com.mycompany.App
java -cp target/AppDauGia.jar com.mycompany.App
```

**Test Scenarios:**

- [ ] **Both clients connect successfully**
  - [ ] Both show "Connected" status
  - [ ] Both can see initial price

- [ ] **Client 1 places bid: 100,000**
  - [ ] Client 1 updates to 100,000 immediately
  - [ ] Client 2 receives update
  - [ ] Client 2 shows 100,000
  - [ ] Both show success color (green)

- [ ] **Client 2 places bid: 150,000**
  - [ ] Client 2 updates to 150,000 immediately
  - [ ] Client 1 receives update
  - [ ] Client 1 shows 150,000
  - [ ] Both synchronized

- [ ] **Close Client 1**
  - [ ] Client 1 shows "Disconnected" message
  - [ ] Client 2 sees "USER_LEFT" event
  - [ ] Client 2 can still bid

- [ ] **Invalid bids are rejected**
  - [ ] Bid lower than current price
  - [ ] Both clients see failure message
  - [ ] Price unchanged

### Step 9: Performance Testing (Optional)

- [ ] Multiple rapid bids from same client
  - [ ] All bids processed
  - [ ] No data corruption
  - [ ] All clients see correct final price

- [ ] Concurrent bids from different clients
  - [ ] Race condition handled by `synchronized`
  - [ ] Highest bid wins
  - [ ] All clients consistent

- [ ] Network lag simulation
  - [ ] Close network
  - [ ] Client shows disconnected
  - [ ] Restore network
  - [ ] Manual reconnect (or implement auto-reconnect)

---

## 🐛 Common Issues & Solutions

### Issue 1: Port Already in Use
```
❌ Error: Address already in use: bind
```

**Solution:**
```bash
# Find process on port 8081
netstat -ano | findstr :8081

# Kill process
taskkill /PID <PID> /F

# Or change port in AuctionWebSocketServer
new AuctionWebSocketServer(8082)  // different port
```

- [ ] Verified port 8081 is available
- [ ] Started server successfully

### Issue 2: Connection Timeout
```
❌ Error: Connection timeout
❌ Error: java.net.ConnectException: Connection refused
```

**Solution:**
- [ ] Verify server is running on port 8081
- [ ] Check firewall settings
- [ ] Verify correct IP/port in client
- [ ] Check server logs for startup errors

### Issue 3: Bid Not Syncing
```
❌ Client 1 bids but Client 2 doesn't see update
```

**Solution:**
- [ ] Check PhienDauGiaService.datGia() is synchronized
- [ ] Verify database transaction commits
- [ ] Check server logs for `ERROR`
- [ ] Verify client received JOIN response

### Issue 4: UI Not Updating
```
❌ Price label doesn't update after bid
```

**Solution:**
- [ ] Verify Platform.runLater() in onMessage()
- [ ] Check listener callback implementation
- [ ] Verify label references in adapter
- [ ] Check JavaFX event logs

### Issue 5: Build Failures
```
❌ Maven build fails
❌ ClassNotFoundException
```

**Solution:**
```bash
# Clean build
mvn clean install -U

# Clear cache
rm -rf ~/.m2/repository/org/java-websocket

# Rebuild
mvn clean compile package
```

- [ ] `mvn clean install` succeeds
- [ ] All dependencies downloaded
- [ ] JAR compiles without errors

---

## ✨ Verification Checklist (Final)

### Server-Side ✅
- [ ] `AuctionWebSocketServer.java` compiles
- [ ] `AuctionWebSocketServerStarter.java` compiles
- [ ] `datGia()` is synchronized
- [ ] Database transactions work
- [ ] Server logs show "started on port 8081"

### Client-Side ✅
- [ ] `AuctionWebSocketClient.java` compiles
- [ ] `AuctionWebSocketListener.java` compiles
- [ ] Singleton pattern implemented
- [ ] Platform.runLater() used in callbacks
- [ ] connect() blocks until ready

### Integration ✅
- [ ] `AuctionWebSocketControllerAdapter.java` compiles
- [ ] HomeController imports WebSocket classes
- [ ] initialize() connects to server
- [ ] onClickedBid() sends bid
- [ ] cleanup() disconnects properly

### Testing ✅
- [ ] Single client connects successfully
- [ ] Single client can place bid
- [ ] Two clients see synchronized updates
- [ ] Disconnection handling works
- [ ] Error messages display correctly
- [ ] No crashes or exceptions

### Documentation ✅
- [ ] WEBSOCKET_IMPLEMENTATION_GUIDE.md explains architecture
- [ ] WEBSOCKET_QUICKSTART.md has integration steps
- [ ] WEBSOCKET_SUMMARY.md summarizes components
- [ ] WEBSOCKET_ARCHITECTURE_DIAGRAMS.md shows diagrams

---

## 🚀 Go-Live Checklist

- [ ] Code reviewed by team member
- [ ] All tests pass
- [ ] Server logs are clean (no warnings)
- [ ] Client handles disconnections gracefully
- [ ] Database integrity maintained
- [ ] Performance acceptable with 10+ concurrent users
- [ ] Documentation is up-to-date
- [ ] Team trained on WebSocket architecture
- [ ] Backup/rollback plan in place
- [ ] Monitoring in place (logs, metrics)

---

## 📚 Reference

| Document | Purpose |
|----------|---------|
| WEBSOCKET_IMPLEMENTATION_GUIDE.md | Detailed technical explanation |
| WEBSOCKET_QUICKSTART.md | Step-by-step integration guide |
| WEBSOCKET_SUMMARY.md | Component overview |
| WEBSOCKET_ARCHITECTURE_DIAGRAMS.md | Visual diagrams |
| WEBSOCKET_INTEGRATION_CHECKLIST.md | This file |

---

**Status: Ready for Integration** ✅

All components created, documented, and ready for integration into the main application!

