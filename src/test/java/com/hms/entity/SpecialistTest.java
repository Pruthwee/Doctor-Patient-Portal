package com.hms.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpecialistTest {

    @Test
    void testDefaultConstructor() {
        Specialist specialist = new Specialist();
        assertNotNull(specialist);
    }

    @Test
    void testParameterizedConstructor() {
        Specialist specialist = new Specialist(1, "Cardiology");
        assertEquals(1, specialist.getId());
        assertEquals("Cardiology", specialist.getSpecialistName());
    }

    @Test
    void testSettersAndGetters() {
        Specialist specialist = new Specialist();
        specialist.setId(2);
        specialist.setSpecialistName("Neurology");
        
        assertEquals(2, specialist.getId());
        assertEquals("Neurology", specialist.getSpecialistName());
    }
}
