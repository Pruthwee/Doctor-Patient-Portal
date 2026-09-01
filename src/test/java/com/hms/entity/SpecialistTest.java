package com.hms.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpecialistTest {

    private Specialist specialist;

    @BeforeEach
    void setUp() {
        specialist = new Specialist();
    }

    // ── Constructor tests ──────────────────────────────────────────────────────

    @Test
    void defaultConstructor_createsNonNullObject() {
        Specialist s = new Specialist();
        assertNotNull(s);
    }

    @Test
    void paramConstructor_setsIdAndName() {
        Specialist s = new Specialist(1, "Cardiology");
        assertEquals(1, s.getId());
        assertEquals("Cardiology", s.getSpecialistName());
    }

    @Test
    void paramConstructor_withZeroId_setsZeroId() {
        Specialist s = new Specialist(0, "General");
        assertEquals(0, s.getId());
        assertEquals("General", s.getSpecialistName());
    }

    @Test
    void paramConstructor_withNegativeId_setsNegativeId() {
        Specialist s = new Specialist(-1, "Neurology");
        assertEquals(-1, s.getId());
    }

    // ── Getter / Setter tests ──────────────────────────────────────────────────

    @Test
    void setAndGetId_returnsCorrectValue() {
        specialist.setId(5);
        assertEquals(5, specialist.getId());
    }

    @Test
    void setAndGetSpecialistName_returnsCorrectValue() {
        specialist.setSpecialistName("Orthopedics");
        assertEquals("Orthopedics", specialist.getSpecialistName());
    }

    @Test
    void setId_withLargeValue_returnsLargeValue() {
        specialist.setId(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, specialist.getId());
    }

    // ── Edge-case tests ────────────────────────────────────────────────────────

    @Test
    void setSpecialistName_withNull_storesNull() {
        specialist.setSpecialistName(null);
        assertNull(specialist.getSpecialistName());
    }

    @Test
    void setSpecialistName_withEmptyString_storesEmpty() {
        specialist.setSpecialistName("");
        assertEquals("", specialist.getSpecialistName());
    }

    @Test
    void defaultConstructor_idDefaultsToZero() {
        assertEquals(0, specialist.getId());
    }

    @Test
    void defaultConstructor_nameDefaultsToNull() {
        assertNull(specialist.getSpecialistName());
    }

    @Test
    void setSpecialistName_withLongName_storesCorrectly() {
        String longName = "Gastroenterology and Hepatology";
        specialist.setSpecialistName(longName);
        assertEquals(longName, specialist.getSpecialistName());
    }
}
