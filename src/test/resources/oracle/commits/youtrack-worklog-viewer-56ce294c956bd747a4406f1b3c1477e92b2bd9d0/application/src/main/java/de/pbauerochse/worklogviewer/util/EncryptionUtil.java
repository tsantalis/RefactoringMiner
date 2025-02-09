package de.pbauerochse.worklogviewer.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.File;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * @author Patrick Bauerochse
 * @since 13.04.15
 */
public class EncryptionUtil {

    private static final String ENCRYPTION = "PBEWithMD5AndDES";
    private static final Charset UTF8 = Charset.forName("utf-8");
    private static final String FIXED_PREFIX = "YouTrackTimelogger::";

    private static final byte[] SALT = new byte[] {
            (byte) 0x12, (byte) 0x04, (byte) 0x18, (byte) 0x96,
            (byte) 0x11, (byte) 0x10, (byte) 0x19, (byte) 0x82
    };

    public static String encryptCleartextString(String cleartext) throws GeneralSecurityException {
        Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);
        return base64Encode(cipher.doFinal(cleartext.getBytes(UTF8)));
    }

    public static String decryptEncryptedString(String encrypted) throws GeneralSecurityException {
        Cipher cipher = getCipher(Cipher.DECRYPT_MODE);
        return new String(cipher.doFinal(base64Decode(encrypted)), UTF8);
    }

    private static Cipher getCipher(int mode) throws GeneralSecurityException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ENCRYPTION);
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(getSystemPassword()));
        Cipher cipher = Cipher.getInstance(ENCRYPTION);
        cipher.init(mode, key, new PBEParameterSpec(SALT, 20));
        return cipher;
    }

    private static char[] getSystemPassword() {
        return new StringBuilder(FIXED_PREFIX)
                .append(System.getProperty("user.name"))
                .append(File.pathSeparatorChar)
                .append(System.getProperty("user.home"))
                .append(File.pathSeparatorChar)
                .append(System.getProperty("os.name"))
                .append(File.pathSeparatorChar)
                .append(System.getProperty("os.arch"))
                .toString()
                .toCharArray();
    }

    private static String base64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] base64Decode(String data) {
        return Base64.getDecoder().decode(data.getBytes(UTF8));
    }
}
