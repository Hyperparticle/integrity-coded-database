package crypto.signer;


import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;


/**
 * Created by ujwal-mac on 9/15/16.
 */
public class RSASHA1Signer {
    private BigInteger modulus;
    private BigInteger exponent;

    public RSASHA1Signer(BigInteger modulus, BigInteger exponent){

        this.exponent=exponent;
        this.modulus=modulus;
    }

    public byte[] computeSHA1RSA(byte[] data){
        return computeRSA(computehash(data));
    }

    public byte[] computehash(byte[] data){
        byte[] hashedData = new byte[0];
        try
        {
            //prepare the input
            MessageDigest hash =MessageDigest.getInstance("SHA-1", "BC");
            hash.update(data);

            hashedData=hash.digest();
        }
        catch (NoSuchAlgorithmException e)
        {
            System.err.println("No such algorithm");
            e.printStackTrace();
        }
        catch (NoSuchProviderException e)
        {
            System.err.println("No such provider");
            e.printStackTrace();
        }
        return hashedData;
    }

    public byte[] computeRSA(byte[] hash){
        return new BigInteger(hash).modPow(exponent, modulus).toByteArray();
    }

    public boolean verify(byte[] data, byte[] signature) {
        return Arrays.equals(computeSHA1RSA(data), signature);
    }

}
