package org.gradle.wrapper;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.zip.*;

/** Small bootstrap used only because generated projects cannot bundle Gradle's copyrighted distribution. */
public final class GradleWrapperMain {
    public static void main(String[] args) throws Exception {
        String version = "8.13";
        Path root = Paths.get(System.getProperty("user.home"), ".gradle", "wrapper", "dists", "parchment-" + version);
        Path launcher = root.resolve("gradle-" + version).resolve("lib").resolve("gradle-launcher-" + version + ".jar");
        if (!Files.exists(launcher)) {
            Files.createDirectories(root);
            Path zip = root.resolve("gradle.zip");
            System.out.println("Downloading Gradle " + version + "…");
            URLConnection connection = new URL("https://services.gradle.org/distributions/gradle-" + version + "-bin.zip").openConnection();
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(120_000);
            try (InputStream in = connection.getInputStream()) { Files.copy(in, zip, StandardCopyOption.REPLACE_EXISTING); }
            unzip(zip, root);
            Files.deleteIfExists(zip);
        }
        URLClassLoader loader = new URLClassLoader(new URL[]{launcher.toUri().toURL()}, ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(loader);
        Class<?> main = loader.loadClass("org.gradle.launcher.GradleMain");
        main.getMethod("main", String[].class).invoke(null, (Object) args);
    }

    private static void unzip(Path archive, Path target) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path output = target.resolve(entry.getName()).normalize();
                if (!output.startsWith(target)) throw new IOException("Unsafe archive entry");
                if (entry.isDirectory()) Files.createDirectories(output);
                else {
                    Files.createDirectories(output.getParent());
                    Files.copy(zip, output, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
