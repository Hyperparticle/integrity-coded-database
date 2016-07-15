
/**
 * <p>
 *A Test for different cipher implementation
 * </p>
 * Created 5/18/2016
 * 
 * @author UJwal 
 * @see CodeCipher
 */

import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

public class TestCipher {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testAESCipher() {
//		Path path = Paths.get("/Users/ujwal-cipher.signature/Desktop/TEST/AES");
//		AESCipher aescipher = new AESCipher(path, "world");
//		String encoded = aescipher.encrypt("testmessage");
//		System.out.println("AES::" + encoded);

//		assertTrue(aescipher.verify("testmessage", encoded));
	}

	@Test
	public void testRSACipher() {
//		Path path = Paths.get("/Users/ujwal-cipher.signature/Desktop/TEST/RSA");
//		RSACipher rsacipher = new RSACipher(path, "world");
//		String encoded = rsacipher.encrypt("testmessage");
//		System.out.println("RSA::" + encoded);
//
//		assertTrue(rsacipher.verify("testmessage", encoded));
	}

	@Test
	public void testSHACipher() {
//		Path path = Paths.get("/Users/ujwal-cipher.signature/Desktop/TEST/SHA");
//		SHACipher shacipher = new SHACipher();
//		String encoded = shacipher.encrypt("testmessage");
//		System.out.println("SHA::" + encoded);
//		assertTrue(shacipher.verify("testmessage", encoded));
	}

}
