package com.hms.user.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyNewServletTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    private MyNewServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new MyNewServlet();
    }

    @Test
    void constructor_createsNonNullInstance() {
        MyNewServlet s = new MyNewServlet();
        assertNotNull(s);
    }

    @Test
    void doGet_writesContextPathToResponse() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        when(mockRequest.getContextPath()).thenReturn("/myapp");
        when(mockResponse.getWriter()).thenReturn(pw);

        servlet.doGet(mockRequest, mockResponse);

        pw.flush();
        String output = sw.toString();
        assertTrue(output.contains("Served at: "));
        assertTrue(output.contains("/myapp"));
    }

    @Test
    void doPost_delegatesToDoGet() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        when(mockRequest.getContextPath()).thenReturn("/myapp");
        when(mockResponse.getWriter()).thenReturn(pw);

        servlet.doPost(mockRequest, mockResponse);

        pw.flush();
        String output = sw.toString();
        assertTrue(output.contains("Served at: "));
    }

    @Test
    void doGet_withEmptyContextPath_writesServedAt() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        when(mockRequest.getContextPath()).thenReturn("");
        when(mockResponse.getWriter()).thenReturn(pw);

        servlet.doGet(mockRequest, mockResponse);

        pw.flush();
        assertTrue(sw.toString().contains("Served at: "));
    }
}
