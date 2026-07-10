package com.hms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.hms.entity.Appointment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
                                                 "john@example.com", "1234567890", "Flu", 501, "123 Main St", "Pending");
        
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = appointmentDAO.addAppointment(appointment);
        assertTrue(result);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testAddAppointment_Failure() throws Exception {
        Appointment appointment = new Appointment(101, "John Doe", "Male", "30", "2023-10-27", 
                                                 "john@example.com", "1234567890", "Flu", 501, "123 Main St", "Pending");
        
        when(mockConnection.prepareStatement(anyString())).thenThrow(new Exception("DB Error"));

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
        when(mockResultSet.getString(4)).thenReturn("Male");
        when(mockResultSet.getString(5)).thenReturn("30");
        when(mockResultSet.getString(6)).thenReturn("2023-10-27");
        when(mockResultSet.getString(7)).thenReturn("john@example.com");
        when(mockResultSet.getString(8)).thenReturn("1234567890");
        when(mockResultSet.getString(9)).thenReturn("Flu");
        when(mockResultSet.getInt(10)).thenReturn(501);
        when(mockResultSet.getString(11)).thenReturn("123 Main St");
        when(mockResultSet.getString(12)).thenReturn("Pending");

        List<Appointment> result = appointmentDAO.getAllAppointmentByLoginUser(userId);
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
        when(mockResultSet.getInt(2)).thenReturn(101);
        when(mockResultSet.getString(3)).thenReturn("John Doe");
        when(mockResultSet.getString(4)).thenReturn("Male");
        when(mockResultSet.getString(5)).thenReturn("30");
        when(mockResultSet.getString(6)).thenReturn("2023-10-27");
        when(mockResultSet.getString(7)).thenReturn("john@example.com");
        when(mockResultSet.getString(8)).thenReturn("1234567890");
        when(mockResultSet.getString(9)).thenReturn("Flu");
        when(mockResultSet.getInt(10)).thenReturn(501);
        when(mockResultSet.getString(11)).thenReturn("123 Main St");
        when(mockResultSet.getString(12)).thenReturn("Pending");

        Appointment result = appointmentDAO.getAppointmentById(apptId);
        assertNotNull(result);
        assertEquals(apptId, result.getId());
    }

    @Test
    void testUpdateDrAppointmentCommentStatus_Success() throws Exception {
        int apptId = 1;
        int docId = 501;
        String comment = "Completed";
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
        when(mockResultSet.getInt(2)).thenReturn(101);
        when(mockResultSet.getString(3)).thenReturn("John Doe");
        when(mockResultSet.getString(4)).thenReturn("Male");
        when(mockResultSet.getString(5)).thenReturn("30");
        when(mockResultSet.getString(6)).thenReturn("2023-10-27");
        when(mockResultSet.getString(7)).thenReturn("john@example.com");
        when(mockResultSet.getString(8)).thenReturn("1234567890");
        when(mockResultSet.getString(9)).thenReturn("Flu");
        when(mockResultSet.getInt(10)).thenReturn(501);
        when(mockResultSet.getString(11)).thenReturn("123 Main St");
        when(mockResultSet.getString(12)).thenReturn("Pending");

        List<Appointment> result = appointmentDAO.getAllAppointment();
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getFullName());
    }
}
