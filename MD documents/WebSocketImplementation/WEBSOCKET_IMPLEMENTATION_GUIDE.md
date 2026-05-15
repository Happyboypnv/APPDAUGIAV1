# 🚀 Real-Time Auction Bidding System - WebSocket Implementation

## 📋 Overview

This document explains the thread-safe WebSocket implementation for real-time auction bidding.

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     SYSTEM ARCHITECTURE                     │
└─────────────────────────────────────────────────────────────┘

[JavaFX Client]                              [Java Server]
     │                                            │
     ├─ AuctionWebSocketClient                   ├─ AuctionWebSocketServer
     │  (port 8081)                              │  (port 8081)
     └──────────────────── WebSocket ─────────────┘
        (JSON messages)                (JSON messages)
                                            │
                                            ├─ PhienDauGiaService
                                            │  (database operations)
                                            │
                                            └─ SQLite DB
```

---

## 🔐 Thread Safety Mechanisms

### 1. **ConcurrentHashMap for Data Structures**

#### Problem:
```java
// ❌ WRONG: HashMap (not thread-safe)
Map<String, Set<WebSocket>> rooms = new HashMap<>();
// Race condition: Thread A reads keys while Thread B modifies
```

#### Solution:
```java
// ✅ CORRECT: ConcurrentHashMap (thread-safe)
Map<String, Set<WebSocket>> rooms = new ConcurrentHashMap<>();
// Multiple threads can safely read/modify simultaneously
```

**Why it works:**
- ConcurrentHashMap uses segment locking (bucket-level locks)
- Each segment can be accessed by different threads independently
- Doesn't need to lock entire map
- Better concurrency than synchronized HashMap

### 2. **Synchronized Blocks for Multi-Step Operations**

#### Problem:
```java
// ❌ WRONG: Not atomic
Set<WebSocket> room = rooms.get(phienId);
room.add(client);  // Two separate operations → race condition
// Another thread might remove room between get() and add()
```

#### Solution:
```java
// ✅ CORRECT: Atomic operation
Set<WebSocket> room = rooms.computeIfAbsent(phienId, k ->
    Collections.newSetFromMap(new ConcurrentHashMap<>())
);
synchronized (room) {  // Extra protection
    room.add(client);
}
// Guaranteed to create room if missing AND add client atomically
```

**When to use:**
- Multiple operations that depend on each other
- Need operation to be indivisible from other threads' perspective

### 3. **volatile Fields for Cross-Thread Visibility**

#### Problem:
```java
// ❌ WRONG: No visibility guarantee
private boolean isConnected = false;
// Thread A sets to true, but Thread B might not see it immediately
```

#### Solution:
```java
// ✅ CORRECT: volatile guarantees visibility
private volatile boolean isConnected = false;
// All threads see updates immediately
```

**How it works:**
- volatile write: update value AND flush to main memory
- volatile read: read from main memory
- Ensures all threads see most recent value
- Does NOT provide atomicity (use for simple flags)

### 4. **Platform.runLater() for JavaFX Thread Safety**

#### Problem:
```java
// ❌ WRONG: Update UI from WebSocket thread
@Override
public void onMessage(String message) {
    // Called on WebSocket thread (NOT JavaFX thread)
    label.setText("New Price: 100000");  // ❌ CRASH! UI can only update from JavaFX thread
}
```

#### Solution:
```java
// ✅ CORRECT: Schedule update on JavaFX thread
@Override
public void onMessage(String message) {
    Platform.runLater(() -> {
        // Now on JavaFX thread - safe to update UI
        label.setText("New Price: 100000");  // ✅ Works!
    });
}
```

**Thread context:**
```
WebSocket Thread              JavaFX Thread
    │                              │
    ├─ Receive message            │
    ├─ Parse JSON                 │
    └─ Platform.runLater()────────┼─> Update UI
                                  │
```

### 5. **CountDownLatch for Synchronization**

#### Usage:
```java
private final CountDownLatch connectionLatch = new CountDownLatch(1);

