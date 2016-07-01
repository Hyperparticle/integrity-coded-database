package convert;

import com.mysql.cj.jdbc.MysqlDataSource;
import main.args.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * </p>
 * Created on 6/3/2016
 *
 * @author Dan Kondratyuk
 */
public class DBConnection {

    private static final MysqlDataSource dataSource = new MysqlDataSource();
    private static final Logger logger = LogManager.getLogger();

//    // Keep a map of existing DB connections
//    private static final Map<String, DBConnection> dbConnections = new HashMap<>();

    /**
     * Configure a connection to a MySQL server
     */
    public static void configure(String serverName, int portNumber, String user, String password) {
        dataSource.setServerName(serverName);
        dataSource.setPortNumber(portNumber);
        dataSource.setUser(user);
        dataSource.setPassword(password);
    }

    public static DBConnection connect(String dbName, Config dbConfig) {
//        if (dbConnections.containsKey(dbName)) {
//            return dbConnections.get(dbName);
//        }

        try {
            DBConnection db = new DBConnection(dbName);
//            dbConnections.put(dbName, db);
            logger.info("Connected to DB {} at {}:{}", dbName, dbConfig.ip, dbConfig.port);
            return db;
        } catch (SQLException | DataAccessException e) {
            logger.error("Unable to connect to {}: {}", dbName, e.getMessage());
            logger.debug(e.getStackTrace());
            System.exit(1);
        }

        return null;
    }

    private final String dbName;
    private final Connection connection;
    private final DSLContext dbCreate;
    private final Schema dbSchema;
    private final List<String> tableNames;

    private DBConnection(String dbName) throws SQLException, DataAccessException {
        this.dbName = dbName;
        dataSource.setDatabaseName(dbName);

        connection = dataSource.getConnection();
        dbCreate = DSL.using(connection, SQLDialect.MYSQL);

        // Get schema metadata
        dbSchema = dbCreate.meta().getSchemas().stream()
                .filter(schema -> schema.getName().equals(dbName))
                .findFirst()
                .orElseThrow(() -> new SQLException("Unable to find schema with name " + dbName));

        // Get table metadata
        tableNames = dbCreate.fetch("show full tables where Table_type = 'BASE TABLE'")
                .map(result -> result.get(0).toString());
    }

    public Connection getConnection() {
        return connection;
    }

    public List<String> getTables() {
        return tableNames;
    }

    public Table<?> getTable(String name) {
        return dbSchema.getTable(name);
    }

    public DSLContext getCreate() {
        return dbCreate;
    }

}
