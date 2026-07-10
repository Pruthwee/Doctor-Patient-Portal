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
    void testFullConstructor() {
        Appointment appointment = new Appointment(1, 101, "John Doe", "Male", "30", "2023-10-27", 
            "john@example.com", "1234567890", "Flu", 201, "123 Main St", "Pending");
        
        assertEquals(1, appointment.getId());
        assertEquals(101, appointment.getUserId());
        assertEquals("John Doe", appointment.getFullName());
        assertEquals("Male", appointment.getGender());
        assertEquals("30", appointment.getAge());
        assertEquals("2023-10-27", appointment.getAppointmentDate());
        assertEquals("john@example.com", appointment.getEmail());
        assertEquals("1234567890", appointment.getPhone());
        assertEquals("Flu", appointment.getDiseases());
        assertEquals(201, appointment.getDoctorId());
        assertEquals("123 Main St", appointment.getAddress());
        assertEquals("Pending", appointment.getStatus());
    }

    @Test
    void testPartialConstructor() {
        Appointment appointment = new Appointment(101, "John Doe", "Male", "30", "2023-10-27", 
            "john@example.com", "1234567890", "Flu", 201, "123 Main St", "Pending");
        
        assertEquals(101, appointment.getUserId());
        assertEquals("John Doe", appointment.getFullName());
        assertEquals("Male", appointment.getGender());
        assertEquals("30", appointment.getAge());
        assertEquals("2023-10-27", appointment.getAppointmentDate());
        assertEquals("john@example.com", appointment.getEmail());
        assertEquals("1234567890", appointment.getPhone());
        assertEquals("Flu", appointment.getDiseases());
        assertEquals(201, appointment.getDoctorId());
        assertEquals("123 Main St", appointment.getAddress());
        assertEquals("Pending", appointment.getStatus());
    }

    @Test
    void testSettersAndGetters() {
        Appointment appointment = new Appointment();
        appointment.setId(1);
        appointment.setUserId(101);
        appointment.setFullName("John Doe");
        appointment.setGender("Male");
        appointment.setAge("30");
        appointment.setAppointmentDate("2023-10-27");
        appointment.setEmail("john@example.com");
        appointment.setPhone("1234567890");
        appointment.setDiseases("Flu");
        appointment.setDoctorId(201);
        appointment.setAddress("123 Main St");
        appointment.setStatus("Pending");

        assertEquals(1, appointment.getId());
        assertEquals(101, appointment.getUserId());
        assertEquals("John Doe", appointment.getFullName());
        assertEquals("Male", appointment.getGender());
        assertEquals("30", appointment.getAge());
        assertEquals("2023-10-27", appointment.getAppointmentDate());
        assertEquals("john@example.com", appointment.getEmail());
        assertEquals("1234567890", appointment.getPhone());
        assertEquals("Flu", appointment.getDiseases());
        assertEquals(201, appointment.getDoctorId());
        assertEquals("123 Main St", appointment.getAddress());
        assertEquals("Pending", appointment.getStatus());
    }
}
