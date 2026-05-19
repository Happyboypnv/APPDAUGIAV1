package com.mycompany.utils;

import com.mycompany.server.dto.LoginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SessionSyncManager - Client-side session validation and conflict handling
 *
 * MỤC ĐÍCH:
 * - Track current session status on client side
 * - Handle ALREADY_IN_USE responses from server
 * - Prevent multiple simultaneous logins on same device
 * - Display meaningful error messages for session conflicts
 *
 * TÍNH NĂNG:
 * - Check session conflict responses from login endpoint
 * - Handle "alreadyInUse" state gracefully
 * - Show existing device info if user already logged in
 * - Provide suggestion to user (wait for timeout or logout on other device)
 *
 * USAGE:
 * - Check response status after login attempt
 * - Display appropriate error message to user
 * - Allow user to choose: wait for timeout or try other device
 *
 * DESIGN:
 * - Static utility methods (no state needed)
 * - Thread-safe for concurrent login attempts
 */
public class SessionSyncManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionSyncManager.class);

    /**
     * CHECK: Is login response indicating session conflict?
     *
     * @param response LoginResponse from server
     * @return true if user is already logged in on another device
     */
    public static boolean isSessionConflict(LoginResponse response) {
        if (response == null) return false;
        String status = response.getSessionStatus();
        return status != null && status.equals("ALREADY_IN_USE");
    }

    /**
     * CHECK: Is login successful?
     *
     * @param response LoginResponse from server
     * @return true if login succeeded
     */
    public static boolean isLoginSuccess(LoginResponse response) {
        if (response == null) return false;
        String status = response.getSessionStatus();
        return status != null && status.equals("SUCCESS");
    }

    /**
     * GET CONFLICT MESSAGE: User-friendly error message for session conflict
     *
     * Shows:
     * - That they're already logged in
     * - On which device (if available)
     * - Suggestion to wait or logout on other device
     *
     * @param response LoginResponse from server
     * @return Formatted error message for UI
     */
    public static String getConflictMessage(LoginResponse response) {
        if (response == null) {
            return "Lỗi kết nối server. Vui lòng thử lại.";
        }

        StringBuilder message = new StringBuilder();

        // Add main message
        String thongBao = response.getThongBao();
        if (thongBao != null && !thongBao.isEmpty()) {
            message.append(thongBao);
        } else {
            message.append("⚠️ Tài khoản của bạn đang đăng nhập trên thiết bị khác.");
        }

        message.append("\n\n");

        // Add existing device info if available
        String deviceId = response.getExistingDeviceId();
        String ipAddress = response.getExistingIpAddress();

        if (deviceId != null || ipAddress != null) {
            message.append("Thiết bị hiện tại: ");
            if (ipAddress != null) {
                message.append("IP ").append(ipAddress);
            }
            if (deviceId != null) {
                message.append(" (").append(deviceId).append(")");
            }
            message.append("\n\n");
        }

        // Add suggestions
        message.append("Lựa chọn của bạn:\n");
        message.append("1. Đợi 30 phút để session cũ hết hạn\n");
        message.append("2. Đăng xuất trên thiết bị kia trước\n");
        message.append("3. Thử đăng nhập trên thiết bị khác");

        return message.toString();
    }

    /**
     * HANDLE LOGIN RESPONSE: Process response from login endpoint
     *
     * Returns status:
     * - LOGIN_SUCCESS: User logged in successfully
     * - SESSION_CONFLICT: User already logged in on another device
     * - LOGIN_FAILED: Invalid credentials
     * - NETWORK_ERROR: Cannot connect to server
     *
     * @param response LoginResponse from server
     * @return Status string
     */
    public static String handleLoginResponse(LoginResponse response) {
        if (response == null) {
            logger.error("[SessionSyncManager] ❌ Login response is null (network error?)");
            return "NETWORK_ERROR";
        }

        String status = response.getSessionStatus();

        if (status != null && status.equals("SUCCESS")) {
            logger.info("[SessionSyncManager] ✅ Login successful");
            return "LOGIN_SUCCESS";
        }

        if (status != null && status.equals("ALREADY_IN_USE")) {
            String existingDevice = response.getExistingDeviceId() != null
                    ? response.getExistingDeviceId()
                    : "unknown device";
            logger.warn("[SessionSyncManager] ⚠️ Session conflict: user already logged in on {}",
                    existingDevice);
            return "SESSION_CONFLICT";
        }

        // Authentication failed (wrong credentials or other error)
        logger.warn("[SessionSyncManager] ❌ Login failed: {}", response.getThongBao());
        return "LOGIN_FAILED";
    }

    /**
     * LOG SESSION INFO: Debug logging of session info
     *
     * @param response LoginResponse
     * @param email User's email
     */
    public static void logSessionInfo(LoginResponse response, String email) {
        if (response == null) return;

        logger.info("[SessionSyncManager] 📊 Session Info for {}:", email);
        logger.info("  - Status: {}", response.getSessionStatus());
        logger.info("  - Token: {} ({})",
                response.getToken() != null ? "✓ Present" : "✗ Missing",
                response.getToken() != null ? response.getToken().substring(0, Math.min(20, response.getToken().length())) + "..." : "N/A");
        logger.info("  - Message: {}", response.getThongBao());

        if (response.getExistingDeviceId() != null) {
            logger.info("  - Existing Device: {}", response.getExistingDeviceId());
        }
        if (response.getExistingIpAddress() != null) {
            logger.info("  - Existing IP: {}", response.getExistingIpAddress());
        }
    }
}

