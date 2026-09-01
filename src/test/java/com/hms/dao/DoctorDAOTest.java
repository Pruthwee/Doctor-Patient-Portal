package com.hms.dao;

import com.hms.entity.Doctor;
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
class DoctorDAOTest {

    @Mock
    private Connection mockConn;

    @Mock
    private PreparedStatement mockPstmt;

    @Mock
    private ResultSet mockResultSet;

    private DoctorDAO doctorDAO;

    @BeforeEach
    void setUp() {
        doctorDAO = new DoctorDAO(mockConn);
    }

    // ── Constructor test ───────────────────────────────────────────────────────

    @Test
    void constructor_withConnection_createsInstance() {
        DoctorDAO dao = new DoctorDAO(mockConn);
        assertNotNull(dao);
    }

    // ── registerDoctor tests ───────────────────────────────────────────────────

    @Test
    void registerDoctor_success_returnsTrue() throws Exception {
        Doctor doctor = new Doctor("Dr. Smith", "1980-01-01", "MBBS",
                "Cardiology", "smith@h.com", "1234567890", "pass");

        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeUpdate()).thenReturn(1);

        boolean result = doctorDAO.registerDoctor(doctor);

        assertTrue(result);
        verify(mockPstmt).executeUpdate();
    }

    @Test
    void registerDoctor_sqlException_returnsFalse() throws Exception {
        Doctor doctor = new Doctor("Dr. Smith", "1980-01-01", "MBBS",
                "Cardiology", "smith@h.com", "1234567890", "pass");

        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        boolean result = doctorDAO.registerDoctor(doctor);

        assertFalse(result);
    }

    // ── getAllDoctor tests ─────────────────────────────────────────────────────

    @Test
    void getAllDoctor_withResults_returnsPopulatedList() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("fullName")).thenReturn("Dr. Jones");
        when(mockResultSet.getString("dateOfBirth")).thenReturn("1975-05-10");
        when(mockResultSet.getString("qualification")).thenReturn("MD");
        when(mockResultSet.getString("specialist")).thenReturn("Neurology");
        when(mockResultSet.getString("email")).thenReturn("jones@h.com");
        when(mockResultSet.getString("phone")).thenReturn("9876543210");
        when(mockResultSet.getString("password")).thenReturn("secret");

        List<Doctor> result = doctorDAO.getAllDoctor();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Dr. Jones", result.get(0).getFullName());
    }

    @Test
    void getAllDoctor_noResults_returnsEmptyList() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        List<Doctor> result = doctorDAO.getAllDoctor();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllDoctor_sqlException_returnsEmptyList() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        List<Doctor> result = doctorDAO.getAllDoctor();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ── getDoctorById tests ────────────────────────────────────────────────────

    @Test
    void getDoctorById_found_returnsDoctor() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("id")).thenReturn(1);
        when(mockResultSet.getString("fullName")).thenReturn("Dr. Alice");
        when(mockResultSet.getString("dateOfBirth")).thenReturn("1985-03-15");
        when(mockResultSet.getString("qualification")).thenReturn("MBBS");
        when(mockResultSet.getString("specialist")).thenReturn("Dermatology");
        when(mockResultSet.getString("email")).thenReturn("alice@h.com");
        when(mockResultSet.getString("phone")).thenReturn("5551234567");
        when(mockResultSet.getString("password")).thenReturn("pwd");

        Doctor result = doctorDAO.getDoctorById(1);

        assertNotNull(result);
        assertEquals("Dr. Alice", result.getFullName());
        assertEquals(1, result.getId());
    }

    @Test
    void getDoctorById_notFound_returnsNull() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        Doctor result = doctorDAO.getDoctorById(999);

        assertNull(result);
    }

    @Test
    void getDoctorById_sqlException_returnsNull() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        Doctor result = doctorDAO.getDoctorById(1);

        assertNull(result);
    }

    // ── updateDoctor tests ─────────────────────────────────────────────────────

    @Test
    void updateDoctor_success_returnsTrue() throws Exception {
        Doctor doctor = new Doctor(1, "Dr. Updated", "1980-01-01", "MD",
                "Cardiology", "updated@h.com", "1111111111", "newpass");

        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeUpdate()).thenReturn(1);

        boolean result = doctorDAO.updateDoctor(doctor);

        assertTrue(result);
    }

    @Test
    void updateDoctor_sqlException_returnsFalse() throws Exception {
        Doctor doctor = new Doctor(1, "Dr. Updated", "1980-01-01", "MD",
                "Cardiology", "updated@h.com", "1111111111", "newpass");

        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        boolean result = doctorDAO.updateDoctor(doctor);

        assertFalse(result);
    }

    // ── deleteDoctorById tests ─────────────────────────────────────────────────

    @Test
    void deleteDoctorById_success_returnsTrue() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeUpdate()).thenReturn(1);

        boolean result = doctorDAO.deleteDoctorById(1);

        assertTrue(result);
        verify(mockPstmt).executeUpdate();
    }

    @Test
    void deleteDoctorById_sqlException_returnsFalse() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        boolean result = doctorDAO.deleteDoctorById(1);

        assertFalse(result);
    }

    // ── loginDoctor tests ──────────────────────────────────────────────────────

    @Test
    void loginDoctor_validCredentials_returnsDoctor() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt(1)).thenReturn(1);
        when(mockResultSet.getString(2)).thenReturn("Dr. Bob");
        when(mockResultSet.getString(3)).thenReturn("1970-07-07");
        when(mockResultSet.getString(4)).thenReturn("MBBS");
        when(mockResultSet.getString(5)).thenReturn("Surgery");
        when(mockResultSet.getString(6)).thenReturn("bob@h.com");
        when(mockResultSet.getString(7)).thenReturn("9990001111");
        when(mockResultSet.getString(8)).thenReturn("pass");

        Doctor result = doctorDAO.loginDoctor("bob@h.com", "pass");

        assertNotNull(result);
        assertEquals("Dr. Bob", result.getFullName());
    }

    @Test
    void loginDoctor_invalidCredentials_returnsNull() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        Doctor result = doctorDAO.loginDoctor("wrong@h.com", "wrongpass");

        assertNull(result);
    }

    @Test
    void loginDoctor_sqlException_returnsNull() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        Doctor result = doctorDAO.loginDoctor("email@h.com", "pass");

        assertNull(result);
    }

    // ── countTotalDoctor tests ─────────────────────────────────────────────────

    @Test
    void countTotalDoctor_withRows_returnsCount() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, true, false);

        int result = doctorDAO.countTotalDoctor();

        assertEquals(3, result);
    }

    @Test
    void countTotalDoctor_noRows_returnsZero() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        int result = doctorDAO.countTotalDoctor();

        assertEquals(0, result);
    }

    @Test
    void countTotalDoctor_sqlException_returnsZero() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        int result = doctorDAO.countTotalDoctor();

        assertEquals(0, result);
    }

    // ── countTotalAppointment tests ────────────────────────────────────────────

    @Test
    void countTotalAppointment_withRows_returnsCount() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);

        int result = doctorDAO.countTotalAppointment();

        assertEquals(2, result);
    }

    @Test
    void countTotalAppointment_noRows_returnsZero() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        int result = doctorDAO.countTotalAppointment();

        assertEquals(0, result);
    }

    // ── countTotalAppointmentByDoctorId tests ──────────────────────────────────

    @Test
    void countTotalAppointmentByDoctorId_withRows_returnsCount() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);

        int result = doctorDAO.countTotalAppointmentByDoctorId(5);

        assertEquals(1, result);
    }

    @Test
    void countTotalAppointmentByDoctorId_sqlException_returnsZero() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        int result = doctorDAO.countTotalAppointmentByDoctorId(5);

        assertEquals(0, result);
    }

    // ── countTotalUser tests ───────────────────────────────────────────────────

    @Test
    void countTotalUser_withRows_returnsCount() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, true, true, false);

        int result = doctorDAO.countTotalUser();

        assertEquals(4, result);
    }

    @Test
    void countTotalUser_noRows_returnsZero() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        int result = doctorDAO.countTotalUser();

        assertEquals(0, result);
    }

    // ── countTotalSpecialist tests ─────────────────────────────────────────────

    @Test
    void countTotalSpecialist_withRows_returnsCount() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);

        int result = doctorDAO.countTotalSpecialist();

        assertEquals(2, result);
    }

    @Test
    void countTotalSpecialist_sqlException_returnsZero() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        int result = doctorDAO.countTotalSpecialist();

        assertEquals(0, result);
    }

    // ── checkOldPassword tests ─────────────────────────────────────────────────

    @Test
    void checkOldPassword_passwordMatches_returnsTrue() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);

        boolean result = doctorDAO.checkOldPassword(1, "oldpass");

        assertTrue(result);
    }

    @Test
    void checkOldPassword_passwordNotMatch_returnsFalse() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        boolean result = doctorDAO.checkOldPassword(1, "wrongpass");

        assertFalse(result);
    }

    @Test
    void checkOldPassword_sqlException_returnsFalse() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        boolean result = doctorDAO.checkOldPassword(1, "pass");

        assertFalse(result);
    }

    // ── changePassword tests ───────────────────────────────────────────────────

    @Test
    void changePassword_success_returnsTrue() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeUpdate()).thenReturn(1);

        boolean result = doctorDAO.changePassword(1, "newpass");

        assertTrue(result);
    }

    @Test
    void changePassword_sqlException_returnsFalse() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        boolean result = doctorDAO.changePassword(1, "newpass");

        assertFalse(result);
    }

    // ── editDoctorProfile tests ────────────────────────────────────────────────

    @Test
    void editDoctorProfile_success_returnsTrue() throws Exception {
        Doctor doctor = new Doctor(1, "Dr. Updated", "1980-01-01", "MD",
                "Cardiology", "updated@h.com", "1111111111", "");

        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeUpdate()).thenReturn(1);

        boolean result = doctorDAO.editDoctorProfile(doctor);

        assertTrue(result);
    }

    @Test
    void editDoctorProfile_sqlException_returnsFalse() throws Exception {
        Doctor doctor = new Doctor(1, "Dr. Updated", "1980-01-01", "MD",
                "Cardiology", "updated@h.com", "1111111111", "");

        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        boolean result = doctorDAO.editDoctorProfile(doctor);

        assertFalse(result);
    }
}
