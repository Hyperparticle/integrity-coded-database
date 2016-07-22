package cipher.signature


import cipher.Key
import org.bouncycastle.crypto.BlockCipher
import org.bouncycastle.crypto.Mac
import org.bouncycastle.crypto.engines.AESFastEngine
import org.bouncycastle.crypto.macs.CMac
import org.bouncycastle.crypto.params.KeyParameter

import java.util.Arrays

/**
 *
 *
 * Created on 6/2/2016
 * @author Dan Kondratyuk
 */
object CMAC {

    private val DATA_SIZE = 16 // Size in bytes

    private val cmac: Mac
        get() {
            val aes = AESFastEngine()
            return CMac(aes)
        }

    fun generate(data: ByteArray, key: Key): ByteArray {
        val cmac = this.cmac

        cmac.init(key.macKey)
        cmac.update(data, 0, data.size)
        val result = ByteArray(DATA_SIZE)
        cmac.doFinal(result, 0)
        return result
    }

    fun verify(data: ByteArray, key: Key, signature: ByteArray): Boolean {
        return Arrays.equals(generate(data, key), signature)
    }

}
