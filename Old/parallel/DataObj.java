package parallel;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DataObj {

	private long serialNumber;
	public long getSerialNumber() {
		return serialNumber;
	}
	public void setSerialNumber(long serialNumber) {
		this.serialNumber = serialNumber;
	}
	
	private final SecureRandom random;
	public SecureRandom getRandom() {
		return random;
	}

	private BigInteger privateKey;
	public BigInteger getPrivateKey() {
		return privateKey;
	}
	public void setPrivateKey(BigInteger privateKey) {
		this.privateKey = privateKey;
	}
	
	private BigInteger publicKey;
	public BigInteger getPublicKey() {
		return publicKey;
	}
	public void setPublicKey(BigInteger publicKey) {
		this.publicKey = publicKey;
	}
	
	private BigInteger modulus;
	public BigInteger getModulus() {
		return modulus;
	}
	public void setModulus(BigInteger modulus) {
		this.modulus = modulus;
	}

	private ArrayList<String> primaryKeyList;
	public ArrayList<String> getPrimaryKeyList() {
		return primaryKeyList;
	}
	public void setPrimaryKeyList(ArrayList<String> primaryKeyList) {
		this.primaryKeyList = primaryKeyList;
	}
	
	private ArrayList<String> atrList;
	public ArrayList<String> getAtrList() {
		return atrList;
	}
	
	private Map<String, String> primaryKeyListWithUnl;
	public Map<String, String> getPrimaryKeyListWithUnl() {
		return primaryKeyListWithUnl;
	}
	
	private Map<String, String> keyPositionMap;
	public Map<String, String> getKeyPositionMap() {
		return keyPositionMap;
	}
	
	private Map<String, String> attributeMap;
	public Map<String, String> getAttributeMap() {
		return attributeMap;
	}
	
	private Path schemaFilePath;
	public Path getSchemaFilePath() {
		return schemaFilePath;
	}
	
	private Path unlFilePath;
	public Path getUnlFilePath() {
		return unlFilePath;
	}
	
	public DataObj(Path schemaFilePath, Path unlFilePath) {
		serialNumber = 0;
		privateKey = null;
		publicKey = null;
		modulus = null;
		primaryKeyList = new ArrayList<String>();
		atrList = new ArrayList<String>();
		primaryKeyListWithUnl = new HashMap<String, String>();
		keyPositionMap = new HashMap<String, String>();
		attributeMap = new HashMap<String, String>();
		random = new SecureRandom();
		this.schemaFilePath = schemaFilePath;
		this.unlFilePath = unlFilePath;
	}
}
