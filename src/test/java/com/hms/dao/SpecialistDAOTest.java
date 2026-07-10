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
    }

    @Test
    void testAddSpecialist_Failure() throws Exception {
        String specialistName = "Cardiology";
        when(mockConnection.prepareStatement(anyString())).thenThrow(new java.sql.SQLException());

        boolean result = specialistDAO.addSpecialist(specialistName);
        assertFalse(result);
    }

    @Test
    void testGetAllSpecialist_Success() throws Exception {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt(1)).thenReturn(1);
        when(mockResultSet.getString(2)).thenReturn("Cardiology");

        List<Specialist> result = specialistDAO.getAllSpecialist();
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Cardiology", result.get(0).getSpecialistName());
    }
}
