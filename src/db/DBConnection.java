package db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Loads connection settings from db.properties (classpath root) and
 * hands out plain JDBC connections to SQL Server. One connection per
 * call - callers are responsible for closing it (try-with-resources).
 */
public final class DBConnection {

    private static final String CONFIG_FILE = "/db.properties";
    private static final String url;
    private static final String user;
    private static final String password;
    private static final String uploadDir;

    static {
        // Explicitly load the driver through this webapp's own classloader.
        // In a servlet container, DriverManager's automatic
        // META-INF/services/java.sql.Driver discovery often runs against a
        // different (container-level) classloader and never sees a driver
        // jar that only lives in this webapp's WEB-INF/lib, leading to
        // "No suitable driver found" even though the jar is right there.
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQL Server JDBC driver not found on classpath", e);
        }

        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new RuntimeException("db.properties not found on classpath: " + CONFIG_FILE);
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load database configuration", e);
        }
        url = props.getProperty("db.url");
        user = props.getProperty("db.user");
        password = props.getProperty("db.password");
        // Directory uploaded documents (e.g. a member's photo) are written to - deliberately
        // outside the deployed webapp's own directory, since that gets wiped on every redeploy.
        uploadDir = props.getProperty("upload.dir");
    }

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public static String getUploadDir() {
        return uploadDir;
    }
}