public void connect() throws InterruptedException {
    this.connectBlocking();  // Blocks until connection established
    
    // Wait for onOpen() callback to execute
    connectionLatch.await(5, TimeUnit.SECONDS);
    // onOpen() calls connectionLatch.countDown()
    // This waits until count reaches 0
}
```

**Thread coordination:**
```
Main Thread                     WebSocket Thread
    │                                │
    ├─ connect()                     │
    ├─ await(5s)  ──blocks───────>   │
    │                           onOpen()
    │                           countDown()
    │           <──unblocks──────
    └─ Returns (connected)
```

---

## 📊 Component Details

### Server-Side: AuctionWebSocketServer

#### Key Features:

1. **Room Management**
   ```java
   private final Map<String, Set<WebSocket>> rooms = new ConcurrentHashMap<>();
   private final Map<WebSocket, String> clientRooms = new ConcurrentHashMap<>();
   ```
   - `rooms`: Phiên ID → Set of connected clients
   - `clientRooms`: Client connection → Phiên ID (for efficient disconnect)

2. **Message Handling**
   ```
   onMessage(WebSocket, String)
       ├─ Parse JSON
       ├─ Check action type
       ├─ handleJoin() or handleBid()
       └─ broadcastToRoom()
   ```

3. **Broadcast Implementation**
   ```java
   private void broadcastToRoom(String phienId, String message) {
       Set<WebSocket> room = rooms.get(phienId);
       synchronized (room) {  // Thread-safe iteration
           for (WebSocket client : room) {
               client.send(message);  // Non-blocking send
           }
       }
   }
   ```

#### Thread Safety:
- ✅ ConcurrentHashMap for room/client tracking
- ✅ Synchronized iteration when broadcasting
- ✅ Atomic room creation with computeIfAbsent()
- ✅ Non-blocking sends (exceptions handled gracefully)

---

### Client-Side: AuctionWebSocketClient

#### Key Features:

1. **Singleton Pattern (Thread-Safe)**
   ```java
   public static synchronized AuctionWebSocketClient getInstance() {
       synchronized (INSTANCE_LOCK) {
           if (instance == null) {
               instance = new AuctionWebSocketClient(new URI("ws://localhost:8081"));
           }
           return instance;
       }
   }
   ```
   - Ensures only 1 client instance exists
   - Double-checked locking prevents overhead after first call

2. **Connection Management**
   ```java
   public void connect() throws InterruptedException {
       boolean connected = this.connectBlocking(5, TimeUnit.SECONDS);
       if (connected) {
           connectionLatch.await(5, TimeUnit.SECONDS);
       }
   }
   ```
   - Blocks until connection established
   - Timeout prevents hanging indefinitely

3. **Message Sending (Thread-Safe)**
   ```java
   public void sendBid(String phienId, String userId, double giaRa) {
       if (!isConnected) return;  // volatile check
       
       try {
           JsonObject bidMessage = new JsonObject();
           bidMessage.addProperty("action", "BID");
           // ...
           this.send(jsonString);  // Non-blocking, thread-safe
       } catch (Exception e) {
           // Handle gracefully
       }
   }
   ```
   - Can be called from any thread
   - WebSocket library handles thread-safety internally

4. **Message Reception (JavaFX Thread Switching)**
   ```java
   @Override
   public void onMessage(String message) {
       // Called on WebSocket thread
       JsonObject json = gson.fromJson(message, JsonObject.class);
       
       Platform.runLater(() -> {
           // Now on JavaFX thread
           switch (event) {
               case "BID_RESULT":
                   listener.onBidResult(json);  // Safe to update UI
           }
       });
   }
   ```

---

## 🔄 Complete Message Flow

### Scenario: User Places Bid

#### Step 1: User Action (JavaFX Thread)
```
User clicks "Place Bid"
    ↓
HomeController.onClickedBid()
    ↓
client.sendBid(phienId, userId, giaRa)  // JavaFX thread
```

#### Step 2: Send to Server (WebSocket Thread)
```
AuctionWebSocketClient.sendBid()
    ↓
this.send(JSON message)  // Non-blocking, thread-safe
    ↓
Message queued in send buffer
```

#### Step 3: Server Processing (WebSocket Server Thread)
```
AuctionWebSocketServer.onMessage()
    ↓
handleBid(conn, json)
    ↓
phienService.datGia(...)  // Service processes bid
    ↓
broadcastToRoom()  // Send result to all clients
```

#### Step 4: Client Reception (WebSocket Thread)
```
onMessage(String message)  ← Called on WebSocket thread
    ↓
