package com.mycompany.utils;

import com.mycompany.models.NguoiDung;
import java.util.Map;

public class SessionManager {
    private static SessionManager instance;
    private NguoiDung currentUser;
    private String currentToken;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setSession(NguoiDung user, String token) {
        this.currentUser = user;
        this.currentToken = token;
    }

    public NguoiDung getCurrentUser() {
        return currentUser;
    }

    public String getCurrentToken() {
        return currentToken;
    }

    public Map<String, Object> getCurrentUserInfo() {
        if (currentToken != null) {
            return TokenUtil.getUserInfoFromToken(currentToken);
        }
        return null;
    }

    public boolean isLoggedIn() {
        return currentUser != null && currentToken != null && TokenUtil.validateToken(currentToken);
    }

    public void logout() {
        this.currentUser = null;
        this.currentToken = null;
    }
}
