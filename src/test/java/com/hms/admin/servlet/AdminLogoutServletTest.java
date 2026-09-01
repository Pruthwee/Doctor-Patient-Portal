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
class AdminLogoutServletTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private HttpSession mockSession;

    private AdminLogoutServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new AdminLogoutServlet();
    }

    @Test
    void doGet_removesAdminObjFromSession() throws Exception {
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockSession).removeAttribute("adminObj");
    }

    @Test
    void doGet_setsSuccessMessage() throws Exception {
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockSession).setAttribute("successMsg", "Admin Logout Successfully");
    }

    @Test
    void doGet_redirectsToAdminLoginPage() throws Exception {
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).sendRedirect("admin_login.jsp");
    }

    @Test
    void doGet_executesAllThreeOperationsInOrder() throws Exception {
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockSession, times(1)).removeAttribute("adminObj");
        verify(mockSession, times(1)).setAttribute("successMsg", "Admin Logout Successfully");
        verify(mockResponse, times(1)).sendRedirect("admin_login.jsp");
    }
}
