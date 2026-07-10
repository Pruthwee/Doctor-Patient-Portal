package com.hms.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.hms.entity.Appointment;

class AppointmentDAOTest {

    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement mockPreparedStatement;
    @Mock
    private ResultSet mockResultSet;

    private AppointmentDAO appointmentDAO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        appointmentDAO = new AppointmentDAO(mockConnection);
    }

    @Test
    void testAddAppointment_Success() throws Exception {
        Appointment appointment = new Appointment(101, "John Doe", "Male", "30", "2023-10-27", 
            "john@example.com", "1234567890", "Flu", 201, "123 Main St", "Pending");
        
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = appointmentDAO.addAppointment(appointment);
        assertTrue(result);
    }

    @Test
    void testAddAppointment_Failure() throws Exception {
        Appointment appointment = new Appointment(101, "John Doe", "Male", "30", "2023-10-27", 
            "john@example.com", "1234567890", "Flu", 201, "123 Main St", "Pending");
        
        when(mockConnection.prepareStatement(anyString())).thenThrow(new java.sql.SQLException());

        boolean result = appointmentDAO.addAppointment(appointment);
        assertFalse(result);
    }

    @Test
    void testGetAllAppointmentByLoginUser_Success() throws Exception {
        int userId = 101;
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt(1)).thenReturn(1);
        when(mockResultSet.getInt(2)).thenReturn(userId);
        when(mockResultSet.getString(3)).thenReturn("John Doe");

        List<Appointment> result = appointmentDAO.getAllAppointmentByLoginUser(userId);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getFullName());
    }

    @Test
    void testGetAppointmentById_Success() throws Exception {
        int apptId = 1;
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt(1)).thenReturn(apptId);

        Appointment result = appointmentDAO.getAppointmentById(apptId);
        assertNotNull(result);
        assertEquals(apptId, result.getId());
    }

    @Test
    void testUpdateDrAppointmentCommentStatus_Success() throws Exception {
        int apptId = 1;
        int docId = 201;
        String comment = "Approved";
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = appointmentDAO.updateDrAppointmentCommentStatus(apptId, docId, comment);
        assertTrue(result);
    }

    @Test
    void testGetAllAppointment_Success() throws Exception {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt(1)).thenReturn(1);

        List<Appointment> result = appointmentDAO.getAllAppointment();
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }
}
