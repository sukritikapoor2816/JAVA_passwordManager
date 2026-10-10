import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class CredentialStore {

    private static final Path VAULT_DIRECTORY = Paths.get(
        System.getProperty("user.home"), ".password-manager"
    );

    private static Path getVaultFile(String email) {
        String safeName = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(email.trim().toLowerCase()
                .getBytes(StandardCharsets.UTF_8));

        return VAULT_DIRECTORY.resolve(safeName + ".vault");
    }

    public static void save(
            String email, String website, String username,
            String password, byte[] key) throws Exception {

        List<String> records = loadRecords(email, key);

        String record = encode(website) + "|"
            + encode(username) + "|" + encode(password);

        records.add(record);

        StringBuilder data = new StringBuilder();

        for (String item : records) {
            data.append(item).append("\n");
        }

        String encrypted = VaultManager.encrypt(data.toString(), key);

        Files.createDirectories(VAULT_DIRECTORY);
        Files.writeString(
            getVaultFile(email), encrypted,
            StandardCharsets.UTF_8
        );
    }

    public static List<String[]> getAll(
            String email, byte[] key) throws Exception {

        List<String[]> credentials = new ArrayList<>();

        for (String record : loadRecords(email, key)) {
            if (record.isEmpty()) {
                continue;
            }

            String[] parts = record.split("\\|", -1);

            if (parts.length != 3) {
                throw new IOException("Invalid vault record.");
            }

            credentials.add(new String[] {
                decode(parts[0]),
                decode(parts[1]),
                decode(parts[2])
            });
        }

        return credentials;
    }

    public static void delete(
            String email, int index, byte[] key) throws Exception {

        List<String> records = loadRecords(email, key);

        if (index < 0 || index >= records.size()) {
            throw new IndexOutOfBoundsException(
                "Credential does not exist."
            );
        }

        records.remove(index);

        StringBuilder data = new StringBuilder();

        for (String item : records) {
            data.append(item).append("\n");
        }

        String encrypted = VaultManager.encrypt(data.toString(), key);

        Files.createDirectories(VAULT_DIRECTORY);
        Files.writeString(
            getVaultFile(email), encrypted,
            StandardCharsets.UTF_8
        );
    }

    private static List<String> loadRecords(
            String email, byte[] key) throws Exception {

        Path file = getVaultFile(email);
        List<String> records = new ArrayList<>();

        if (!Files.exists(file)) {
            return records;
        }

        String encrypted = Files.readString(
            file, StandardCharsets.UTF_8
        );

        String plainText = VaultManager.decrypt(encrypted, key);

        for (String line : plainText.split("\\n")) {
            if (!line.isEmpty()) {
                records.add(line);
            }
        }

        return records;
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(
            value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String decode(String value) {
        return new String(
            Base64.getDecoder().decode(value),
            StandardCharsets.UTF_8
        );
    }
}