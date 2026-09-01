package com.hms.admin.servlet;

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
class AdminLoginServletTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private HttpSession mockSession;

    private AdminLoginServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new AdminLoginServlet();
    }

    @Test
    void doPost_validAdminCredentials_redirectsToAdminIndex() throws Exception {
        when(mockRequest.getParameter("email")).thenReturn("admin@gmail.com");
        when(mockRequest.getParameter("password")).thenReturn("admin");
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doPost(mockRequest, mockResponse);

        verify(mockSession).setAttribute(eq("adminObj"), any());
        verify(mockResponse).sendRedirect("admin/index.jsp");
    }

    @Test
    void doPost_invalidEmail_redirectsToAdminLogin() throws Exception {
        when(mockRequest.getParameter("email")).thenReturn("wrong@gmail.com");
        when(mockRequest.getParameter("password")).thenReturn("admin");
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doPost(mockRequest, mockResponse);

        verify(mockSession).setAttribute("errorMsg", "Invalid Username or Password.");
        verify(mockResponse).sendRedirect("admin_login.jsp");
    }

    @Test
    void doPost_invalidPassword_redirectsToAdminLogin() throws Exception {
        when(mockRequest.getParameter("email")).thenReturn("admin@gmail.com");
        when(mockRequest.getParameter("password")).thenReturn("wrongpassword");
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doPost(mockRequest, mockResponse);

        verify(mockSession).setAttribute("errorMsg", "Invalid Username or Password.");
        verify(mockResponse).sendRedirect("admin_login.jsp");
    }

    @Test
    void doPost_bothInvalid_redirectsToAdminLogin() throws Exception {
        when(mockRequest.getParameter("email")).thenReturn("notadmin@gmail.com");
        when(mockRequest.getParameter("password")).thenReturn("notadmin");
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doPost(mockRequest, mockResponse);

        verify(mockSession).setAttribute("errorMsg", "Invalid Username or Password.");
        verify(mockResponse).sendRedirect("admin_login.jsp");
    }

    @Test
    void doPost_emptyCredentials_redirectsToAdminLogin() throws Exception {
        when(mockRequest.getParameter("email")).thenReturn("");
        when(mockRequest.getParameter("password")).thenReturn("");
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doPost(mockRequest, mockResponse);

        verify(mockSession).setAttribute("errorMsg", "Invalid Username or Password.");
        verify(mockResponse).sendRedirect("admin_login.jsp");
    }

    @Test
    void doPost_nullCredentials_redirectsToAdminLogin() throws Exception {
        when(mockRequest.getParameter("email")).thenReturn(null);
        when(mockRequest.getParameter("password")).thenReturn(null);
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doPost(mockRequest, mockResponse);

        verify(mockSession).setAttribute("errorMsg", "Invalid Username or Password.");
        verify(mockResponse).sendRedirect("admin_login.jsp");
    }
}
