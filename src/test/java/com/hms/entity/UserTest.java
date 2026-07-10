package com.hms.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testDefaultConstructor() {
        User user = new User();
        assertNotNull(user);
    }

    @Test
    void testFullParameterizedConstructor() {
        User user = new User(1, "John Doe", "john@example.com", "pass123");
        assertEquals(1, user.getId());
        assertEquals("John Doe", user.getFullName());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("pass123", user.getPassword());
    }

    @Test
    void testPartialParameterizedConstructor() {
        User user = new User("Jane Doe", "jane@example.com", "pass456");
        assertEquals("Jane Doe", user.getFullName());
        assertEquals("jane@example.com", user.getEmail());
        assertEquals("pass456", user.getPassword());
    }

    @Test
    void testGettersAndSetters() {
        User user = new User();
        user.setId(10);
        user.setFullName("Alice");
        user.setEmail("alice@example.com");
        user.setPassword("secret");
        
        assertEquals(10, user.getId());
        assertEquals("Alice", user.getFullName());
        assertEquals("alice@example.com", user.getEmail());
        assertEquals("secret", user.getPassword());
    }

    @Test
    void testToString() {
        User user = new User(1, "John", "john@email.com", "pw");
        String expected = "User [id=1, fullName=John, email=john@email.com, password=pw]";
        assertEquals(expected, user.toString());
    }
}
