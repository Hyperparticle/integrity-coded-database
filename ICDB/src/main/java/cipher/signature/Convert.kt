package cipher.signature

import com.google.common.io.BaseEncoding

/**
 * Converts data to different representations.
 *
 * Created on 6/18/2016
 * @author Dan Kondratyuk
 */
object Convert {

    fun toBase64(signature: ByteArray): String {
        return BaseEncoding.base64().encode(signature)
    }

    fun fromBase64(signature: String): ByteArray {
        return BaseEncoding.base64().decode(signature)
    }

}
