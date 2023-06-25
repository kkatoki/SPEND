
package com.ssg.database;

import com.ssg.utils.ProgramUtils;
import com.ssg.utils.RuntimeData;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.*;


public class SpendBConnection {

    private static final String DATABASE_NAME = "spendB";
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB_CONNECTION = "jdbc:mysql://localhost:3306/";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    private static Connection connection;
    public static final Map<String, List<String>> TABLECOLUMNS = new LinkedHashMap<>();

    // Editable
    static {
        /*
         * Adding Columns in a Table:
         *    Edit this HashMap
         *    Edit the Reading
         *    Edit the Updating
         *    Edit the Creating
         *    Generate Random Data
         *    Model Data
         *    Model Values
         * In creating data, columns ending with _ID, _CD and is a TIMESTAMP are skipped
         */
        TABLECOLUMNS.put("USERS", Arrays.asList(
                "USER_ID/INT",
                "FIRSTNAME/VARCHAR(255)",
                "MIDDLEINITIAL/VARCHAR(255)",
                "LASTNAME/VARCHAR(255)",
                "USERNAME/VARCHAR(255)",
                "PASSWORD/VARCHAR(255)",
                "ISADMIN/BIT",
                "USER_CD/DATE",
                "UPDATETIME/TIMESTAMP"
        ));
        TABLECOLUMNS.put("OFFICERS", Arrays.asList(
                "OFFICER_ID/INT",
                "FIRSTNAME/VARCHAR(255)",
                "MIDDLEINITIAL/VARCHAR(255)",
                "LASTNAME/VARCHAR(255)",
                "DESCRIPTION/VARCHAR(400)",
                "POSITION/VARCHAR(255)",
                "STRAND/VARCHAR(255)",
                "USER_ID/INT/USERS",
                "OFFICER_CD/DATE",
                "TERM/INT",
                "UPDATETIME/TIMESTAMP",
                "AVATAR/LONGBLOB"
        ));
        TABLECOLUMNS.put("PROJECTS", Arrays.asList(
                "PROJECT_ID/INT",
                "TITLE/VARCHAR(255)",
                "DESCRIPTION/VARCHAR(400)",
                "USER_ID/INT/USERS",
                "PROJECT_CD/DATE",
                "EVENTDATE/DATE",
                "UPDATETIME/TIMESTAMP"
        ));
        TABLECOLUMNS.put("EXPENSES", Arrays.asList(
                "EXPENSE_ID/INT",
                "PROJECT_ID/INT/PROJECTS",
                "ITEMNAME/VARCHAR(255)",
                "TOTALPRICE/DOUBLE",
                "EXPENSEDATE_CD/DATE",
                "QUANTITY/DOUBLE",
                "UNITPRICE/DOUBLE",
                "STATUS/INT",
                "UPDATETIME/TIMESTAMP"
        ));
        TABLECOLUMNS.put("CONTRIBUTORS", Arrays.asList(
                "PROJECT_ID/INT/PROJECTS",
                "OFFICER_ID/INT/OFFICERS",
                "UPDATETIME/TIMESTAMP"
        ));
        TABLECOLUMNS.put("FUNDS", Arrays.asList(
                "FUND_ID/INT",
                "AMOUNT/DOUBLE",
                "DESCRIPTION/VARCHAR(400)",
                "UPDATETIME/TIMESTAMP",
                "FUND_CD/DATE"
        ));
        TABLECOLUMNS.put("SCHOOLDATA", Arrays.asList(
                "DATA_ID/INT",
                "UPDATETIME/TIMESTAMP",
                "SCHOOLYEAR/INT",
                "SCHOOLLOGO/LONGBLOB",
                "SSGLOGO/LONGBLOB",
                "REPORTEXPORTLOCATION/VARCHAR(255)",
                "MANAGEXAMPP/BIT",
                "XAMPPLOCATION/VARCHAR(255)",
                "VIEWPDF/BIT",
                "CURRENTSCHOOLYEAR/BIT",
                "SSGADVISER/VARCHAR(255)",
                "PRINCIPAL/VARCHAR(255)",
                "PROPOSALPARAGRAPH/VARCHAR(500)"
        ));
    }

    public static void main(String[] args) {
        System.out.println(getColumnNames("USERS"));
    }

