package verify.serial

/**
 * Defines the behavior of an Integrity Code Revocation List (ICRL)
 *
 * Created on 9/18/2016
 * @author Dan Kondratyuk
 */
interface AbstractIcrl {
    /**
     * Increments the current serial counter by 1
     * @return the newest valid serial number
     */
    fun addNext(): Long

    /**
     * Commits the running serials to the list
     */
    fun commit()

    /**
     * Revokes the serial number from the list
     */
    fun revoke(serial: Long)

    /**
     * Validates whether the serial is contained in the valid list of serial numbers
     */
    operator fun contains(serial: Long): Boolean
}