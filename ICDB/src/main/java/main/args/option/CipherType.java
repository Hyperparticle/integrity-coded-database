package main.args.option;

import cipher.AESCipher;
import cipher.CodeCipher;
import cipher.RSACipher;
import cipher.SHACipher;

import java.nio.file.Path;

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
        public CodeCipher getCipher(Path dataPath, String databaseName) {
            return new RSACipher(dataPath, databaseName);
        }
    },
    AES {
        @Override
        public CodeCipher getCipher(Path dataPath, String databaseName) {
            return new AESCipher(dataPath, databaseName);
        }
    },
    SHA {
        @Override
        public CodeCipher getCipher(Path dataPath, String databaseName) {
            return new SHACipher(dataPath, databaseName);
        }
    };

    public abstract CodeCipher getCipher(Path dataPath, String databaseName);
}
