import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class AccountManager {
    private static final Path ACCOUNT_FILE = Paths.get(
        System.getProperty("user.home"), ".vault-accounts.properties"
    );
    private static final int ITERATIONS = 600_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static Properties loadAccounts() throws IOException {
        Properties properties = new Properties();
        if (Files.exists(ACCOUNT_FILE)) {
            try (var input = Files.newInputStream(ACCOUNT_FILE)) {
                properties.load(input);
            }
        }
        return properties;
    }

    private static void saveAccounts(Properties properties) throws IOException {
        Path parent = ACCOUNT_FILE.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (var output = Files.newOutputStream(ACCOUNT_FILE)) {
            properties.store(output, "Password Manager account data");
        }
    }

    public static synchronized boolean register(
            String email, char[] password) throws Exception {
        String normalizedEmail = normalizeEmail(email);
        Properties accounts = loadAccounts();

        if (accounts.containsKey(normalizedEmail)) {
            return false;
        }

        byte[] verificationSalt = randomSalt();
        byte[] encryptionSalt = randomSalt();
        byte[] passwordHash = derive(password, verificationSalt);

        String record = Base64.getEncoder().encodeToString(verificationSalt)
            + ":" + Base64.getEncoder().encodeToString(passwordHash)
            + ":" + Base64.getEncoder().encodeToString(encryptionSalt);

        accounts.setProperty(normalizedEmail, record);
        saveAccounts(accounts);
        return true;
    }

    public static synchronized boolean login(
            String email, char[] password) throws Exception {
        String normalizedEmail = normalizeEmail(email);
        Properties accounts = loadAccounts();
        String record = accounts.getProperty(normalizedEmail);

        if (record == null) {
            return false;
        }

        String[] parts = record.split(":", -1);
        if (parts.length != 3) {
            return false;
        }

        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
        byte[] actualHash = derive(password, salt);

        try {
            return MessageDigest.isEqual(expectedHash, actualHash);
        } finally {
            java.util.Arrays.fill(actualHash, (byte) 0);
        }
    }

    public static byte[] deriveEncryptionKey(
            String email, char[] password) throws Exception {
        String normalizedEmail = normalizeEmail(email);
        Properties accounts = loadAccounts();
        String record = accounts.getProperty(normalizedEmail);

        if (record == null) {
            throw new IOException("Account was not found.");
        }

        String[] parts = record.split(":", -1);
        if (parts.length != 3) {
            throw new IOException(
                "Account data format is outdated. Please register a new account."
            );
        }

        byte[] encryptionSalt = Base64.getDecoder().decode(parts[2]);
        return derive(password, encryptionSalt);
    }

    private static byte[] derive(char[] password, byte[] salt)
            throws Exception {
        PBEKeySpec spec = new PBEKeySpec(
            password, salt, ITERATIONS, KEY_LENGTH
        );
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] randomSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return salt;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
