# 🏗️ WebSocket Architecture - Visual Diagrams

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                     COMPLETE SYSTEM DIAGRAM                         │
└─────────────────────────────────────────────────────────────────────┘

╔════════════════════╗                         ╔════════════════════╗
║   JavaFX Client    ║                         ║  Java Server       ║
║  (Port: Auto)      ║                         ║  (Port: 8081)      ║
╚════════════════════╝                         ╚════════════════════╝
         │                                            │
         │           WS Protocol                      │
         │            JSON Messages                   │
         │◄──────────────────────────────────────────►│
         │                                            │
    ┌────▼────────────┐                     ┌────────▼─────────┐
    │ WebSocket       │                     │ WebSocket        │
    │ Client          │                     │ Server           │
    │                 │                     │                  │
    │ - singleton     │                     │ - room mgmt      │
    │ - non-blocking  │                     │ - broadcast      │
    │ - thread-safe   │                     │ - thread-safe    │
    └────┬────────────┘                     └────────┬─────────┘
         │                                           │
    ┌────▼─────────────────┐                ┌────────▼──────────────┐
    │ onMessage()          │                │ onMessage()           │
    │ (WebSocket Thread)   │                │ (WebSocket Thread)    │
    │                      │                │                       │
    │ 1. Parse JSON        │                │ 1. Parse JSON         │
    │ 2. Call listener     │                │ 2. Validate bid       │
    │ 3. Platform.         │                │ 3. Call service       │
    │    runLater()        │                │ 4. Broadcast result   │
    └────┬─────────────────┘                └────────┬──────────────┘
         │                                           │
    ┌────▼────────────────────┐            ┌────────▼────────────────┐
    │ HomeController          │            │ PhienDauGiaService      │
    │ (JavaFX Thread)         │            │ (Service Thread)        │
    │                         │            │                        │
    │ - Update price label    │            │ ⚠️ MUST BE             │
    │ - Show bid result       │            │    THREAD-SAFE!        │
    │ - Enable/disable button │            │ - Validate price       │
    │ - Show notifications    │            │ - Update database      │
    └─────────────────────────┘            └────────────────────────┘
         │
    ┌────▼──────────────┐
    │ JavaFX UI         │
    │ - Price label     │
    │ - Bid button      │
    │ - Alerts          │
    └───────────────────┘
```

---

## Thread Model

```
┌────────────────────────────────────────────────────────────────────┐
│                      THREAD EXECUTION MODEL                       │
└────────────────────────────────────────────────────────────────────┘

╔════════════════════════════════════════════════════════════════╗
║                         TIME ──────>                          ║
╚════════════════════════════════════════════════════════════════╝

┌─ JAVAFX THREAD ─────────────────────────────────────────────────┐
│                                                                 │
│  User clicks bid  Connect logic   Platform.runLater()          │
│        │              │                   │                    │
│        ▼              ▼                   ▼                    │
│    [UI EVENT]   [WS CONNECT]       [UPDATE UI]                │
│        │              │                   │                    │
│        └──onClickedBid()                  └──onBidResult()    │
│          sendBid()                                             │
│                                                                 │
└─────────────────────┬──────────────────────────────────────────┘
                      │
                      │ (async send)
                      ▼
┌─ WEBSOCKET THREAD──────────────────────────────────────────────┐
│                                                                 │
│  send() [non-blocking]    onMessage()    Platform.runLater()  │
│         │                    │                   │             │
│         ▼                    ▼                   ▼             │
│   [MESSAGE QUEUE]      [PARSE JSON]        [SCHEDULE UI]      │
│                                                                │
│          ────────────>  [NETWORK]        ────────────>        │
│                                                                 │
└────────────────┬─────────────────────────────────────────────┘
                 │
                 ▼
┌─ SERVER WEBSOCKET THREAD ───────────────────────────────────────┐
│                                                                 │
│  onMessage()           broadcastToRoom()                       │
│       │                     │                                  │
│       ▼                     ▼                                  │
│  [HANDLE BID]      [SYNCHRONIZED LOOP]                        │
│       │                     │                                  │
│       └─> datGia()          └─> send() to all clients        │
│           [SERVICE]                                            │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## Data Structure Thread Safety

