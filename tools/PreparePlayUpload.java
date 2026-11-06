import java.io.Console;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Run locally with JDK 17+. Only public store configuration is inserted in the tested AAB. */
public class PreparePlayUpload {
    private static final String CONFIG = "base/assets/play-release.properties";
    private static final String DEV_CERT = "06d91de19adf5add10e5a54dbf14bc81d6604cdad7b783e3137a8eed55c24d0e";

    public static void main(String[] args) {
        if (args.length < 4 || args.length > 5) {
            System.err.println("Usage: java PreparePlayUpload.java <unsigned.aab> <upload.aab> <upload-keystore.p12> <alias> [public-config.properties]");
            System.exit(2);
        }
        try {
            prepare(args);
        } catch (Exception e) {
            System.err.println("No upload bundle created: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void prepare(String[] args) throws Exception {
        Path input = Path.of(args[0]).toAbsolutePath();
        Path output = Path.of(args[1]).toAbsolutePath();
        Path keystore = Path.of(args[2]).toAbsolutePath();
        if (Files.exists(output) || input.equals(output)) throw new IllegalArgumentException("Choose a new output filename.");
        if (!Files.isRegularFile(input) || !Files.isRegularFile(keystore)) throw new IllegalArgumentException("Input AAB or upload keystore is missing.");
        if (!output.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".aab")) throw new IllegalArgumentException("Use .aab for the output.");
        Properties config = config(args.length == 5 ? Path.of(args[4]) : null);
        StringWriter writer = new StringWriter();
        config.store(writer, "Public BOSS XI Play release configuration");
        byte[] data = writer.toString().getBytes(StandardCharsets.UTF_8);
        Path unsigned = Files.createTempFile(output.getParent(), "boss-xi-configured-", ".aab");
        Path signed = null;
        try {
            signed = Files.createTempFile(output.getParent(), "boss-xi-signed-", ".aab");
            repack(input, unsigned, data);
            String binary = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "jarsigner.exe" : "jarsigner";
            Path jarsigner = Path.of(System.getProperty("java.home"), "bin", binary);
            if (!Files.isRegularFile(jarsigner)) throw new IllegalArgumentException("Run this with a full JDK 17 or later, which includes jarsigner.");
            Process process = new ProcessBuilder(jarsigner.toString(), "-keystore", keystore.toString(),
                    "-digestalg", "SHA-256", "-signedjar", signed.toString(), unsigned.toString(), args[3])
                    .inheritIO().start();
            if (process.waitFor() != 0) throw new IllegalStateException("Signing failed. Check the keystore password and alias.");
            String fingerprint = verify(signed, data);
            Files.move(signed, output);
            System.out.println("Signed bundle: " + output);
            System.out.println("Upload certificate SHA-256: " + fingerprint);
            System.out.println("Upload it to the Internal testing track first.");
        } finally {
            Files.deleteIfExists(unsigned);
            if (signed != null) Files.deleteIfExists(signed);
        }
    }

    private static Properties config(Path file) throws Exception {
        Properties values = new Properties();
        if (file != null) {
            try (var in = Files.newBufferedReader(file)) { values.load(in); }
        } else {
            Console console = System.console();
            if (console == null) throw new IllegalArgumentException("Run in a terminal or supply a public-config.properties file.");
            values.setProperty("billing_public_key", requireLine(console, "Play Console public RSA licence key: "));
            values.setProperty("privacy_url", requireLine(console, "Public HTTPS privacy policy URL: "));
            values.setProperty("support_email", requireLine(console, "Public support email: "));
        }
        String key = values.getProperty("billing_public_key", "").replaceAll("\\s", "");
        String url = values.getProperty("privacy_url", "").trim();
        String email = values.getProperty("support_email", "").trim();
        try {
            RSAPublicKey rsa = (RSAPublicKey)KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(key)));
            if (rsa.getModulus().bitLength() < 2048) throw new IllegalArgumentException();
        } catch (Exception e) {
            throw new IllegalArgumentException("Enter the Base64 public RSA licence key from Monetize > Monetization setup > Licensing.");
        }
        URI uri;
        try { uri = URI.create(url); } catch (IllegalArgumentException e) { throw new IllegalArgumentException("Enter a valid public HTTPS privacy URL."); }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Enter a valid public HTTPS privacy URL.");
        }
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) throw new IllegalArgumentException("Enter a valid public support email.");
        Properties onlyPublicValues = new Properties();
        onlyPublicValues.setProperty("billing_public_key", key);
        onlyPublicValues.setProperty("privacy_url", url);
        onlyPublicValues.setProperty("support_email", email);
        return onlyPublicValues;
    }

    private static String requireLine(Console console, String prompt) {
        String value = console.readLine("%s", prompt);
        if (value == null) throw new IllegalArgumentException("Configuration cancelled.");
        return value;
    }

    private static void repack(Path input, Path output, byte[] data) throws Exception {
        try (ZipFile zip = new ZipFile(input.toFile());
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(output))) {
            if (zip.getEntry(CONFIG) == null || zip.getEntry("BundleConfig.pb") == null
                    || zip.getEntry("base/manifest/AndroidManifest.xml") == null) {
                throw new IllegalArgumentException("Use the unsigned BOSS XI AAB from the launch kit.");
            }
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry source = entries.nextElement();
                String name = source.getName();
                if (name.toUpperCase(Locale.ROOT).matches("META-INF/(?:[^/]+\\.(?:SF|RSA|DSA|EC)|SIG-[^/]+)")) {
                    throw new IllegalArgumentException("Use the original unsigned bundle.");
                }
                boolean replace = name.equals(CONFIG);
                ZipEntry next = new ZipEntry(name);
                next.setMethod(source.getMethod());
                if (source.getTime() >= 0) next.setTime(source.getTime());
                if (source.getMethod() == ZipEntry.STORED) {
                    CRC32 crc = new CRC32();
                    if (replace) crc.update(data);
                    long size = replace ? data.length : source.getSize();
                    next.setSize(size);
                    next.setCompressedSize(size);
                    next.setCrc(replace ? crc.getValue() : source.getCrc());
                }
                out.putNextEntry(next);
                if (replace) out.write(data);
                else try (InputStream in = zip.getInputStream(source)) { in.transferTo(out); }
                out.closeEntry();
            }
        }
    }

    private static String verify(Path signed, byte[] data) throws Exception {
        String fingerprint = "";
        try (JarFile jar = new JarFile(signed.toFile(), true)) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory() || entry.getName().startsWith("META-INF/")) continue;
                try (InputStream in = jar.getInputStream(entry)) {
                    if (entry.getName().equals(CONFIG)) {
                        if (!MessageDigest.isEqual(in.readAllBytes(), data)) throw new IllegalStateException("Bundle configuration verification failed.");
                    } else in.transferTo(OutputStream.nullOutputStream());
                }
                if (entry.getCodeSigners() == null || entry.getCodeSigners().length != 1) {
                    throw new IllegalStateException("A bundle file is not signed by exactly one upload key.");
                }
                Certificate cert = entry.getCodeSigners()[0].getSignerCertPath().getCertificates().get(0);
                String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
                if (digest.equals(DEV_CERT)) throw new IllegalArgumentException("The BOSS XI public test key cannot sign a Play upload.");
                if (!fingerprint.isEmpty() && !digest.equals(fingerprint)) throw new IllegalStateException("Bundle contains more than one signing identity.");
                fingerprint = digest;
            }
        }
        if (fingerprint.isEmpty()) throw new IllegalStateException("No signed files found.");
        return fingerprint;
    }
}
