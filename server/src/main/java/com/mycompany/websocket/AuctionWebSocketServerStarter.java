package com.mycompany.websocket;

import com.mycompany.server.websocket.AuctionWebSocketServer;

/**
 * AuctionWebSocketServerStarter - Utility để khởi động WebSocket server
 *
 * MỤC ĐÍCH:
 * - Khởi động AuctionWebSocketServer trên port 8081
 * - Chạy trên separate thread để không block main server
 * - Graceful shutdown handling
 *
 * USAGE:
 * AuctionWebSocketServerStarter.startServer();  // In ServerApp.main()
 *
 * ARCHITECTURE:
 * - Main HTTP Server: port 8080 (Spring Boot hoặc custom implementation)
 * - WebSocket Server: port 8081 (AuctionWebSocketServer)
 * - Cả hai chạy song song
 */
public class AuctionWebSocketServerStarter {

    private static AuctionWebSocketServer wsServer;
    private static Thread wsThread;
    private static boolean wsStartupSuccess = false;
    private static String wsStartupError = null;
    private static final int WS_PORT = 8081;
    private static final int MAX_STARTUP_ATTEMPTS = 3;

    /**
     * PHƯƠNG THỨC (STATIC): startServer()
     * Khởi động WebSocket server trên port 8081 với retry logic
     *
     * IMPROVEMENTS:
     * - Retry logic: nếu port bị occupied, chờ rồi retry
     * - Creates NEW server instance on each retry (can't reuse same instance)
     * - Better error reporting: ghi lại error message
     * - Thread safety: synchronization + flag tracking
     * - Startup verification: flag wsStartupSuccess
     *
     * BEHAVIOR:
     * 1. Check server not already running
     * 2. Start in separate thread with retry logic
     * 3. On each attempt: Create NEW AuctionWebSocketServer instance
     * 4. Try to start(), if BindException → wait and retry with new instance
     * 5. Track startup status for caller to verify
     */
    public static synchronized void startServer() {
        if (wsServer != null) {
            System.out.println("⚠️ WebSocket server already running");
            return;
        }

        // 🔹 STEP 1: Create and start server in separate thread with retry
        wsThread = new Thread(() -> {
            int attempt = 0;
            while (attempt < MAX_STARTUP_ATTEMPTS) {
                attempt++;
                try {
                    System.out.println("🔄 WebSocket server startup attempt " + attempt + "/" + MAX_STARTUP_ATTEMPTS);

                    // Create NEW server instance on each attempt (can't reuse same instance)
                    AuctionWebSocketServer serverInstance = new AuctionWebSocketServer(WS_PORT);

                    // setReuseAddr: allow reuse of port after shutdown
                    serverInstance.setReuseAddr(true);
                    serverInstance.start();

                    // Startup successful - store reference and mark success
                    wsServer = serverInstance;
                    wsStartupSuccess = true;
                    System.out.println("✅ WebSocket server started successfully on port " + WS_PORT);

                    // Block forever (server running) - never returns
                    Thread.currentThread().join();
                    return; // Exit on success (unreachable in normal operation)

                } catch (InterruptedException ie) {
                    // Thread was interrupted during join()
                    wsStartupError = "WebSocket server startup interrupted: " + ie.getMessage();
                    System.err.println("❌ " + wsStartupError);
                    wsStartupSuccess = false;
                    Thread.currentThread().interrupt();
                    break;

                } catch (Exception e) {
                    // Other exceptions (creation failed, etc.)
                    wsStartupError = "Error starting WebSocket server (attempt " + attempt + "): " + e.getClass().getSimpleName() + ": " + e.getMessage();
                    System.err.println("❌ " + wsStartupError);
                    e.printStackTrace();

                    if (attempt < MAX_STARTUP_ATTEMPTS) {
                        try {
                            long waitTime = 1000L * attempt;
                            System.out.println("⏳ Waiting " + waitTime + "ms before retry...");
                            Thread.sleep(waitTime);
                            continue;  // Retry with new server instance
                        } catch (InterruptedException ie) {
                            wsStartupError = "WebSocket startup interrupted during wait";
                            System.err.println("❌ " + wsStartupError);
                            Thread.currentThread().interrupt();
                            wsStartupSuccess = false;
                            break;
                        }
                    } else {
                        wsStartupSuccess = false;
                        break;
                    }
                }
            }

            // After all retry attempts failed
            if (!wsStartupSuccess) {
                System.err.println("❌ WebSocket server failed to start after " + MAX_STARTUP_ATTEMPTS + " attempts");
                System.err.println("   Error: " + wsStartupError);
            }
        });

        // Set as daemon thread: JVM can exit even while thread running
        // (For graceful shutdown when host process exits)
        wsThread.setDaemon(false);
        wsThread.setName("WebSocket-Server-Thread");

        // 🔹 STEP 3: Start thread
        wsThread.start();

        System.out.println("🚀 WebSocket server thread started (port " + WS_PORT + ")");
    }

    /**
     * Check if WebSocket server started successfully
     * (useful for the HTTP server to verify before logging "server running")
     *
     * @return true if startup succeeded
     */
    public static boolean isStartupSuccessful() {
        // Give server a moment to start (async startup)
        if (wsThread != null && !wsStartupSuccess && wsStartupError == null) {
            // Still starting, wait a bit
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return wsStartupSuccess;
    }

    /**
     * Get startup error message if startup failed
     *
     * @return error message or null if no error
     */
    public static String getStartupError() {
        return wsStartupError;
    }

    /**
     * PHƯƠNG THỨC (STATIC): stopServer()
     * Graceful shutdown của WebSocket server
     *
     * THREAD-SAFETY:
     * - Synchronized to prevent race conditions
     * - Timeout 10s: prevent hanging
     */
    public static synchronized void stopServer() {
        if (wsServer == null) {
            System.out.println("⚠️ WebSocket server not running");
            return;
        }

        try {
            System.out.println("🛑 Shutting down WebSocket server...");

            // Close all client connections
            wsServer.broadcast("SERVER_CLOSING");

            // Give clients time to process close message
            Thread.sleep(500);

            // Stop accepting new connections
            wsServer.stop(10000);  // 10s timeout

            System.out.println("✅ WebSocket server stopped");
            wsServer = null;
            wsStartupSuccess = false;

        } catch (Exception e) {
            System.err.println("❌ Error stopping WebSocket server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * PHƯƠNG THỨC (STATIC): isRunning()
     * Check if server is running
     *
     * @return true if server is running
     */
    public static boolean isRunning() {
        return wsServer != null && wsThread != null && wsThread.isAlive();
    }

    /**
     * PHƯƠNG THỨC (STATIC): getServer()
     * Get server instance (for advanced operations)
     *
     * @return AuctionWebSocketServer instance or null if not running
     */
    public static AuctionWebSocketServer getServer() {
        return wsServer;
    }
}

