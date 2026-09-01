package com.hms.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    // ── Constructor tests ──────────────────────────────────────────────────────

    @Test
    void defaultConstructor_createsNonNullObject() {
        User u = new User();
        assertNotNull(u);
    }

    @Test
    void paramConstructorWithId_setsAllFields() {
        User u = new User(1, "John Doe", "john@example.com", "password123");
        assertEquals(1, u.getId());
        assertEquals("John Doe", u.getFullName());
        assertEquals("john@example.com", u.getEmail());
        assertEquals("password123", u.getPassword());
    }

    @Test
    void paramConstructorWithoutId_setsFieldsExceptId() {
        User u = new User("Jane Doe", "jane@example.com", "secret");
        assertEquals(0, u.getId()); // id not set → default 0
        assertEquals("Jane Doe", u.getFullName());
        assertEquals("jane@example.com", u.getEmail());
        assertEquals("secret", u.getPassword());
    }

    // ── Getter / Setter tests ──────────────────────────────────────────────────

    @Test
    void setAndGetId_returnsCorrectValue() {
        user.setId(42);
        assertEquals(42, user.getId());
    }

    @Test
    void setAndGetFullName_returnsCorrectValue() {
        user.setFullName("Alice");
        assertEquals("Alice", user.getFullName());
    }

    @Test
    void setAndGetEmail_returnsCorrectValue() {
        user.setEmail("alice@test.com");
        assertEquals("alice@test.com", user.getEmail());
    }

    @Test
    void setAndGetPassword_returnsCorrectValue() {
        user.setPassword("mypassword");
        assertEquals("mypassword", user.getPassword());
    }

    // ── toString tests ─────────────────────────────────────────────────────────

    @Test
    void toString_containsAllFields() {
        User u = new User(1, "Bob", "bob@test.com", "pass");
        String result = u.toString();
        assertTrue(result.contains("1"));
        assertTrue(result.contains("Bob"));
        assertTrue(result.contains("bob@test.com"));
        assertTrue(result.contains("pass"));
    }

    @Test
    void toString_returnsExpectedFormat() {
        User u = new User(5, "Carol", "carol@test.com", "abc");
        String expected = "User [id=5, fullName=Carol, email=carol@test.com, password=abc]";
        assertEquals(expected, u.toString());
    }

    // ── Edge-case tests ────────────────────────────────────────────────────────

    @Test
    void setFullName_withNull_storesNull() {
        user.setFullName(null);
        assertNull(user.getFullName());
    }

    @Test
    void setEmail_withEmptyString_storesEmpty() {
        user.setEmail("");
        assertEquals("", user.getEmail());
    }

    @Test
    void setId_withZero_returnsZero() {
        user.setId(0);
        assertEquals(0, user.getId());
    }

    @Test
    void setId_withNegativeValue_returnsNegative() {
        user.setId(-10);
        assertEquals(-10, user.getId());
    }

    @Test
    void defaultConstructor_idDefaultsToZero() {
        assertEquals(0, user.getId());
    }

    @Test
    void defaultConstructor_fieldsDefaultToNull() {
        assertNull(user.getFullName());
        assertNull(user.getEmail());
        assertNull(user.getPassword());
    }

    @Test
    void setPassword_withSpecialChars_storesCorrectly() {
        user.setPassword("P@$$w0rd#2024");
        assertEquals("P@$$w0rd#2024", user.getPassword());
    }
}
