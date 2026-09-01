package com.hms.dao;

import com.hms.entity.Appointment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentDAOTest {

    @Mock
    private Connection mockConn;

    @Mock
    private PreparedStatement mockPstmt;

    @Mock
    private ResultSet mockResultSet;

    private AppointmentDAO appointmentDAO;

    @BeforeEach
    void setUp() {
        appointmentDAO = new AppointmentDAO(mockConn);
    }

    // ── Constructor test ───────────────────────────────────────────────────────

    @Test
    void constructor_withConnection_createsInstance() {
        AppointmentDAO dao = new AppointmentDAO(mockConn);
        assertNotNull(dao);
    }

    // ── addAppointment tests ───────────────────────────────────────────────────

    @Test
    void addAppointment_success_returnsTrue() throws Exception {
        Appointment appointment = new Appointment(1, "John", "Male", "30",
                "2024-01-15", "john@test.com", "1234567890", "Fever", 2, "123 St", "Pending");

        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeUpdate()).thenReturn(1);

        boolean result = appointmentDAO.addAppointment(appointment);

        assertTrue(result);
        verify(mockPstmt, times(1)).executeUpdate();
    }

    @Test
    void addAppointment_sqlException_returnsFalse() throws Exception {
        Appointment appointment = new Appointment(1, "John", "Male", "30",
                "2024-01-15", "john@test.com", "1234567890", "Fever", 2, "123 St", "Pending");

        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        boolean result = appointmentDAO.addAppointment(appointment);

        assertFalse(result);
    }

    // ── getAllAppointmentByLoginUser tests ─────────────────────────────────────

    @Test
    void getAllAppointmentByLoginUser_withResults_returnsPopulatedList() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt(1)).thenReturn(1);
        when(mockResultSet.getInt(2)).thenReturn(10);
        when(mockResultSet.getString(3)).thenReturn("John");
        when(mockResultSet.getString(4)).thenReturn("Male");
        when(mockResultSet.getString(5)).thenReturn("30");
        when(mockResultSet.getString(6)).thenReturn("2024-01-15");
        when(mockResultSet.getString(7)).thenReturn("john@test.com");
        when(mockResultSet.getString(8)).thenReturn("1234567890");
        when(mockResultSet.getString(9)).thenReturn("Fever");
        when(mockResultSet.getInt(10)).thenReturn(2);
        when(mockResultSet.getString(11)).thenReturn("123 St");
        when(mockResultSet.getString(12)).thenReturn("Pending");

        List<Appointment> result = appointmentDAO.getAllAppointmentByLoginUser(10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getFullName());
    }

    @Test
    void getAllAppointmentByLoginUser_noResults_returnsEmptyList() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        List<Appointment> result = appointmentDAO.getAllAppointmentByLoginUser(99);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllAppointmentByLoginUser_sqlException_returnsEmptyList() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        List<Appointment> result = appointmentDAO.getAllAppointmentByLoginUser(1);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ── getAllAppointmentByLoginDoctor tests ───────────────────────────────────

    @Test
    void getAllAppointmentByLoginDoctor_withResults_returnsPopulatedList() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt(1)).thenReturn(5);
        when(mockResultSet.getInt(2)).thenReturn(3);
        when(mockResultSet.getString(3)).thenReturn("Jane");
        when(mockResultSet.getString(4)).thenReturn("Female");
        when(mockResultSet.getString(5)).thenReturn("25");
        when(mockResultSet.getString(6)).thenReturn("2024-02-10");
        when(mockResultSet.getString(7)).thenReturn("jane@test.com");
        when(mockResultSet.getString(8)).thenReturn("9876543210");
        when(mockResultSet.getString(9)).thenReturn("Cold");
        when(mockResultSet.getInt(10)).thenReturn(7);
        when(mockResultSet.getString(11)).thenReturn("456 Ave");
        when(mockResultSet.getString(12)).thenReturn("Approved");

        List<Appointment> result = appointmentDAO.getAllAppointmentByLoginDoctor(7);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Jane", result.get(0).getFullName());
    }

    @Test
    void getAllAppointmentByLoginDoctor_noResults_returnsEmptyList() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        List<Appointment> result = appointmentDAO.getAllAppointmentByLoginDoctor(99);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ── getAppointmentById tests ───────────────────────────────────────────────

    @Test
    void getAppointmentById_found_returnsAppointment() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt(1)).thenReturn(1);
        when(mockResultSet.getInt(2)).thenReturn(10);
        when(mockResultSet.getString(3)).thenReturn("Bob");
        when(mockResultSet.getString(4)).thenReturn("Male");
        when(mockResultSet.getString(5)).thenReturn("40");
        when(mockResultSet.getString(6)).thenReturn("2024-03-01");
        when(mockResultSet.getString(7)).thenReturn("bob@test.com");
        when(mockResultSet.getString(8)).thenReturn("5551234567");
        when(mockResultSet.getString(9)).thenReturn("Headache");
        when(mockResultSet.getInt(10)).thenReturn(4);
        when(mockResultSet.getString(11)).thenReturn("789 Blvd");
        when(mockResultSet.getString(12)).thenReturn("Pending");

        Appointment result = appointmentDAO.getAppointmentById(1);

        assertNotNull(result);
        assertEquals("Bob", result.getFullName());
        assertEquals(1, result.getId());
    }

    @Test
    void getAppointmentById_notFound_returnsNull() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        Appointment result = appointmentDAO.getAppointmentById(999);

        assertNull(result);
    }

    @Test
    void getAppointmentById_sqlException_returnsNull() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        Appointment result = appointmentDAO.getAppointmentById(1);

        assertNull(result);
    }

    // ── updateDrAppointmentCommentStatus tests ─────────────────────────────────

    @Test
    void updateDrAppointmentCommentStatus_success_returnsTrue() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeUpdate()).thenReturn(1);

        boolean result = appointmentDAO.updateDrAppointmentCommentStatus(1, 2, "Reviewed");

        assertTrue(result);
        verify(mockPstmt, times(1)).executeUpdate();
    }

    @Test
    void updateDrAppointmentCommentStatus_sqlException_returnsFalse() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        boolean result = appointmentDAO.updateDrAppointmentCommentStatus(1, 2, "Reviewed");

        assertFalse(result);
    }

    // ── getAllAppointment tests ─────────────────────────────────────────────────

    @Test
    void getAllAppointment_withResults_returnsPopulatedList() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getInt(1)).thenReturn(1, 2);
        when(mockResultSet.getInt(2)).thenReturn(10, 11);
        when(mockResultSet.getString(3)).thenReturn("Alice", "Charlie");
        when(mockResultSet.getString(4)).thenReturn("Female", "Male");
        when(mockResultSet.getString(5)).thenReturn("22", "35");
        when(mockResultSet.getString(6)).thenReturn("2024-01-01", "2024-01-02");
        when(mockResultSet.getString(7)).thenReturn("a@t.com", "c@t.com");
        when(mockResultSet.getString(8)).thenReturn("111", "222");
        when(mockResultSet.getString(9)).thenReturn("Flu", "Cough");
        when(mockResultSet.getInt(10)).thenReturn(3, 4);
        when(mockResultSet.getString(11)).thenReturn("Addr1", "Addr2");
        when(mockResultSet.getString(12)).thenReturn("Pending", "Done");

        List<Appointment> result = appointmentDAO.getAllAppointment();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getAllAppointment_noResults_returnsEmptyList() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        List<Appointment> result = appointmentDAO.getAllAppointment();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllAppointment_sqlException_returnsEmptyList() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        List<Appointment> result = appointmentDAO.getAllAppointment();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
