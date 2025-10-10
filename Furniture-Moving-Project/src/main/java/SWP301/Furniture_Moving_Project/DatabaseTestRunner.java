package SWP301.Furniture_Moving_Project;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Standalone Database Connection Tester
 * Chạy class này trực tiếp để test kết nối database
 * Không cần Spring Boot context
 */
public class DatabaseTestRunner {
    
    // Database configuration
    private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=FurnitureTransportPlatform;encrypt=false";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "123";
    
    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🚀 FURNITURE TRANSPORT PLATFORM - DATABASE CONNECTION TEST");
        System.out.println("=".repeat(70));
        
        System.out.println("\n📋 Configuration:");
        System.out.println("   Server:   localhost:1433");
        System.out.println("   Database: FurnitureTransportPlatform");
        System.out.println("   Username: " + DB_USER);
        System.out.println("   Password: " + "*".repeat(DB_PASSWORD.length()));
        
        // Test 1: Load Driver
        System.out.println("\n" + "-".repeat(70));
        System.out.println("📦 Test 1: Loading SQL Server JDBC Driver...");
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            System.out.println("✅ PASSED: Driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ FAILED: Driver not found");
            System.err.println("💡 Solution: Add mssql-jdbc dependency to pom.xml");
            return;
        }
        
        // Test 2: Establish Connection
        System.out.println("\n" + "-".repeat(70));
        System.out.println("🔌 Test 2: Establishing database connection...");
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            
            System.out.println("✅ PASSED: Connection established successfully");
            
            // Test 3: Check Connection Validity
            System.out.println("\n" + "-".repeat(70));
            System.out.println("🔍 Test 3: Checking connection validity...");
            
            if (conn.isValid(2)) {
                System.out.println("✅ PASSED: Connection is valid");
            } else {
                System.err.println("❌ FAILED: Connection is not valid");
                return;
            }
            
            // Test 4: Get Database Metadata
            System.out.println("\n" + "-".repeat(70));
            System.out.println("📊 Test 4: Retrieving database metadata...");
            
            System.out.println("✅ Database Product: " + conn.getMetaData().getDatabaseProductName());
            System.out.println("✅ Database Version: " + conn.getMetaData().getDatabaseProductVersion());
            System.out.println("✅ Driver Name:      " + conn.getMetaData().getDriverName());
            System.out.println("✅ Driver Version:   " + conn.getMetaData().getDriverVersion());
            System.out.println("✅ JDBC URL:         " + conn.getMetaData().getURL());
            
            // Test 5: Execute Simple Query
            System.out.println("\n" + "-".repeat(70));
            System.out.println("🔧 Test 5: Executing test query...");
            
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT DB_NAME() AS DatabaseName, @@VERSION AS SQLVersion");
                if (rs.next()) {
                    System.out.println("✅ PASSED: Query executed successfully");
                    System.out.println("   Connected to database: " + rs.getString("DatabaseName"));
                }
            }
            
            // Test 6: Check Required Tables
            System.out.println("\n" + "-".repeat(70));
            System.out.println("📋 Test 6: Checking required tables...");
            
            String[] requiredTables = {
                "users",
                "customers",
                "service_requests",
                "request_addresses",
                "furniture_items"
            };
            
            try (Statement stmt = conn.createStatement()) {
                for (String table : requiredTables) {
                    String query = "SELECT COUNT(*) AS cnt FROM INFORMATION_SCHEMA.TABLES " +
                                 "WHERE TABLE_NAME = '" + table + "'";
                    ResultSet rs = stmt.executeQuery(query);
                    if (rs.next() && rs.getInt("cnt") > 0) {
                        System.out.println("   ✅ Table '" + table + "' exists");
                    } else {
                        System.out.println("   ⚠️  Table '" + table + "' NOT FOUND");
                    }
                }
            }
            
            // Test 7: Count Records
            System.out.println("\n" + "-".repeat(70));
            System.out.println("📊 Test 7: Counting records in tables...");
            
            try (Statement stmt = conn.createStatement()) {
                
                // Check service_requests
                try {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM service_requests");
                    if (rs.next()) {
                        System.out.println("   ✅ service_requests:  " + rs.getInt("cnt") + " records");
                    }
                } catch (Exception e) {
                    System.out.println("   ⚠️  service_requests: Table not accessible");
                }
                
                // Check request_addresses
                try {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM request_addresses");
                    if (rs.next()) {
                        System.out.println("   ✅ request_addresses: " + rs.getInt("cnt") + " records");
                    }
                } catch (Exception e) {
                    System.out.println("   ⚠️  request_addresses: Table not accessible");
                }
                
                // Check furniture_items
                try {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM furniture_items");
                    if (rs.next()) {
                        System.out.println("   ✅ furniture_items:   " + rs.getInt("cnt") + " records");
                    }
                } catch (Exception e) {
                    System.out.println("   ⚠️  furniture_items: Table not accessible");
                }
            }
            
            // Final Summary
            System.out.println("\n" + "=".repeat(70));
            System.out.println("🎉 ALL TESTS COMPLETED SUCCESSFULLY!");
            System.out.println("=".repeat(70));
            System.out.println("\n✨ Your database is ready for use!");
            System.out.println("💡 You can now run the Spring Boot application\n");
            
        } catch (Exception e) {
            System.err.println("\n❌ CONNECTION FAILED!");
            System.err.println("Error Type: " + e.getClass().getSimpleName());
            System.err.println("Error Message: " + e.getMessage());
            
            System.err.println("\n" + "=".repeat(70));
            System.err.println("💡 TROUBLESHOOTING GUIDE");
            System.err.println("=".repeat(70));
            System.err.println("\n1. Check if SQL Server is running:");
            System.err.println("   - Open Services (services.msc)");
            System.err.println("   - Look for 'SQL Server (MSSQLSERVER)'");
            System.err.println("   - Make sure it's running");
            
            System.err.println("\n2. Verify database exists:");
            System.err.println("   - Open SQL Server Management Studio (SSMS)");
            System.err.println("   - Connect to localhost");
            System.err.println("   - Check if 'FurnitureTransportPlatform' database exists");
            
            System.err.println("\n3. Check authentication:");
            System.err.println("   - SQL Server must allow SQL Server Authentication");
            System.err.println("   - Username: sa");
            System.err.println("   - Password: 1234");
            
            System.err.println("\n4. Check firewall:");
            System.err.println("   - Allow port 1433 in Windows Firewall");
            
            System.err.println("\n5. Enable TCP/IP:");
            System.err.println("   - Open SQL Server Configuration Manager");
            System.err.println("   - Protocols for MSSQLSERVER > TCP/IP > Enable");
            
            System.err.println("\n" + "=".repeat(70) + "\n");
            
            e.printStackTrace();
        }
    }
}