Platform.runLater(() -> {  ← Schedule on JavaFX thread
    listener.onBidResult(json)
})
```

#### Step 5: UI Update (JavaFX Thread)
```
HomeController.onBidResult()  ← Called on JavaFX thread
    ↓
Update price label
Update color
Update status
```

---

## 🎯 Testing Guide

### Test Setup: Multiple Clients

```
Terminal 1: Start Server
    java -cp AppDauGia.jar com.mycompany.server.ServerApp
    Output: "🚀 AuctionWebSocketServer started on port 8081"

Terminal 2: Start Client 1
    Run JavaFX application
    Navigate to auction room
    Output: "✅ WebSocket connected successfully"

Terminal 3: Start Client 2
    Run JavaFX application (same auction session)
    Navigate to same auction room
    Output: "✅ WebSocket connected successfully"
```

### Test: Real-Time Bid Sync

1. **Client 1:** Place bid 100,000
   ```
   Expected: Client 1 updates price to 100,000
   Expected: Client 2 receives update and updates price to 100,000
   ```

2. **Client 2:** Place bid 150,000
   ```
   Expected: Client 2 updates price to 150,000
   Expected: Client 1 receives update and updates price to 150,000
   ```

3. **Close App**
   ```
   Expected: onClose() called
   Expected: User removed from room
   Expected: Other clients see "USER_LEFT" event
   ```

---

## ⚠️ Common Pitfalls

### ❌ Pitfall 1: Updating UI from WebSocket Thread
```java
@Override
public void onMessage(String message) {
    label.setText("Price updated");  // ❌ CRASH!
}
```
**Fix:** Use Platform.runLater()

### ❌ Pitfall 2: Not Synchronizing Multi-Step Operations
```java
Set<WebSocket> room = rooms.get(phienId);  // Thread A reads
room.add(client);  // Thread B replaces room between these lines
```
**Fix:** Use synchronized block or computeIfAbsent()

### ❌ Pitfall 3: Multiple Client Instances
```java
AuctionWebSocketClient client1 = new AuctionWebSocketClient(...);
AuctionWebSocketClient client2 = new AuctionWebSocketClient(...);
// ❌ Two separate connections, not synchronized
```
**Fix:** Use getInstance() singleton instead

### ❌ Pitfall 4: Not Handling Disconnection Gracefully
```java
public void sendBid(...) {
    this.send(json);  // Might throw if disconnected
}
```
**Fix:** Check isConnected() and handle exceptions

---

## 🔧 Configuration

### Server Configuration
```java
// Port: 8081
new AuctionWebSocketServer(8081)

// Timeout: 5 seconds
this.connectBlocking(5, TimeUnit.SECONDS)

// Max queue: depends on Java-WebSocket library
```

### Client Configuration
```java
// Server URI: ws://localhost:8081
new URI("ws://localhost:8081")

// Connection timeout: 5 seconds
connectionLatch.await(5, TimeUnit.SECONDS)

// Message parsing: Gson (thread-safe)
```

---

## 📚 Related Classes

- `AuctionWebSocketServer`: Server-side WebSocket handler
- `AuctionWebSocketClient`: Client-side WebSocket handler
- `AuctionWebSocketListener`: Callback interface for events
- `AuctionWebSocketServerStarter`: Server startup utility
- `PhienDauGiaService`: Database service for bids (must be thread-safe)

---

## 🚀 Next Steps

1. **Implement PhienDauGiaService.datGia()** - Must be thread-safe
2. **Implement AuctionWebSocketListener** in Controllers
3. **Start WebSocket Server** in ServerApp.main()
4. **Connect Client** in HomeController.initialize()
5. **Test with Multiple Clients** - Verify real-time sync

---

## 📖 Further Reading

- [Java-WebSocket Documentation](https://github.com/TooTallNate/Java-WebSocket)
- [JavaFX Platform.runLater()](https://docs.oracle.com/javase/8/javafx/api/javafx/application/Platform.html)
- [ConcurrentHashMap internals](https://docs.oracle.com/javase/10/docs/api/java/util/concurrent/ConcurrentHashMap.html)
- [Thread Safety in Java](https://docs.oracle.com/javase/tutorial/essential/concurrency/)

