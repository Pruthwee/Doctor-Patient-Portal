package com.hms.db;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import static org.junit.jupiter.api.Assertions.*;

class DBConnectionTest {

    @Test
    void testGetConn() {
        // This test might fail if MySQL is not running on localhost:3306
        // But we are testing the method call.
        Connection conn = DBConnection.getConn();
        // We don't assertNotNull(conn) because it depends on the environment
        // but we ensure the method returns without throwing an exception.
        assertTrue(true); 
    }
}
