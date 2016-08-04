package verify.serial

import org.apache.logging.log4j.LogManager
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.serializer.GroupSerializer

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.security.SecureRandom

/**
 * Maintains the Integrity Code Revocation List (ICRL), storing and loading it from a file.

 * Created 5/8/2016
 * @author Dan Kondratyuk
 */
class Icrl private constructor() {

    private val db: DB
    private val serials: RevocationTree<Long>

    private val random = SecureRandom()

    init {
        // Load the DB
        db = DBMaker.fileDB(ICRL_LOCATION).fileMmapEnable().fileMmapPreclearDisable().make()
        db.getStore().fileLoad()

        // Generate the map
        val treeMap = db.treeMap("serial", GroupSerializer.LONG, GroupSerializer.LONG).createOrOpen()

        // Add a random start value if one does not exist
        serials = when {
            treeMap.isEmpty() -> LongRevocationTree(treeMap, random.nextInt(Integer.MAX_VALUE).toLong())
            else -> LongRevocationTree(treeMap)
        }
    }

    /**
     * Increments the current serial counter by 1, and copies it to the ICRL storage as a valid serial
     * @return the new serial number
     */
    val next: Long
        get() {
            val next = current!!.incrementAndGet()
            serials.add(next)
            return next
        }

    /**
     * @return the next serial number to be generated
     */
    fun peekNext(): Long {
        return current!!.get() + 1
    }

    /**
     * Adds a serial number to the list
     */
    fun add(serial: Long) {
        serials.add(serial)
    }

    /**
     * Revokes the serial number from the list
     */
    fun revoke(serial: Long) {
        serials.remove(serial)
    }

    operator fun contains(serial: Long): Boolean {
        return serials.contains(serial)
    }

    private fun save() {
        db.close()
    }

    companion object {

        private val ICRL_LOCATION = "./src/main/resources/icrl.db"
        private val logger = LogManager.getLogger()

        val icrl: Icrl = Icrl()
            get() {
                    icrl

                }

                return icrl
            }

        init {
            Runtime.getRuntime().addShutdownHook(Thread({ icrl.save() }))
        }

        fun reset(): Icrl {
            try {
                // Delete the old DB
                Files.deleteIfExists(Paths.get(ICRL_LOCATION))
            } catch (e: IOException) {
                logger.error("Failed to reset ICRL DB: {}", e.message)
                System.exit(1)
            }

            return icrl
        }
    }

}
