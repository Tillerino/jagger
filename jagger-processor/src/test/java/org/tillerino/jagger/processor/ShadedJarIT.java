package org.tillerino.jagger.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

class ShadedJarIT {
    @Test
    void shadedJarContainsLicenseFile() throws IOException {
        Path shadedJar = Paths.get("target/jagger-processor-0.6.5-SNAPSHOT.jar");

        String version;
        try (ZipFile zip = new ZipFile(shadedJar.toFile())) {
            var pomPropertiesEntry =
                    zip.getEntry("META-INF/maven/org.tillerino.jagger/jagger-processor/pom.properties");
            Properties pomProperties = new Properties();
            pomProperties.load(zip.getInputStream(pomPropertiesEntry));
            version = pomProperties.getProperty("version");
        }

        Path jarPath = Paths.get("target/jagger-processor-" + version + ".jar");

        String licenseFromJar;
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            ZipEntry licenseEntry = zip.getEntry("META-INF/LICENSE.txt");
            assertThat(licenseEntry).isNotNull();
            licenseFromJar = new String(zip.getInputStream(licenseEntry).readAllBytes());

            assertThat(zip.getEntry("META-INF/NOTICE.txt")).isNotNull();

            var entries = zip.stream().map(ZipEntry::getName).toList();
            assertThat(entries)
                    .allMatch(e -> e.equals("org/")
                            || e.equals("org/tillerino/")
                            || e.startsWith("org/tillerino/jagger")
                            || e.equals("META-INF/")
                            || e.equals("META-INF/LICENSE.txt")
                            || e.equals("META-INF/NOTICE.txt")
                            || e.equals("META-INF/MANIFEST.MF")
                            || e.startsWith("META-INF/maven/")
                            || e.equals("META-INF/services/")
                            || e.equals("META-INF/services/javax.annotation.processing.Processor")
                            || e.startsWith("META-INF/versions/"));
        }

        Path projectRoot = Paths.get("").toAbsolutePath().getParent();
        Path licenseFile = projectRoot.resolve("LICENSE");
        String licenseFromFile = Files.readString(licenseFile);

        assertThat(licenseFromJar.trim()).isEqualTo(licenseFromFile.trim());
    }
}
