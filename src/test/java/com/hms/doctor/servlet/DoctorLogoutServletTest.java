package com.hms.doctor.servlet;

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
class DoctorLogoutServletTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private HttpSession mockSession;

    private DoctorLogoutServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new DoctorLogoutServlet();
    }

    @Test
    void doGet_removesDoctorObjFromSession() throws Exception {
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockSession).removeAttribute("doctorObj");
    }

    @Test
    void doGet_setsSuccessMessage() throws Exception {
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockSession).setAttribute("successMsg", "Doctor Logout Successfully.");
    }

    @Test
    void doGet_redirectsToDoctorLoginPage() throws Exception {
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockResponse).sendRedirect("doctor_login.jsp");
    }

    @Test
    void doGet_executesAllOperations() throws Exception {
        when(mockRequest.getSession()).thenReturn(mockSession);

        servlet.doGet(mockRequest, mockResponse);

        verify(mockSession, times(1)).removeAttribute("doctorObj");
        verify(mockSession, times(1)).setAttribute("successMsg", "Doctor Logout Successfully.");
        verify(mockResponse, times(1)).sendRedirect("doctor_login.jsp");
    }
}
