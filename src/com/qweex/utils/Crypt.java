package com.qweex.utils;

import android.util.Base64;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

// Heavily "inspired" by http://stackoverflow.com/questions/3451670/java-aes-and-using-my-own-key
public class Crypt {
    private static String ALGORITHM, KEY_ALGORITHM, SALT;
    private static int KEY_SIZE, ITER_COUNT, SALT_LENGTH;
    static {
        KEY_ALGORITHM = "PBKDF2WithHmacSHA1";
        ALGORITHM = "AES";
        SALT = null;
        SALT_LENGTH = 20;
        KEY_SIZE = 256;
        ITER_COUNT = 1; //65536;
    }

    public static Key getKeyFromPassword(String password) {
        SecretKey key = null;
        try {
            if(SALT==null)
            {
                byte[] s = new byte[SALT_LENGTH];
                new SecureRandom().nextBytes(s);
                SALT = new String(s);
            }

            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), SALT.getBytes(), ITER_COUNT, KEY_SIZE);
            key = factory.generateSecret(spec); //tmp
            key = new SecretKeySpec(key.getEncoded(), ALGORITHM);
        } catch(Exception e) {}

        return key;
    }

    public static String encrypt(String dataToEncode, Key key) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.encodeToString(cipher.doFinal(dataToEncode.getBytes()), Base64.DEFAULT);
    }

    public static String decrypt(String dataToDecode, Key key) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(Base64.decode(dataToDecode.getBytes(), Base64.DEFAULT)));
    }


    public static void setAlgorithm(String algorithm) { ALGORITHM = algorithm; }
    public static void setSalt(String salt) { SALT = salt; }
    public static void setSaltLength(int salt_length) { SALT_LENGTH = salt_length; }
    public static void setKeySize(int keysize) {KEY_SIZE = keysize; }
    public static void setIterationCount(int iterationCount) { ITER_COUNT = iterationCount; }
    public static String getAlgorithm() { return ALGORITHM; }
    public static String getSalt() { return SALT; }
    public static int getSaltLength() { return SALT_LENGTH; }
    public static int getKeySize() { return KEY_SIZE; }
    public static int getIterationCount() { return ITER_COUNT; }
}