```
┌──────────────────────────────────────────────────────────────────┐
│              THREAD-SAFE DATA STRUCTURES                        │
└──────────────────────────────────────────────────────────────────┘

╔═══════════════════════════════════════════════════════════════╗
║ ConcurrentHashMap: rooms Map                                 ║
╚═══════════════════════════════════════════════════════════════╝

Data:
  {
    "PHIEN_001": Set{client1, client2, client3},
    "PHIEN_002": Set{client4, client5},
    "PHIEN_003": Set{client6}
  }

Thread Access Pattern:

  Thread A              Thread B              Thread C
    │                    │                      │
    ├─ get(PHIEN_001)   ├─ put(PHIEN_002)    ├─ remove(PHIEN_003)
    │  [Segment 1]      │  [Segment 2]        │  [Segment 3]
    │  (can proceed)    │  (can proceed)      │  (can proceed)
    │                   │                      │
    └─ All 3 threads work simultaneously! ───┘

✅ RESULT: True parallelism (not just locking)


╔═══════════════════════════════════════════════════════════════╗
║ Synchronized Blocks: Room Modification                       ║
╚═══════════════════════════════════════════════════════════════╝

Code:
  Set<WebSocket> room = rooms.get(phienId);
  synchronized (room) {
      room.add(client);  // Atomic operation
  }

Thread Access:

  Thread A                    Thread B
    │                           │
    ├─ Get room reference       │
    ├─ Acquire room lock        │
    │  ✓ LOCKED                 ├─ Get room reference
    │  Add client               ├─ Wait for room lock
    │  Release lock             │  ⏳ BLOCKED
    │  ✓ UNLOCKED               │
    │                           ├─ Acquire room lock
    │                           │  ✓ LOCKED
    │                           ├─ Add client
    │                           └─ Release lock

✅ RESULT: Operations are
           serialized but safe


╔═══════════════════════════════════════════════════════════════╗
║ volatile Field: Connection Status                             ║
╚═══════════════════════════════════════════════════════════════╝

Field:
  private volatile boolean isConnected = false;

Write Operation:
  isConnected = true;
  └─> Write to CPU cache
  └─> Flush to main memory immediately
  └─> All threads see the change

Read Operation:
  if (isConnected) { ... }
  └─> Read from main memory (not cache)
  └─> Always see latest value

Example Timeline:

  Time  Thread A           Main Memory         Thread B
  0     isConnected=false  [false]            isConnected=?
  1     isConnected=true   [false]            isConnected=?
  2     (flush)            [true]             isConnected=?
  3     (cache updated)    [true]             if (isConnected)
  4                        [true]             ✓ See true!

✅ RESULT: All threads see
           latest value
```

---

## Message Sequence Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                   MESSAGE FLOW SEQUENCE                       │
└────────────────────────────────────────────────────────────────┘

JavaFX Thread          WebSocket Client       Server             DB
     │                      │                  │                 │
     │ onClickedBid()       │                  │                 │
     ├─────────────────────>│                  │                 │
     │                      │                  │                 │
     │                      │ sendBid()        │                 │
     │                      │ (parse JSON)     │                 │
     │                      │                  │                 │
     │                      │ send message     │                 │
     │                      ├─────────────────>│                 │
     │                      │                  │ onMessage()     │
     │                      │                  ├───────────────> │
     │                      │                  │ handleBid()     │
     │                      │                  │ datGia()        │
     │                      │                  │ UPDATE          │
     │                      │                  │ (DB transaction)│
     │                      │                  │<───────────────┤
     │                      │                  │ Success         │
     │                      │                  │                 │
     │                      │ broadcast()      │                 │
     │                      │<─────────────────┤                 │
     │                      │ BID_RESULT JSON  │                 │
     │                      │ (WebSocket thread)│                │
     │                      │                  │                 │
     │ Platform.runLater()  │                  │                 │
     │<─────────────────────┤                  │                 │
     │ (Switch to JavaFX    │                  │                 │
     │  thread queue)       │                  │                 │
     │                      │                  │                 │
     │ onBidResult()        │                  │                 │
     │ Called on JavaFX     │                  │                 │
     ├─> Update UI         │                  │                 │
     │ Label.setText()      │                  │                 │
     │ Show alert           │                  │                 │
     │                      │                  │                 │
     │ ✅ Complete          │                  │                 │
     ▼                      ▼                  ▼                 ▼
