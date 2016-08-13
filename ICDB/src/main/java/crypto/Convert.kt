package crypto

import com.google.common.io.BaseEncoding

/**
 * Converts data to different representations.
 *
 * Created on 6/18/2016
 * @author Dan Kondratyuk
 */
object Convert {

    // TODO: remove static annotations
    @JvmStatic private val base64 = BaseEncoding.base64()

    @JvmStatic fun toBase64(signature: ByteArray): String = base64.encode(signature)
    @JvmStatic fun fromBase64(signature: String): ByteArray = base64.decode(signature)

}
