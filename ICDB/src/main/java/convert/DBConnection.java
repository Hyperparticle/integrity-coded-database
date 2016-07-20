package convert;

import com.mysql.cj.jdbc.MysqlDataSource;
import main.args.config.ConfigArgs;
import main.args.config.UserConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

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

    /**
     * Configure a connection to a MySQL server
     */
    public static void configure(UserConfig dbConfig) {
        dataSource.setServerName(dbConfig.ip);
        dataSource.setPortNumber(dbConfig.port);
        dataSource.setUser(dbConfig.user);
        dataSource.setPassword(dbConfig.password);
    }

    public static DBConnection connect(String dbName, UserConfig dbConfig) {
        try {
            DBConnection db = new DBConnection(dbName);
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
//    private final Map<Table<?>, UniqueKey<?>> primaryKeyMap; // Map table names to primary keys
//    private final Map<Table<?>, List<Field<?>>> fieldMap;
    private final Map<String, List<String>> fieldStringMap;

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

//        primaryKeyMap = dbCreate
//                .meta().getPrimaryKeys().stream()
//                .collect(Collectors.toMap(Key::getTable, key -> key));

//        fieldMap = dbCreate
//                .meta().getTables()
//                .stream()
//                .collect(Collectors.toMap(table -> table, table -> Arrays.asList(table.fields())));


//        fieldStringMap = dbCreate
//            .meta().getTables().stream()
//            .collect(Collectors.toMap(
//                    QueryPart::toString,
//                    table -> Arrays.stream(table.fields())
//                        .map(QueryPart::toString)
//                        .collect(Collectors.toList())
//                )
//            );

        // Get all table names
        tableNames = dbCreate.fetch("SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'")
                .map(result -> result.get(0).toString());

        // Map a table (String) to a list of columns (List<String>)
        fieldStringMap = tableNames.stream()
            .collect(Collectors.toMap(
                tableName -> tableName,
                tableName -> dbCreate.fetch("DESCRIBE `" + tableName + "`")
                    .map(result -> result.get(0).toString())
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
        return fieldStringMap.get(table);
    }

//    public Map<Table<?>, UniqueKey<?>> getPrimaryKeyMap() {
//        return primaryKeyMap;
//    }

    public DSLContext getCreate() {
        return dbCreate;
    }

}
