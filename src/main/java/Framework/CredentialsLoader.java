package Framework;

import java.io.*;
import java.util.Base64;

public class CredentialsLoader {

    public static InputStream getCredentialsStream() {
        // Try environment variable first (for production)
        String base64Creds = System.getenv("GOOGLE_CREDENTIALS_BASE64");

        if (base64Creds != null && !base64Creds.isEmpty()) {
            byte[] decoded = Base64.getDecoder().decode(base64Creds);
            return new ByteArrayInputStream(decoded);
        }

        // Fall back to file (for local development)
        return CredentialsLoader.class.getResourceAsStream("/credentials.json");
    }
}