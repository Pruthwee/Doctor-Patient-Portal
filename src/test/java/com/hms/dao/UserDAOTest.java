package com.hms.dao;

import com.hms.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDAOTest {

    @Mock
    private Connection mockConn;

    @Mock
    private PreparedStatement mockPstmt;

    @Mock
    private ResultSet mockResultSet;

    private UserDAO userDAO;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO(mockConn);
    }

    // ── Constructor test ───────────────────────────────────────────────────────

    @Test
    void constructor_withConnection_createsInstance() {
        UserDAO dao = new UserDAO(mockConn);
        assertNotNull(dao);
    }

    // ── userRegister tests ─────────────────────────────────────────────────────

    @Test
    void userRegister_success_returnsTrue() throws Exception {
        User user = new User("John Doe", "john@test.com", "password123");

        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeUpdate()).thenReturn(1);

        boolean result = userDAO.userRegister(user);

        assertTrue(result);
        verify(mockPstmt).setString(1, "John Doe");
        verify(mockPstmt).setString(2, "john@test.com");
        verify(mockPstmt).setString(3, "password123");
        verify(mockPstmt).executeUpdate();
    }

    @Test
    void userRegister_sqlException_returnsFalse() throws Exception {
        User user = new User("John Doe", "john@test.com", "password123");

        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        boolean result = userDAO.userRegister(user);

        assertFalse(result);
    }

    @Test
    void userRegister_withNullFields_returnsFalse() throws Exception {
        User user = new User(null, null, null);

        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeUpdate()).thenThrow(new RuntimeException("null constraint"));

        boolean result = userDAO.userRegister(user);

        assertFalse(result);
    }

    // ── loginUser tests ────────────────────────────────────────────────────────

    @Test
    void loginUser_validCredentials_returnsUser() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("full_name")).thenReturn("John Doe");
        when(mockResultSet.getString("email")).thenReturn("john@test.com");
        when(mockResultSet.getString("password")).thenReturn("password123");

        User result = userDAO.loginUser("john@test.com", "password123");

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("John Doe", result.getFullName());
        assertEquals("john@test.com", result.getEmail());
        assertEquals("password123", result.getPassword());
    }

    @Test
    void loginUser_invalidCredentials_returnsNull() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        User result = userDAO.loginUser("wrong@test.com", "wrongpass");

        assertNull(result);
    }

    @Test
    void loginUser_sqlException_returnsNull() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        User result = userDAO.loginUser("john@test.com", "pass");

        assertNull(result);
    }

    @Test
    void loginUser_emptyCredentials_returnsNull() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        User result = userDAO.loginUser("", "");

        assertNull(result);
    }

    // ── checkOldPassword tests ─────────────────────────────────────────────────

    @Test
    void checkOldPassword_passwordMatches_returnsTrue() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);

        boolean result = userDAO.checkOldPassword(1, "oldpass");

        assertTrue(result);
    }

    @Test
    void checkOldPassword_passwordNotMatch_returnsFalse() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        boolean result = userDAO.checkOldPassword(1, "wrongpass");

        assertFalse(result);
    }

    @Test
    void checkOldPassword_sqlException_returnsFalse() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        boolean result = userDAO.checkOldPassword(1, "pass");

        assertFalse(result);
    }

    @Test
    void checkOldPassword_withZeroUserId_returnsFalse() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        boolean result = userDAO.checkOldPassword(0, "pass");

        assertFalse(result);
    }

    // ── changePassword tests ───────────────────────────────────────────────────

    @Test
    void changePassword_success_returnsTrue() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeUpdate()).thenReturn(1);

        boolean result = userDAO.changePassword(1, "newpassword");

        assertTrue(result);
        verify(mockPstmt).setString(1, "newpassword");
        verify(mockPstmt).setInt(2, 1);
        verify(mockPstmt).executeUpdate();
    }

    @Test
    void changePassword_sqlException_returnsFalse() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        boolean result = userDAO.changePassword(1, "newpassword");

        assertFalse(result);
    }

    @Test
    void changePassword_withEmptyPassword_returnsTrue() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeUpdate()).thenReturn(1);

        boolean result = userDAO.changePassword(1, "");

        assertTrue(result);
    }
}
