package com.mycompany.action;

import com.mycompany.models.User;
import com.mycompany.utils.*;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test ChangePasswordAction.handlePasswordChange()
 * Dùng Mockito để mock SessionManager và HandleNavigationAndAlert
 * vì chúng phụ thuộc vào JavaFX và DB.
 */
class ChangePasswordActionTest {

    private ChangePasswordAction changePasswordAction;

    // Dữ liệu test
    private static final String CORRECT_OLD_PASSWORD = "OldPass1!";
    private static final String NEW_STRONG_PASSWORD  = "NewPass2@";
    private static final String WEAK_PASSWORD        = "weak";

    @BeforeEach
    void setUp() {
        changePasswordAction = ChangePasswordAction.getInstance();
    }

    @Test
    @DisplayName("Mật khẩu cũ rỗng: phải hiện cảnh báo, không crash")
    void handlePasswordChange_emptyOldPassword_shouldShowAlert() {
        // Không throw exception — chỉ hiện alert (mock Alert)
        try (MockedStatic<HandleNavigationAndAlert> mockNav =
                     mockStatic(HandleNavigationAndAlert.class)) {
            HandleNavigationAndAlert mockInstance = mock(HandleNavigationAndAlert.class);
            mockNav.when(HandleNavigationAndAlert::getInstance).thenReturn(mockInstance);

            // Không được throw exception
            assertDoesNotThrow(() ->
                    changePasswordAction.handlePasswordChange("", NEW_STRONG_PASSWORD, NEW_STRONG_PASSWORD, null));

            // Phải gọi showAlert với ERROR
            verify(mockInstance).showAlert(
                    eq(javafx.scene.control.Alert.AlertType.ERROR),
                    anyString(), anyString());
        }
    }

    @Test
    @DisplayName("Mật khẩu mới không khớp xác nhận: phải cảnh báo")
    void handlePasswordChange_passwordMismatch_shouldShowAlert() {
        try (MockedStatic<HandleNavigationAndAlert> mockNav =
                     mockStatic(HandleNavigationAndAlert.class)) {
            HandleNavigationAndAlert mockInstance = mock(HandleNavigationAndAlert.class);
            mockNav.when(HandleNavigationAndAlert::getInstance).thenReturn(mockInstance);

            assertDoesNotThrow(() ->
                    changePasswordAction.handlePasswordChange(
                            CORRECT_OLD_PASSWORD, "NewPass2@", "DifferentPass3#", null));

            verify(mockInstance).showAlert(
                    eq(javafx.scene.control.Alert.AlertType.ERROR),
                    anyString(), anyString());
        }
    }

    @Test
    @DisplayName("Mật khẩu mới trùng mật khẩu cũ: phải cảnh báo")
    void handlePasswordChange_newSameAsOld_shouldShowAlert() {
        try (MockedStatic<HandleNavigationAndAlert> mockNav =
                     mockStatic(HandleNavigationAndAlert.class)) {
            HandleNavigationAndAlert mockInstance = mock(HandleNavigationAndAlert.class);
            mockNav.when(HandleNavigationAndAlert::getInstance).thenReturn(mockInstance);

            assertDoesNotThrow(() ->
                    changePasswordAction.handlePasswordChange(
                            CORRECT_OLD_PASSWORD, CORRECT_OLD_PASSWORD, CORRECT_OLD_PASSWORD, null));

            verify(mockInstance).showAlert(
                    eq(javafx.scene.control.Alert.AlertType.ERROR),
                    anyString(), anyString());
        }
    }

    @Test
    @DisplayName("Mật khẩu mới yếu: phải cảnh báo")
    void handlePasswordChange_weakNewPassword_shouldShowAlert() {
        try (MockedStatic<HandleNavigationAndAlert> mockNav =
                     mockStatic(HandleNavigationAndAlert.class)) {
            HandleNavigationAndAlert mockInstance = mock(HandleNavigationAndAlert.class);
            mockNav.when(HandleNavigationAndAlert::getInstance).thenReturn(mockInstance);

            assertDoesNotThrow(() ->
                    changePasswordAction.handlePasswordChange(
                            CORRECT_OLD_PASSWORD, WEAK_PASSWORD, WEAK_PASSWORD, null));

            verify(mockInstance).showAlert(
                    eq(javafx.scene.control.Alert.AlertType.ERROR),
                    anyString(), anyString());
        }
    }
}