package com.hms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.hms.entity.Doctor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DoctorDAOTest {

    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement mockPreparedStatement;
    @Mock
    private ResultSet mockResultSet;

    private DoctorDAO doctorDAO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        doctorDAO = new DoctorDAO(mockConnection);
    }

    @Test
    void testRegisterDoctor_Success() throws Exception {
        Doctor doctor = new Doctor("Dr. Smith", "1980-01-01", "MD", "Cardiology", "smith@example.com", "1234567890", "pass123");
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = doctorDAO.registerDoctor(doctor);
        assertTrue(result);
    }

    @Test
    void testGetAllDoctor_Success() throws Exception {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("fullName")).thenReturn("Dr. Smith");
        when(mockResultSet.getString("dateOfBirth")).thenReturn("1980-01-01");
        when(mockResultSet.getString("qualification")).thenReturn("MD");
        when(mockResultSet.getString("specialist")).thenReturn("Cardiology");
        when(mockResultSet.getString("email")).thenReturn("smith@example.com");
        when(mockResultSet.getString("phone")).thenReturn("1234567890");
        when(mockResultSet.getString("password")).thenReturn("pass123");

        List<Doctor> result = doctorDAO.getAllDoctor();
        assertEquals(1, result.size());
        assertEquals("Dr. Smith", result.get(0).getFullName());
    }

    @Test
    void testGetDoctorById_Success() throws Exception {
        int id = 1;
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        Doctor result = doctorDAO.getDoctorById(id);
        assertNotNull(result);
        assertEquals(id, result.getId());
    }

    @Test
    void testUpdateDoctor_Success() throws Exception {
        Doctor doctor = new Doctor(1, "Dr. Smith", "1980-01-01", "MD", "Cardiology", "smith@example.com", "1234567890", "pass123");
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = doctorDAO.updateDoctor(doctor);
        assertTrue(result);
    }

    @Test
    void testDeleteDoctorById_Success() throws Exception {
        int id = 1;
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = doctorDAO.deleteDoctorById(id);
        assertTrue(result);
    }

    @Test
    void testLoginDoctor_Success() throws Exception {
        String email = "smith@example.com";
        String password = "pass123";
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt(1)).thenReturn(1);
        when(mockResultSet.getString(2)).thenReturn("Dr. Smith");
        when(mockResultSet.getString(3)).thenReturn("1980-01-01");
        when(mockResultSet.getString(4)).thenReturn("MD");
        when(mockResultSet.getString(5)).thenReturn("Cardiology");
        when(mockResultSet.getString(6)).thenReturn(email);
        when(mockResultSet.getString(7)).thenReturn("1234567890");
        when(mockResultSet.getString(8)).thenReturn(password);

        Doctor result = doctorDAO.loginDoctor(email, password);
        assertNotNull(result);
        assertEquals(email, result.getEmail());
    }

    @Test
    void testCountTotalDoctor_Success() throws Exception {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);

        int count = doctorDAO.countTotalDoctor();
        assertEquals(2, count);
    }

    @Test
    void testCheckOldPassword_Success() throws Exception {
        int doctorId = 1;
        String oldPassword = "oldPass";
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);

        boolean result = doctorDAO.checkOldPassword(doctorId, oldPassword);
        assertTrue(result);
    }

    @Test
    void testChangePassword_Success() throws Exception {
        int doctorId = 1;
        String newPassword = "newPass";
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = doctorDAO.changePassword(doctorId, newPassword);
        assertTrue(result);
    }

    @Test
    void testEditDoctorProfile_Success() throws Exception {
        Doctor doctor = new Doctor(1, "Dr. Smith", "1980-01-01", "MD", "Cardiology", "smith@example.com", "1234567890", "pass123");
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = doctorDAO.editDoctorProfile(doctor);
        assertTrue(result);
    }
}
