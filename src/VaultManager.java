import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class VaultManager {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String encrypt(String plainText, byte[] key)
            throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey,
            new GCMParameterSpec(TAG_LENGTH_BITS, iv));

        byte[] encrypted = cipher.doFinal(
            plainText.getBytes(StandardCharsets.UTF_8)
        );

        ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
        buffer.put(iv);
        buffer.put(encrypted);
        return Base64.getEncoder().encodeToString(buffer.array());
    }

    public static String decrypt(String encryptedText, byte[] key)
            throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedText);
        if (combined.length <= IV_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted vault data.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(combined);
        byte[] iv = new byte[IV_LENGTH];
        buffer.get(iv);
        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey,
            new GCMParameterSpec(TAG_LENGTH_BITS, iv));

        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }
}
