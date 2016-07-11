package verify;

import cipher.RNG;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 *     Maintains the Integrity Code Revocation List (ICRL).
 * </p>
 * Created 5/8/2016
 * @author Dan Kondratyuk
 */
public class ICRL implements Serializable {

    private static final long start = RNG.randomInt();
    private static long current = start;

//    private static Set<Long> serials = new HashSet<>(5_000_000);
    private static DB db = DBMaker.fileDB("./src/main/resources/icrl.db")
        .fileMmapEnable().make();
    private static Set<Long> serials = db.hashSet("serial", Serializer.LONG).create();

//    private static final String DB_LOCATION = "./src/main/resources/icrl.db";
//    private static DSLContext dbCreate;
//    private static Table<Record> table;
//    private static Field<Long> field;

    private static final Logger logger = LogManager.getLogger();

//    static {
//        try {
//            SQLiteDataSource dataSource = new SQLiteDataSource();
//            dataSource.setUrl("jdbc:sqlite:" + DB_LOCATION);
//            Connection connection = FiberDataSource.wrap(dataSource).getConnection();
//            dbCreate = DSL.using(connection, SQLDialect.SQLITE);
//
//            dbCreate.execute("CREATE TABLE IF NOT EXISTS `serials` (`serial` BIGINT NOT NULL, UNIQUE (`serial`), PRIMARY KEY (`serial`))");
//            dbCreate.truncate("serials");
//
//            table = DSL.table("serials");
//            field = DSL.field("serial", Long.class);
//        } catch (SQLException e) {
//            logger.error("Failed to connect to local SQLite DB: {}", e.getMessage());
//            System.exit(1);
//        }
//    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ICRL::save));
    }

    /**
     * Increments the current serial counter by 1, and copies it to the ICRL storage as a valid serial
     * @return the new serial number
     */
    public static long getNext() {
        current++;
        serials.add(current);

//        // Store all values to the DB
//        dbCreate.insertInto(table, field)
//                .values(current)
//                .execute();

        return current;
    }

    // TODO: load into memory
    private static void save() {
//        try(
//            FileOutputStream fileOutputStream = new FileOutputStream("./src/main/resources/icrl.ser");
//            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)
//        ) {
//            objectOutputStream.writeObject(serials);
//        } catch(Exception e) {
//            logger.error("Failed to save ICRL to disk: " + e.getMessage());
//        }

        db.close();
    }

}