```

---

## Platform.runLater() Thread Switching

```
┌──────────────────────────────────────────────────────────────┐
│            JAVAFX THREAD SWITCHING MECHANISM                │
└──────────────────────────────────────────────────────────────┘

❌ WRONG: Direct UI Update from WebSocket Thread

  onMessage() (WebSocket Thread)
      │
      ├─ Parse JSON
      │
      └─ label.setText("Update")    ← ❌ EXCEPTION!
         UI Access violation
         "Not on JavaFX thread"


✅ CORRECT: Platform.runLater()

  onMessage() (WebSocket Thread)
      │
      ├─ Parse JSON
      │
      └─ Platform.runLater(() -> {
             // Code to execute on JavaFX thread
             label.setText("Update");   ← ✅ Safe!
         });
         │
         └─> Schedule on JavaFX event queue


Timeline:

  WebSocket Thread              JavaFX Thread              JavaFX Queue
      │                             │                          │
      ├─ onMessage()                │                          │
      │  Parse JSON                 │                          │
      │  Platform.runLater()        │                          │
      │  ────────────────────────────┴─> ADD TO QUEUE ─────────┤
      │                             │                   Runnable
      │  Return                     │                          │
      │  (non-blocking)             │                          │
      │                             │                          │
      │                             │  Process Queue          │
      │                             ├─────────────────────────>│
      │                             │  Execute Runnable       │
      │                             │  label.setText()        │
      │                             │<─────────────────────────┤
      │                             │  Complete               │
      ▼                             ▼                          ▼


Thread Safety Guarantees:

┌─────────────────────────────────────┐
│ WebSocket Thread                    │
├─────────────────────────────────────┤
│ ✓ Can receive data                  │
│ ✓ Can parse JSON                    │
│ ✓ Can call Platform.runLater()      │
│ ✗ CANNOT update UI                  │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ JavaFX Thread (Inside runLater)     │
├─────────────────────────────────────┤
│ ✓ Can receive data                  │
│ ✓ Can parse JSON                    │
│ ✓ Can update UI                     │
│ ✓ Can call services                 │
│ ✓ All JavaFX operations are safe    │
└─────────────────────────────────────┘
```

---

## Connection Synchronization

```
┌──────────────────────────────────────────────────────────────┐
│           COUNTDOWNLATCH SYNCHRONIZATION                    │
└──────────────────────────────────────────────────────────────┘

Main Thread                WebSocket Thread
     │                           │
     ├─ client.connect()         │
     │  ├─ connectBlocking()     │
     │  │                        └─> Server connects
     │  │                        │
     │  │                        └─> onOpen() called
     │  └─ connectionLatch       │
     │     .await(5s)            │
     │     (WAIT HERE)           │
     │     ⏳ BLOCKED            ├─ connectionLatch
     │                           │  .countDown()
     │     ✓ UNBLOCKED           │
     │ (count = 0)               │
     │                           │
     └─> Return successfully     │
         Connected!              ▼


CountDownLatch Behavior:

  Create: CountDownLatch(1)
          count = 1

  Main thread:
    await() ─┐
             ├─> BLOCKED if count > 0
            count == 0
             UNBLOCKED

  WebSocket thread:
    countDown() ─┐
                ├─> count--
                   (1 ─> 0)
                   NOTIFY all waiting threads


Result:
  ✅ Main thread waits until WebSocket connection ready
  ✅ Prevents race conditions (onOpen not called yet)
  ✅ Timeout prevents indefinite blocking (5s)
