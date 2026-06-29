package security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private static final String ALGORITHM =
            "PBKDF2WithHmacSHA256";

    private static final int ITERATIONS = 120000;
    private static final int SALT_LENGTH = 16;
    private static final int KEY_LENGTH = 256;

    private PasswordUtil() {
    }

    public static String createHash(String password) {

        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);

        byte[] hash = deriveKey(
                password.toCharArray(),
                salt,
                ITERATIONS);

        return ITERATIONS
                + ":"
                + Base64.getEncoder()
                        .encodeToString(salt)
                + ":"
                + Base64.getEncoder()
                        .encodeToString(hash);
    }

    public static boolean verify(
            String password,
            String storedValue) {

        if (password == null || storedValue == null) {
            return false;
        }

        String[] parts = storedValue.split(":");

        if (parts.length != 3) {
            return false;
        }

        try {
            int iterations =
                    Integer.parseInt(parts[0]);

            byte[] salt =
                    Base64.getDecoder().decode(parts[1]);

            byte[] expectedHash =
                    Base64.getDecoder().decode(parts[2]);

            byte[] actualHash = deriveKey(
                    password.toCharArray(),
                    salt,
                    iterations);

            return MessageDigest.isEqual(
                    expectedHash,
                    actualHash);

        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] deriveKey(
            char[] password,
            byte[] salt,
            int iterations) {

        PBEKeySpec spec = new PBEKeySpec(
                password,
                salt,
                iterations,
                KEY_LENGTH);

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            ALGORITHM);

            return factory.generateSecret(spec)
                          .getEncoded();

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "パスワードのハッシュ化に失敗しました。",
                    e);

        } finally {
            spec.clearPassword();
        }
    }
}