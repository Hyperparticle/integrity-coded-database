package verify.serial

import org.apache.logging.log4j.LogManager
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.serializer.GroupSerializer
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicLong

/**
 * Maintains the Integrity Code Revocation List (ICRL), storing and loading it from a file.

 * Created 5/8/2016
 * @author Dan Kondratyuk
 */
class Icrl private constructor() {

    private val db: DB
    private val serials: RevocationTree<Long>

    private val pending: AtomicLong
    private val next: AtomicLong

    private val logger = LogManager.getLogger()

    init {
        // Load the DB
        db = DBMaker.fileDB(ICRL_FILE).fileMmapEnable().fileMmapPreclearDisable().make()
        db.getStore().fileLoad()
        Runtime.getRuntime().addShutdownHook(Thread({ db.close() }))

        // Generate the map
        val treeMap = db.treeMap("serial", GroupSerializer.LONG, GroupSerializer.LONG).createOrOpen()

        // Add a random start value if one does not exist
        serials = when {
            treeMap.isEmpty() -> LongRevocationTree(treeMap, Rng.next())
            else -> LongRevocationTree(treeMap)
        }

        pending = AtomicLong(serials.next)
        next = AtomicLong(serials.next)
    }

    /**
     * Increments the current serial counter by 1
     * @return the newest valid serial number
     */
    fun addNext(): Long = next.andIncrement

    fun commit() {
        if (next.get() == pending.get()) { return }

        logger.debug("Committed new serial range: [{}, {}]", next.get(), pending.get())

        serials.add(next.get() - pending.get())
        pending.set(next.get())
    }

    /**
     * Revokes the serial number from the list
     */
    fun revoke(serial: Long) {
        // TODO: commit pending serials
        serials.remove(serial)
    }

    /**
     * Validates whether the serial is contained in the valid list of serial numbers
     */
    operator fun contains(serial: Long): Boolean {
        return serials.contains(serial)
    }

    companion object {
        private val ICRL_FILE = File("./src/main/resources/icrl.db")

        // If init() is called before getting the Icrl, then the it is reset
        private var reset: Boolean = false
        val icrl: Icrl by lazy {
            if (reset) { Files.deleteIfExists(ICRL_FILE.toPath()) }
            Icrl()
        }

        fun init(): Icrl {
            reset = true
            return icrl
        }
    }

}
