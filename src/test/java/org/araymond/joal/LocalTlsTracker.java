package org.araymond.joal;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Test-only TLS tracker and proxy. CONNECT is permitted only to this fixture's loopback TLS port. */
final class LocalTlsTracker implements AutoCloseable {
    final Path trustStore;
    final AtomicInteger requests = new AtomicInteger();
    private final HttpsServer tracker;
    private final ServerSocket proxy;
    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();

    LocalTlsTracker(Path directory) throws Exception {
        trustStore = directory.resolve("tls.p12");
        final Path keytoolLog = directory.resolve("keytool.log");
        final Process keytool = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/keytool").toString(),
                "-genkeypair", "-alias", "local", "-keyalg", "RSA", "-keysize", "2048", "-dname", "CN=localhost",
                "-ext", "SAN=ip:127.0.0.1,dns:localhost", "-storetype", "PKCS12", "-storepass", "changeit",
                "-keypass", "changeit", "-keystore", trustStore.toString(), "-validity", "1", "-noprompt")
                .redirectErrorStream(true).redirectOutput(keytoolLog.toFile()).start();
        if (!keytool.waitFor(15, TimeUnit.SECONDS)) {
            keytool.destroyForcibly();
            throw new IOException("Test certificate generation timed out");
        }
        if (keytool.exitValue() != 0) throw new IOException(Files.readString(keytoolLog));
        final KeyStore keys = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(trustStore)) { keys.load(input, "changeit".toCharArray()); }
        final KeyManagerFactory managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        managers.init(keys, "changeit".toCharArray());
        final SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(managers.getKeyManagers(), null, null);
        tracker = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        tracker.setHttpsConfigurator(new HttpsConfigurator(ssl));
        tracker.createContext("/announce", exchange -> {
            requests.incrementAndGet();
            final byte[] response = "d8:completei2e10:incompletei3e8:intervali60e5:peers0:e".getBytes(StandardCharsets.US_ASCII);
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) { output.write(response); }
        });
        proxy = new ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"));
        tracker.start();
        workers.submit(() -> {
            while (!proxy.isClosed()) {
                try {
                    final Socket socket = proxy.accept();
                    workers.submit(() -> handle(socket));
                } catch (IOException closed) {
                    break;
                }
            }
        });
    }

    int proxyPort() { return proxy.getLocalPort(); }
    String url() { return "https://127.0.0.1:" + tracker.getAddress().getPort() + "/announce"; }

    private void handle(Socket socket) {
        try (socket) {
            socket.setSoTimeout(5000);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            final String request = reader.readLine();
            String header;
            while ((header = reader.readLine()) != null && !header.isEmpty()) { }
            if (request == null) return;
            if (!request.startsWith("CONNECT ")) {
                socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 9\r\nConnection: close\r\n\r\n127.0.0.1".getBytes(StandardCharsets.US_ASCII));
            } else if (request.startsWith("CONNECT 127.0.0.1:" + tracker.getAddress().getPort() + " ")) {
                try (Socket upstream = new Socket("127.0.0.1", tracker.getAddress().getPort())) {
                    upstream.setSoTimeout(5000);
                    socket.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                    workers.submit(() -> {
                        try { upstream.getInputStream().transferTo(socket.getOutputStream()); }
                        catch (IOException closed) { /* Client shutdown or fixture timeout. */ }
                    });
                    socket.getInputStream().transferTo(upstream.getOutputStream());
                }
            } else {
                socket.getOutputStream().write("HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
            }
        } catch (IOException closed) { /* Client shutdown or fixture timeout. */ }
    }

    @Override
    public void close() throws IOException {
        proxy.close();
        tracker.stop(0);
        workers.shutdownNow();
        workers.close();
    }
}
