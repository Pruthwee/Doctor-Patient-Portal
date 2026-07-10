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

import com.hms.entity.Specialist;

class SpecialistDAOTest {

    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement mockPreparedStatement;
    @Mock
    private ResultSet mockResultSet;

    private SpecialistDAO specialistDAO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        specialistDAO = new SpecialistDAO(mockConnection);
    }

    @Test
    void testAddSpecialist_Success() throws Exception {
        String specialistName = "Cardiology";
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = specialistDAO.addSpecialist(specialistName);
        assertTrue(result);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testAddSpecialist_Failure() throws Exception {
        // Use a specific checked exception that PreparedStatement.prepareStatement might throw
        when(mockConnection.prepareStatement(anyString())).thenThrow(new java.sql.SQLException("DB Error"));
        boolean result = specialistDAO.addSpecialist("Neurology");
        assertFalse(result);
    }

    @Test
    void testGetAllSpecialist_Success() throws Exception {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getInt(1)).thenReturn(1, 2);
        when(mockResultSet.getString(2)).thenReturn("Cardiology", "Neurology");

        List<Specialist> result = specialistDAO.getAllSpecialist();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Cardiology", result.get(0).getSpecialistName());
        assertEquals("Neurology", result.get(1).getSpecialistName());
    }
}
