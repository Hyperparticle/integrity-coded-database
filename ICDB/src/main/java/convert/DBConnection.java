package convert;

import com.mysql.cj.jdbc.MysqlDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * <p>
 * </p>
 * Created on 6/3/2016
 *
 * @author Dan Kondratyuk
 */
public class DBConnection {

    private static final MysqlDataSource dataSource = new MysqlDataSource();

    public DBConnection(String serverName, int portNumber, String user, String password) {
        dataSource.setServerName(serverName);
        dataSource.setPortNumber(portNumber);
        dataSource.setUser(user);
        dataSource.setPassword(password);
    }

    public Connection connect(String databaseName) throws SQLException {
        dataSource.setDatabaseName(databaseName);
        return dataSource.getConnection();
    }
}
