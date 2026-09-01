package com.hms.dao;

import com.hms.entity.Specialist;
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
class SpecialistDAOTest {

    @Mock
    private Connection mockConn;

    @Mock
    private PreparedStatement mockPstmt;

    @Mock
    private ResultSet mockResultSet;

    private SpecialistDAO specialistDAO;

    @BeforeEach
    void setUp() {
        specialistDAO = new SpecialistDAO(mockConn);
    }

    // ── Constructor test ───────────────────────────────────────────────────────

    @Test
    void constructor_withConnection_createsInstance() {
        SpecialistDAO dao = new SpecialistDAO(mockConn);
        assertNotNull(dao);
    }

    // ── addSpecialist tests ────────────────────────────────────────────────────

    @Test
    void addSpecialist_success_returnsTrue() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeUpdate()).thenReturn(1);

        boolean result = specialistDAO.addSpecialist("Cardiology");

        assertTrue(result);
        verify(mockPstmt).setString(1, "Cardiology");
        verify(mockPstmt).executeUpdate();
    }

    @Test
    void addSpecialist_sqlException_returnsFalse() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        boolean result = specialistDAO.addSpecialist("Cardiology");

        assertFalse(result);
    }

    @Test
    void addSpecialist_withEmptyString_callsExecuteUpdate() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeUpdate()).thenReturn(1);

        boolean result = specialistDAO.addSpecialist("");

        assertTrue(result);
    }

    @Test
    void addSpecialist_withNullValue_returnsFalse() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeUpdate()).thenThrow(new RuntimeException("null value"));

        boolean result = specialistDAO.addSpecialist(null);

        assertFalse(result);
    }

    // ── getAllSpecialist tests ─────────────────────────────────────────────────

    @Test
    void getAllSpecialist_withResults_returnsPopulatedList() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getInt(1)).thenReturn(1, 2);
        when(mockResultSet.getString(2)).thenReturn("Cardiology", "Neurology");

        List<Specialist> result = specialistDAO.getAllSpecialist();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Cardiology", result.get(0).getSpecialistName());
        assertEquals("Neurology", result.get(1).getSpecialistName());
        assertEquals(1, result.get(0).getId());
        assertEquals(2, result.get(1).getId());
    }

    @Test
    void getAllSpecialist_noResults_returnsEmptyList() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        List<Specialist> result = specialistDAO.getAllSpecialist();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllSpecialist_sqlException_returnsEmptyList() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenThrow(new RuntimeException("DB error"));

        List<Specialist> result = specialistDAO.getAllSpecialist();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllSpecialist_singleResult_returnsListWithOneElement() throws Exception {
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPstmt);
        when(mockPstmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt(1)).thenReturn(5);
        when(mockResultSet.getString(2)).thenReturn("Orthopedics");

        List<Specialist> result = specialistDAO.getAllSpecialist();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getId());
        assertEquals("Orthopedics", result.get(0).getSpecialistName());
    }
}
