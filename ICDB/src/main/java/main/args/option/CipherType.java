package main.args.option;

import cipher.AESCipher;
import cipher.CodeCipher;
import cipher.RSACipher;
import cipher.SHACipher;

import java.nio.file.Path;
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
        @Override
        public CodeCipher getCipher(String key) {
            return new RSACipher(Paths.get("."), "");
        }
    },
    AES {
        @Override
        public CodeCipher getCipher(String key) {
            return new AESCipher(Paths.get("."), "");
        }
    },
    SHA {
        @Override
        public CodeCipher getCipher(String salt) {
            return new SHACipher(salt);
        }
    };

    public abstract CodeCipher getCipher(String key);
}
