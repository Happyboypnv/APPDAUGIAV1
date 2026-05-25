package com.mycompany.utils;

import com.mycompany.models.User;
import java.util.Map;

public class SessionManager {

    private static volatile SessionManager instance;
    private volatile User currentUser;
    private volatile String currentToken;       // Token local (Base64) — dùng để đọc thông tin user
    private volatile String serverToken;        // Token server (USER_...) — dùng để gọi API
    private volatile String currentPhienId;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    /** Gọi sau khi đăng nhập thành công */
    public void setSession(User user, String token) {
        this.currentUser = user;
        this.currentToken = token;
    }

    /**
     * Lưu thêm server token (dạng USER_email_timestamp) nhận từ server.
     * Dùng token này khi gọi các API cần xác thực (tạo phiên, đặt giá...).
     */
    public void setServerToken(String serverToken) {
        this.serverToken = serverToken;
    }

    /** Token server dạng USER_... để gọi API */
    public String getServerToken() {
        return serverToken;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getCurrentToken() {
        return currentToken;
    }

    public String getCurrentPhienId() {
        return currentPhienId;
    }

    public void setCurrentPhienId(String currentPhienId) {
        this.currentPhienId = currentPhienId;
    }

    public Map<String, Object> getCurrentUserInfo() {
        if (currentToken != null) {
            return TokenUtil.getUserInfoFromToken(currentToken);
        }
        return null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void logout() {
        this.currentUser = null;
        this.currentToken = null;
        this.serverToken = null;
        this.currentPhienId = null;
    }
}