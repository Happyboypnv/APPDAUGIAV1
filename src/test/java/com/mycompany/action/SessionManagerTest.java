package com.mycompany.action;

import com.mycompany.models.User;
import com.mycompany.utils.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SessionManager Tests")
public class SessionManagerTest {

    private SessionManager sessionManager;
    private User testUser;

    @BeforeEach
    void setUp() {
        sessionManager = SessionManager.getInstance();

        testUser = new User("Test User", "test@example.com", "Pass1234!", "1990-01-01");
        testUser.setUserId("USER001");
    }

    @Test
    @DisplayName("Test setSession() - Should store user and token")
    void testSetSession() {
        String token = "test_token_123";

        sessionManager.setSession(testUser, token);
        User retrievedUser = sessionManager.getCurrentUser();

        assertNotNull(retrievedUser);
        assertEquals("USER001", retrievedUser.getUserId());
        assertEquals("test@example.com", retrievedUser.getEmail());
    }

    @Test
    @DisplayName("Test getCurrentUser() - Should return current user")
    void testGetCurrentUser() {
        sessionManager.setSession(testUser, "token123");

        User current = sessionManager.getCurrentUser();

        assertNotNull(current);
        assertEquals("Test User", current.getFullName());
    }

    @Test
    @DisplayName("Test setServerToken() - Should store server token")
    void testSetServerToken() {
        String serverToken = "USER_test@example.com_123456";

        sessionManager.setServerToken(serverToken);
        String retrieved = sessionManager.getServerToken();

        assertNotNull(retrieved);
        assertEquals(serverToken, retrieved);
    }

    @Test
    @DisplayName("Test getServerToken() - Should return server token")
    void testGetServerToken() {
        String token = "SERVER_TOKEN_XYZ";
        sessionManager.setServerToken(token);

        String result = sessionManager.getServerToken();

        assertEquals(token, result);
    }

    @Test
    @DisplayName("Test clearSession() - Should clear user and token")
    void testClearSession() {
        sessionManager.setSession(testUser, "token123");
        sessionManager.setServerToken("server_token");

        sessionManager.logout();

        assertNull(sessionManager.getCurrentUser());
        assertNull(sessionManager.getServerToken());
    }

    @Test
    @DisplayName("Test isLoggedIn() - Should return true when user is set")
    void testIsLoggedInTrue() {
        sessionManager.setSession(testUser, "token123");

        assertTrue(sessionManager.isLoggedIn());
    }

    @Test
    @DisplayName("Test isLoggedIn() - Should return false when no user")
    void testIsLoggedInFalse() {
        sessionManager.logout();

        assertFalse(sessionManager.isLoggedIn());
    }

    @Test
    @DisplayName("Test singleton - Should return same instance")
    void testSingletonInstance() {
        SessionManager instance1 = SessionManager.getInstance();
        SessionManager instance2 = SessionManager.getInstance();

        assertSame(instance1, instance2);
    }

    @Test
    @DisplayName("Test setSession() - Should handle null user gracefully")
    void testSetSessionNullUser() {
        assertDoesNotThrow(() -> {
            sessionManager.setSession(null, "token");
        });
    }

    @Test
    @DisplayName("Test concurrent access - Multiple threads")
    void testConcurrentAccess() throws InterruptedException {
        User user1 = new User("User 1", "user1@test.com", "Pass1234!", "1990-01-01");
        user1.setUserId("USER001");

        Thread t1 = new Thread(() -> {
            sessionManager.setSession(user1, "token1");
        });

        Thread t2 = new Thread(() -> {
            User current = sessionManager.getCurrentUser();
            assertNotNull(current);
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertNotNull(sessionManager.getCurrentUser());
    }
}

