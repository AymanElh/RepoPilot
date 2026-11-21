package dev.ayman.repopilot.repository;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class GithubArchiveClientTest {
    @Test
    void downloadsArchiveAfterRedirect() throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("repo/README.md"));
            zip.write("Sample repository".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        byte[] archive = bytes.toByteArray();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/repos/owner/repo/zipball/commit", exchange -> {
            exchange.getResponseHeaders().add("Location", "/archive.zip");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/archive.zip", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/zip");
            exchange.sendResponseHeaders(200, archive.length);
            try (var response = exchange.getResponseBody()) {
                response.write(archive);
            }
        });
        server.start();
        Path downloaded = null;
        try {
            var client = new GithubArchiveClient("http://127.0.0.1:" + server.getAddress().getPort(), "");
            downloaded = client.download("owner", "repo", "commit");
            assertArrayEquals(archive, Files.readAllBytes(downloaded));
        } finally {
            if (downloaded != null) Files.deleteIfExists(downloaded);
            server.stop(0);
        }
    }
}
