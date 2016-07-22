package crypto

import com.google.common.io.BaseEncoding

/**
 * Converts data to different representations.
 *
 * Created on 6/18/2016
 * @author Dan Kondratyuk
 */
object Convert {

    private val base64 = BaseEncoding.base64()

    fun toBase64(signature: ByteArray): String {
        return base64.encode(signature)
    }

    fun fromBase64(signature: String): ByteArray {
        return base64.decode(signature)
    }

}
