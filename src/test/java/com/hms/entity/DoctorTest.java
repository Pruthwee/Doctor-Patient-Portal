package com.hms.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoctorTest {

    @Test
    void testDefaultConstructor() {
        Doctor doctor = new Doctor();
        assertNotNull(doctor);
    }

    @Test
    void testFullConstructor() {
        Doctor doctor = new Doctor(1, "Dr. Smith", "1980-01-01", "MBBS, MD", "Cardiology", "smith@example.com", "1234567890", "pass123");
        assertEquals(1, doctor.getId());
        assertEquals("Dr. Smith", doctor.getFullName());
        assertEquals("1980-01-01", doctor.getDateOfBirth());
        assertEquals("MBBS, MD", doctor.getQualification());
        assertEquals("Cardiology", doctor.getSpecialist());
        assertEquals("smith@example.com", doctor.getEmail());
        assertEquals("1234567890", doctor.getPhone());
        assertEquals("pass123", doctor.getPassword());
    }

    @Test
    void testPartialConstructor() {
        Doctor doctor = new Doctor("Dr. Smith", "1980-01-01", "MBBS, MD", "Cardiology", "smith@example.com", "1234567890", "pass123");
        assertEquals("Dr. Smith", doctor.getFullName());
        assertEquals("1980-01-01", doctor.getDateOfBirth());
        assertEquals("MBBS, MD", doctor.getQualification());
        assertEquals("Cardiology", doctor.getSpecialist());
        assertEquals("smith@example.com", doctor.getEmail());
        assertEquals("1234567890", doctor.getPhone());
        assertEquals("pass123", doctor.getPassword());
    }

    @Test
    void testSettersAndGetters() {
        Doctor doctor = new Doctor();
        doctor.setId(1);
        doctor.setFullName("Dr. Smith");
        doctor.setDateOfBirth("1980-01-01");
        doctor.setQualification("MBBS, MD");
        doctor.setSpecialist("Cardiology");
        doctor.setEmail("smith@example.com");
        doctor.setPhone("1234567890");
        doctor.setPassword("pass123");

        assertEquals(1, doctor.getId());
        assertEquals("Dr. Smith", doctor.getFullName());
        assertEquals("1980-01-01", doctor.getDateOfBirth());
        assertEquals("MBBS, MD", doctor.getQualification());
        assertEquals("Cardiology", doctor.getSpecialist());
        assertEquals("smith@example.com", doctor.getEmail());
        assertEquals("1234567890", doctor.getPhone());
        assertEquals("pass123", doctor.getPassword());
    }
}
