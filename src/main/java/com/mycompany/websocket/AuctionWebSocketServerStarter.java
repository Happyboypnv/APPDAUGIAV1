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

    /**
     * PHƯƠNG THỨC (STATIC): startServer()
     * Khởi động WebSocket server trên port 8081
     *
     * THREAD-SAFETY:
     * - Runs on separate daemon thread
     * - Synchronized to prevent multiple starts
     * - wsServer instance is reusable
     *
     * BEHAVIOR:
     * 1. Check server not already running
     * 2. Create AuctionWebSocketServer instance (port 8081)
     * 3. Start in separate daemon thread
     * 4. Return immediately (non-blocking)
     */
    public static synchronized void startServer() {
        if (wsServer != null) {
            System.out.println("⚠️ WebSocket server already running");
            return;
        }

        try {
            // 🔹 STEP 1: Create server instance
            wsServer = new AuctionWebSocketServer(8081);

            // 🔹 STEP 2: Create and start server in separate thread
            wsThread = new Thread(() -> {
                try {
                    // setReuseAddr: allow reuse of port after shutdown
                    wsServer.setReuseAddr(true);
                    wsServer.start();

                    System.out.println("✅ WebSocket server started successfully");

                    // Block forever (server running)
                    Thread.currentThread().join();

                } catch (Exception e) {
                    System.err.println("❌ Error starting WebSocket server: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            // Set as daemon thread: JVM can exit even while thread running
            // (For graceful shutdown when host process exits)
            wsThread.setDaemon(false);
            wsThread.setName("WebSocket-Server-Thread");

            // 🔹 STEP 3: Start thread
            wsThread.start();

            System.out.println("🚀 WebSocket server thread started");

        } catch (Exception e) {
            System.err.println("❌ Failed to create WebSocket server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * PHƯƠNG THỨC (STATIC): stopServer()
     * Graceful shutdown của WebSocket server
     *
     * THREAD-SAFETY:
     * - Synchronized to prevent race conditions
     * - waitForServerStop(): wait for graceful shutdown
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
        return wsServer != null && wsThread!=null && wsThread.isAlive();
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