    public static void intializeConnection() {
        try {
            boolean newDatabase = setupDatabase();
            connection = SpendBConnection.getDBConnection();
            SpendBUtils.spendBPacket(5048576);
            if (!newDatabase) return;
            setupTable();
            if (RuntimeData.FILLDATA) SpendBPrefill.generate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private static boolean setupDatabase() {
        try  (Connection tempConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD)){
            if (!RuntimeData.CREATEDATABASE) return false;

            System.out.println("Setting up the database");
            // Drop the old database
            String sql = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + DATABASE_NAME + "'";
            ResultSet rs= tempConnection.createStatement().executeQuery(sql);
            if (rs.next()) {
                String dbName = rs.getString("SCHEMA_NAME");
                String dropSql = "DROP DATABASE " + dbName;
                tempConnection.createStatement().executeUpdate(dropSql);
                System.out.println("Dropped database: " + dbName);
            }

            // Create the Database
            System.out.println("Creating " + DATABASE_NAME + " database");
            String query = "CREATE DATABASE " + DATABASE_NAME;
            tempConnection.createStatement().executeUpdate(query);
            System.out.println("Database created successfully.");
            return true;
        } catch (SQLException e) {
            ProgramUtils.showDialogMessage("MYSQL Connection Error", "The MySQL connection encountered an error while initializing.\nPlease ensure that the MySQL server is running and accessible.\nVerify that the server is up and running properly to establish\na successful connection.");
            throw new RuntimeException();
        }
    }
    private static void setupTable() throws SQLException {
        Statement stmt = getConnection().createStatement();

        for (String tableName : TABLECOLUMNS.keySet()) {
            List<String> columns = TABLECOLUMNS.get(tableName);

            System.out.println("Setting up " + tableName);

            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
            int i = 0;
            for (String column : columns) {
                String[] tokens = column.split("/");
                String columnName = tokens[0];
                String columnType = tokens[1];

                boolean isReferenced = tokens.length == 3;
                boolean isPrimary = columnName.endsWith("_ID") && !isReferenced;
                boolean isTimestamp = Objects.equals(columnType, "TIMESTAMP");
                boolean isDate = Objects.equals(columnType, "DATE");

                sb.append(columnName).append(" ").append(columnType);

                if (isPrimary) sb.append(" AUTO_INCREMENT PRIMARY KEY");
                if (isTimestamp || isDate) sb.append(" DEFAULT CURRENT_TIMESTAMP");
                if (isReferenced) sb.append(", FOREIGN KEY (").append(columnName).append(") REFERENCES ").append(tokens[2]).append("(").append(columnName).append(") ON DELETE CASCADE");

                if (i < columns.size() - 1) sb.append(", ");
                i++;
            }
            sb.append(");");
            String sql = sb.toString();
            stmt.executeUpdate(sql);
        }
        stmt.close();
    }
    private static Connection getDBConnection() {
        try {
            Class.forName(DB_DRIVER); // Load a JDBC driver dynamically at runtime.
            return DriverManager.getConnection(DB_CONNECTION + DATABASE_NAME, DB_USER, DB_PASSWORD);
        } catch (Exception e) {
            ProgramUtils.showDialogMessage("MYSQL Connection Error", "The MySQL connection encountered an error while initializing.\nPlease ensure that the MySQL server is running and accessible.\nVerify that the server is up and running properly to establish\na successful connection.");
            e.printStackTrace();
            return null;
        }
    }
    /**
     * Gets the columns of a table excluding
     * Columns ending with "_ID" (AND not referenced) or "CD", Columns with TIMESTAMP type
     * @param tableName The name of the table
     * @return A LinkedHashMap containing the name as key and type as the value of the column
     */
    public static LinkedHashMap<String, String> getColumnNames(String tableName) {
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        for (String column: TABLECOLUMNS.get(tableName)) {
            String[] tokens = column.split("/");
            String columnName = tokens[0];
            String columnType = tokens[1];
            boolean isReferenced = tokens.length == 3;
            boolean isInserted = !columnName.endsWith("_ID") || isReferenced;
            boolean isTimestamp = Objects.equals(columnType, "TIMESTAMP");
            boolean isCreationDate = columnName.endsWith("_CD");
            if (!isInserted || isTimestamp || isCreationDate) continue;
            columns.put(columnName, columnType);
        }
        return columns;
    }
    public static Connection getConnection() {
        assert connection != null;
        return connection;
    }

}
