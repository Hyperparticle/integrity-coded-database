package verify;

import cipher.RNG;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 *     Maintains the Integrity Code Revocation List (ICRL), storing and loading it from a file.
 * </p>
 * Created 5/8/2016
 * @author Dan Kondratyuk
 */
public class ICRL implements Serializable {

    private static final String ICRL_LOCATION = "./src/main/resources/icrl.db";

    private static final long start = RNG.randomInt();
    private static long current = start;

    private static DB db;
    private static Set<Long> serials;

    private static final Logger logger = LogManager.getLogger();

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

        return current;
    }

    // TODO: load into memory
    private static void save() {
        db.close();
    }

    public static void init(boolean newDB) {
        try {
            // Delete the old DB if a new one is requested
            if (newDB) {
                Files.deleteIfExists(Paths.get(ICRL_LOCATION));
            }

            // Load the DB
            db = DBMaker.fileDB(ICRL_LOCATION)
                    .fileMmapEnable()
                    .fileMmapPreclearDisable()
                    .allocateStartSize(80L * 1024*1024) // 80 MB
                    .make();
            db.getStore().fileLoad();

            // Generate the set
            serials = db
                    .treeSet("serial", Serializer.LONG)
                    .create();
        } catch (IOException e) {
            logger.error("Failed to initialize ICRL DB: {}", e.getMessage());
            System.exit(1);
        }
    }

//    private static Set<Long> serials = new HashSet<>(5_000_000);

//        try(
//            FileOutputStream fileOutputStream = new FileOutputStream("./src/main/resources/icrl.ser");
//            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)
//        ) {
//            objectOutputStream.writeObject(serials);
//        } catch(Exception e) {
//            logger.error("Failed to save ICRL to disk: " + e.getMessage());
//        }
}