```

---

## Concurrent Bid Processing

```
┌──────────────────────────────────────────────────────────────┐
│        MULTIPLE CONCURRENT BIDS (Server-Side)              │
└──────────────────────────────────────────────────────────────┘

Time  Client-1            Client-2            Server            DB
      onClickedBid()      onClickedBid()
        │                   │
        ├─ sendBid()       ├─ sendBid()
        │  100,000         │  150,000
        │                   │
        └──────────────────>│                   onMessage()
                             │<──────────────────┤
                             │                   │
                             │                   ├─ handleBid()
                             │                   │  synchronized
                             │                   │  (room) {
                             │                   │    broadcastToRoom()
                             │                   │  }
                             │                   │
                             │<──────────────────┤
                             │                   │ GET CURRENT PRICE
                             │                   │ FROM DB (100,000)
                             │                   │
                             │<──────────────────┤
    ┌─────────────────────┐ │                   │
    │ Both receive        │ │                   │
    │ BID_RESULT: 150,000 │ │                   │
    │                     │ │                   │
    │ ✅ Synchronized!    │ │                   │
    └─────────────────────┘ │                   │
                             │                   │
                             ▼                   ▼

❌ Without synchronization:
  Race condition: Who wins?
    - Client 1 reads price (100,000)
    - Client 2 reads price (100,000)
    - Both calculate new price
    - Only highest wins (depends on timing)

✅ With synchronization:
  Mutual exclusion: handleBid() synchronized
    - Only one bid processed at a time
    - Each bid sees correct current price
    - Results are consistent across all clients
```

---

## File Organization

```
src/main/java/com/mycompany/
│
├── websocket/
│   ├── AuctionWebSocketClient.java       ◄── Client handler
│   │   ├─ getInstance() [synchronized]
│   │   ├─ connect() [CountDownLatch]
│   │   ├─ sendBid() [thread-safe]
│   │   ├─ onMessage() [Platform.runLater]
│   │   └─ Listener callback system
│   │
│   └── AuctionWebSocketListener.java     ◄── Callback interface
│       ├─ onBidResult()
│       ├─ onUserJoined()
│       ├─ onUserLeft()
│       ├─ onConnected()
│       ├─ onDisconnected()
│       └─ onError()
│
├── controller/websocket/
│   └── AuctionWebSocketControllerAdapter.java  ◄── UI Integration
│       └─ implements AuctionWebSocketListener
│           ├─ Updates UI
│           ├─ Shows alerts
│           └─ Sends bids
│
└── server/websocket/
    ├── AuctionWebSocketServer.java      ◄── Server handler
    │   ├─ onMessage() [message routing]
    │   ├─ handleJoin() [room management]
    │   ├─ handleBid() [bid processing]
    │   ├─ broadcastToRoom() [synchronized]
    │   └─ rooms [ConcurrentHashMap]
    │
    └── AuctionWebSocketServerStarter.java  ◄── Server startup
        ├─ startServer()
        ├─ stopServer()
        └─ isRunning()

Resources/
│
├── WEBSOCKET_IMPLEMENTATION_GUIDE.md    ◄── Detailed docs
├── WEBSOCKET_QUICKSTART.md              ◄── Quick start
└── WEBSOCKET_SUMMARY.md                 ◄── This summary
```

---

## Summary: Thread Safety Mechanisms

| Mechanism | Location | Purpose | Impact |
|-----------|----------|---------|--------|
| **ConcurrentHashMap** | Server rooms | Multi-threaded map access | ✅ True parallelism |
| **synchronized()** | Room operations | Atomic multi-step ops | ✅ No race conditions |
| **volatile** | isConnected | Cross-thread visibility | ✅ Latest value |
| **Platform.runLater()** | onMessage() | UI thread switching | ✅ No UI exceptions |
| **CountDownLatch** | connect() | Thread synchronization | ✅ Blocked until ready |
| **Non-blocking sends** | sendBid() | Async communication | ✅ No thread blocking |

---

All mechanisms work together to ensure **100% thread-safe** real-time auction bidding! 🎉

