package com.hms.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppointmentTest {

    @Test
    void testDefaultConstructor() {
        Appointment appointment = new Appointment();
        assertNotNull(appointment);
    }

    @Test
    void testFullParameterizedConstructor() {
        Appointment appointment = new Appointment(1, 101, "John Doe", "Male", "30", "2023-10-10", 
            "john@example.com", "1234567890", "Fever", 201, "New York", "Pending");
        
        assertEquals(1, appointment.getId());
        assertEquals(101, appointment.getUserId());
        assertEquals("John Doe", appointment.getFullName());
        assertEquals("Male", appointment.getGender());
        assertEquals("30", appointment.getAge());
        assertEquals("2023-10-10", appointment.getAppointmentDate());
        assertEquals("john@example.com", appointment.getEmail());
        assertEquals("1234567890", appointment.getPhone());
        assertEquals("Fever", appointment.getDiseases());
        assertEquals(201, appointment.getDoctorId());
        assertEquals("New York", appointment.getAddress());
        assertEquals("Pending", appointment.getStatus());
    }

    @Test
    void testPartialParameterizedConstructor() {
        Appointment appointment = new Appointment(101, "John Doe", "Male", "30", "2023-10-10", 
            "john@example.com", "1234567890", "Fever", 201, "New York", "Pending");
        
        assertEquals(101, appointment.getUserId());
        assertEquals("John Doe", appointment.getFullName());
        assertEquals("Pending", appointment.getStatus());
        assertEquals(0, appointment.getId());
    }

    @Test
    void testGettersAndSetters() {
        Appointment appointment = new Appointment();
        appointment.setId(1);
        appointment.setUserId(101);
        appointment.setFullName("John Doe");
        appointment.setGender("Male");
        appointment.setAge("30");
        appointment.setAppointmentDate("2023-10-10");
        appointment.setEmail("john@example.com");
        appointment.setPhone("1234567890");
        appointment.setDiseases("Fever");
        appointment.setDoctorId(201);
        appointment.setAddress("New York");
        appointment.setStatus("Pending");

        assertEquals(1, appointment.getId());
        assertEquals(101, appointment.getUserId());
        assertEquals("John Doe", appointment.getFullName());
        assertEquals("Male", appointment.getGender());
        assertEquals("30", appointment.getAge());
        assertEquals("2023-10-10", appointment.getAppointmentDate());
        assertEquals("john@example.com", appointment.getEmail());
        assertEquals("1234567890", appointment.getPhone());
        assertEquals("Fever", appointment.getDiseases());
        assertEquals(201, appointment.getDoctorId());
        assertEquals("New York", appointment.getAddress());
        assertEquals("Pending", appointment.getStatus());
    }
}
