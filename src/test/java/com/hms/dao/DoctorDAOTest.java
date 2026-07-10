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

import com.hms.entity.Doctor;

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
        Doctor doctor = new Doctor("Dr. Smith", "1980-01-01", "MBBS", "Cardiology", "smith@email.com", "123456", "pass");
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

        List<Doctor> result = doctorDAO.getAllDoctor();
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Dr. Smith", result.get(0).getFullName());
    }

    @Test
    void testGetDoctorById_Success() throws Exception {
        int id = 1;
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(id);

        Doctor result = doctorDAO.getDoctorById(id);
        assertNotNull(result);
        assertEquals(id, result.getId());
    }

    @Test
    void testUpdateDoctor_Success() throws Exception {
        Doctor doctor = new Doctor(1, "Dr. Smith", "1980-01-01", "MBBS", "Cardiology", "smith@email.com", "123456", "pass");
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
        String email = "smith@email.com";
        String password = "pass";
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt(1)).thenReturn(1);
        when(mockResultSet.getString(2)).thenReturn("Dr. Smith");

        Doctor result = doctorDAO.loginDoctor(email, password);
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Dr. Smith", result.getFullName());
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
        int id = 1;
        String oldPass = "oldpass";
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);

        boolean result = doctorDAO.checkOldPassword(id, oldPass);
        assertTrue(result);
    }

    @Test
    void testChangePassword_Success() throws Exception {
        int id = 1;
        String newPass = "newpass";
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = doctorDAO.changePassword(id, newPass);
        assertTrue(result);
    }

    @Test
    void testEditDoctorProfile_Success() throws Exception {
        Doctor doctor = new Doctor(1, "Dr. Smith", "1980-01-01", "MBBS", "Cardiology", "smith@email.com", "123456", "pass");
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = doctorDAO.editDoctorProfile(doctor);
        assertTrue(result);
    }
}
