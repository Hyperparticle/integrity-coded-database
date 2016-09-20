package io;

import com.mysql.cj.jdbc.MysqlDataSource;
import main.args.config.UserConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import javax.naming.Reference;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * </p>
 * Created on 6/3/2016
 *
 * @author Dan Kondratyuk
 */
public class DBConnection {

//    private static final MysqlDataSource dataSource = new MysqlDataSource();
    private static final Logger logger = LogManager.getLogger();
    private static UserConfig userConfig;

    /**
     * Configure a connection to a MySQL server
     */
    public static void configure(UserConfig dbConfig) {
        userConfig = dbConfig;
//        dataSource.setServerName(dbConfig.ip);
//        dataSource.setPortNumber(dbConfig.port);
//        dataSource.setUser(dbConfig.user);
//        dataSource.setPassword(dbConfig.password);
    }

    public static DBConnection connect(String dbName, UserConfig dbConfig) {
        try {
            DBConnection db = new DBConnection(dbName);
            logger.trace("Connected to DB {} at {}:{}", dbName, dbConfig.ip, dbConfig.port);
            return db;
        } catch (SQLException | DataAccessException e) {
            logger.error("Unable to connect to {}: {}", dbName, e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    private final String dbName;
    private final Connection connection;
    private final DSLContext dbCreate;
    private final Schema dbSchema;
    private final List<String> tableNames;
    private final Map<String, List<String>> fieldMap;
    private final Map<String, List<String>> primaryKeyMap;

    private DBConnection(String dbName) throws SQLException, DataAccessException {
        this.dbName = dbName;
//        dataSource.setDatabaseName(dbName);

        final String url = "jdbc:mysql://" + userConfig.ip + ":" + userConfig.port + "/" + dbName + "?" +
                "user=" + userConfig.user + "&password=" + userConfig.password +
                "&maxAllowedPacket=1000000000&autoReconnect=true&useSSL=false" +
                "&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=America/Denver";

//        connection = dataSource.getConnection();
        connection = DriverManager.getConnection(url);

        dbCreate = DSL.using(connection, SQLDialect.MYSQL);

        // Get schema metadata
        dbSchema = dbCreate.meta().getSchemas().stream()
                .filter(schema -> schema.getName().equals(dbName))
                .findFirst()
                .orElseThrow(() -> new SQLException("Unable to find schema with name " + dbName));

        // Get all table names
        tableNames = dbCreate.fetch("SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'")
                .map(result -> result.get(0).toString());

        // Map a table (String) to a list of columns (List<String>)
        fieldMap = tableNames.stream()
            .collect(Collectors.toMap(
                tableName -> tableName,
                tableName -> dbCreate.fetch("DESCRIBE `" + tableName + "`")
                    .map(result -> result.get(0).toString())
            ));

        // Map a table (String) to a list of primary keys (List<String>)
        primaryKeyMap = tableNames.stream()
            .collect(Collectors.toMap(
                tableName -> tableName,
                tableName -> dbCreate.fetch("SHOW KEYS FROM`" + tableName + "`WHERE Key_name = 'PRIMARY'")
                    .map(result -> result.get(DSL.field("Column_name")).toString())
            ));
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

    public List<String> getFields(String table) {
        return fieldMap.get(table);
    }

    public List<String> getPrimaryKeys(String table) {
        return primaryKeyMap.get(table);
    }

    public DSLContext getCreate() {
        return dbCreate;
    }

}
