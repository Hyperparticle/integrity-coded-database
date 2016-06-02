package main.args.option;

import cipher.AESCipher;
import cipher.CodeCipher;
import cipher.RSACipher;
import cipher.SHACipher;

import java.nio.file.Paths;

/**
 * <p>
 * </p>
 * Created on 5/21/2016
 *
 * @author Dan Kondratyuk
 */
public enum CipherType {
    RSA {
    },
    AES {
    },
    SHA {
    };

}
