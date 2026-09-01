package com.hms.user.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserLogoutServletTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private HttpSession mockSession;

    private UserLogoutServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new UserLogoutServlet();
    }

    @Test
    void doGet_removesUserObjFromSession() throws Exception {
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockSession).removeAttribute("userObj");
    }

    @Test
    void doGet_setsSuccessMessage() throws Exception {
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockSession).setAttribute("successMsg", "User Logout Successfully.");
    }

    @Test
    void doGet_redirectsToUserLoginPage() throws Exception {
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).sendRedirect("user_login.jsp");
    }

    @Test
    void doGet_executesAllOperations() throws Exception {
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockSession, times(1)).removeAttribute("userObj");
        verify(mockSession, times(1)).setAttribute("successMsg", "User Logout Successfully.");
        verify(mockResponse, times(1)).sendRedirect("user_login.jsp");
    }
}
