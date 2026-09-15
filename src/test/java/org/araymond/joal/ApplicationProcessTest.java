package org.araymond.joal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.*;

/** Black-box acceptance suite for either the native executable or the packaged JVM jar. */
@EnabledIfSystemProperty(named = "joal.test.application", matches = ".+")
class ApplicationProcessTest {
    @TempDir
    Path temp;
    private final ObjectMapper json = new ObjectMapper();
    private final List<App> apps = new ArrayList<>();
    private final BlockingQueue<String> announces = new LinkedBlockingQueue<>();
    private HttpServer proxy;
    private HttpClient http;

    @BeforeEach
    void startLocalNetwork() throws IOException {
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        proxy = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        proxy.createContext("/", exchange -> {
            // Every outbound HTTP request terminates here: no forwarding to external services.
            final String query = exchange.getRequestURI().getRawQuery();
            final boolean tracker = query != null && query.contains("info_hash=");
            final byte[] response = (tracker
                    ? "d8:completei2e10:incompletei3e8:intervali60e5:peers0:e"
                    : "127.0.0.1").getBytes(StandardCharsets.US_ASCII);
            if (tracker) announces.add(query);
            exchange.sendResponseHeaders(200, response.length);
            try (var body = exchange.getResponseBody()) { body.write(response); }
        });
        proxy.start();
    }

    @AfterEach
    void closeProcessesAndNetwork() throws Exception {
        try {
            for (App app : apps) app.close();
        } finally {
            if (proxy != null) proxy.stop(0);
            if (http != null) http.close();
        }
    }

    @Test
    void consoleColorsIncludeTomcatJulLogs() throws Exception {
        final App app = start(config("colors", "utorrent-3.5.0_43916.client"), availablePort(), false, "", "");
        app.ready();
        app.close();
        final String log = app.log();
        assertThat(log).contains("\u001b[34m", "\u001b[32m", "\u001b[35m", "\u001b[36m")
                .doesNotContain("Unrecognized format specifier", "MissingReflectionRegistrationError");
        final List<String> tomcat = log.lines().filter(line -> line.contains("Starting service [Tomcat]")).toList();
        assertThat(tomcat).hasSize(1);
        assertThat(tomcat.getFirst()).contains("\u001b[32m[INFO", "\u001b[36m", "StandardService");
    }

    @Test
    void disabledUiDoesNotListenAndExitsCleanly() throws Exception {
        final int port = availablePort();
        final App app = start(config("headless", "utorrent-3.5.0_43916.client"), port, false, "", "");
        app.ready();
        assertThatThrownBy(() -> {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 1000);
            }
        }).isInstanceOf(IOException.class);
        app.close();
        assertThat(app.log()).contains("JOAL gracefully shut down").doesNotContain("MissingReflectionRegistrationError");
    }

    @Test
    void uiAuthenticationConfigurationAndLocalTrackerWork() throws Exception {
        final Path conf = config("web", "utorrent-3.5.0_43916.client");
        final int port = availablePort();
        final App app = start(conf, port, true, "Native123", "local-token");
        app.ready();
        final var page = get(port, "/Native123/ui/");
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("<html");
        assertThat(get(port, "/Native123/ui/manifest.json").statusCode()).isEqualTo(200);
        for (String path : List.of("/", "/ui/", "/Native123suffix/ui/", "/Native123/other")) {
            assertThat(get(port, path).statusCode()).as(path).isIn(403, 404);
        }
        for (String token : List.of("wrong", "")) {
            try (Stomp stomp = connect(port, "Native123", token)) {
                assertThat(stomp.next()).startsWith("ERROR");
            }
        }
        try (Stomp stomp = connect(port, "Native123", "local-token")) {
            assertThat(stomp.next()).startsWith("CONNECTED");
            stomp.send("SUBSCRIBE\nid:initial\ndestination:/joal/initialize-me\n\n");
            assertThat(stomp.next()).startsWith("MESSAGE").contains("GLOBAL_SEED_STARTED", "utorrent-3.5.0_43916.client");
            final ObjectNode saved = (ObjectNode) json.readTree(conf.resolve("config.json").toFile());
            saved.put("minUploadRate", 31);
            stomp.send("SEND\ndestination:/joal/config/save\ncontent-type:application/json\n\n" + saved);
            await(() -> {
                try { return json.readTree(conf.resolve("config.json").toFile()).path("minUploadRate").asInt() == 31; }
                catch (IOException e) { return false; }
            }, app);
            Files.write(conf.resolve("torrents/local.torrent"), torrent());
            assertThat(announces.poll(20, TimeUnit.SECONDS)).as(app.log()).contains("event=started", "peer_id=", "key=");
            Files.delete(conf.resolve("torrents/local.torrent"));
            await(() -> app.log().contains("hot deleted file"), app);
        }
        app.close();
        assertThat(app.log()).doesNotContain("MissingReflectionRegistrationError", "Unrecognized format specifier", "Failed to announce");
        // Same artifact, different directory, port, prefix, and token; the first runtime values must not be frozen.
        final int secondPort = availablePort();
        final App second = start(config("web2", "qbittorrent-4.6.7.client"), secondPort, true, "Changed456", "second-token");
        second.ready();
        assertThat(get(secondPort, "/Changed456/ui/").statusCode()).isEqualTo(200);
        assertThat(get(secondPort, "/Native123/ui/").statusCode()).isEqualTo(404);
        try (Stomp stomp = connect(secondPort, "Changed456", "second-token")) {
            assertThat(stomp.next()).startsWith("CONNECTED");
        }
    }

    @Test
    void allBundledClientFilesDeserializeInExecutable() throws Exception {
        int count = 0;
        try (var clients = Files.list(Path.of("resources/clients"))) {
            for (Path client : clients.filter(p -> p.toString().endsWith(".client")).sorted().toList()) {
                final App app = start(config("client" + apps.size(), client.getFileName().toString()), availablePort(), false, "", "");
                app.ready();
                app.close();
                count++;
            }
        }
        // Bundled clients cover every algorithm, but omit the ALWAYS and TIMED refresh policies.
        for (String refresh : List.of("ALWAYS", "TIMED")) {
            final Path conf = config("policy" + refresh, "native-1.0.client");
            final ObjectNode client = (ObjectNode) json.readTree(Path.of("resources/clients/utorrent-3.5.0_43916.client").toFile());
            for (String generator : List.of("keyGenerator", "peerIdGenerator")) {
                final ObjectNode policy = (ObjectNode) client.get(generator);
                policy.put("refreshOn", refresh);
                if (refresh.equals("TIMED")) policy.put("refreshEvery", 10);
                else policy.remove("refreshEvery");
            }
            json.writeValue(conf.resolve("clients/native-1.0.client").toFile(), client);
            final App app = start(conf, availablePort(), false, "", "");
            app.ready();
            app.close();
        }
        System.out.println("Executable validated " + count + " bundled client files and ALWAYS/TIMED refresh policies");
    }

    @Test
    void representativeAlgorithmsGenerateTrackerRequests() throws Exception {
        // These four definitions cover both peer-id algorithms and all four key algorithms.
        for (String client : List.of("bittorrent-7.10.1_43917.client", "deluge-1.3.13.client",
                "transmission-2.82_14160.client", "vuze-5.7.5.0.client")) {
            announces.clear();
            final Path conf = config("algorithm" + apps.size(), client);
            Files.write(conf.resolve("torrents/local.torrent"), torrent());
            final App app = start(conf, availablePort(), false, "", "");
            app.ready();
            assertThat(announces.poll(20, TimeUnit.SECONDS)).as(app.log()).contains("info_hash=", "peer_id=");
            await(() -> app.log().contains("has announced successfully"), app);
            app.close();
        }
    }

    @Test
    void httpsTrackerWorksWithRuntimeTrustStore() throws Exception {
        try (LocalTlsTracker tls = new LocalTlsTracker(temp)) {
            final Path conf = config("tls", "utorrent-3.5.0_43916.client");
            Files.write(conf.resolve("torrents/local.torrent"), torrent(tls.url()));
            final App app = start(conf, availablePort(), false, "", "",
                    "-Dhttp.proxyPort=" + tls.proxyPort(), "-Djavax.net.ssl.trustStore=" + tls.trustStore,
                    "-Djavax.net.ssl.trustStorePassword=changeit", "-Djavax.net.ssl.trustStoreType=PKCS12");
            try {
                app.ready();
                await(() -> tls.requests.get() > 0 && app.log().contains("has announced successfully"), app);
            } finally {
                app.close();
            }
        }
    }

    @Test
    void invalidInputsExitNonzeroWithoutLeakingProcess() throws Exception {
        final Path missing = config("missing", "utorrent-3.5.0_43916.client");
        Files.delete(missing.resolve("config.json"));
        fails(start(missing, availablePort(), false, "", ""), "not found");
        final Path broken = config("broken", "utorrent-3.5.0_43916.client");
        Files.writeString(broken.resolve("config.json"), "{broken");
        fails(start(broken, availablePort(), false, "", ""), "Failed to read configuration");
        final Path badClient = config("badClient", "not-present.client");
        fails(start(badClient, availablePort(), false, "", ""), "not found");
        fails(start(config("prefix", "utorrent-3.5.0_43916.client"), availablePort(), true, "a/b", "token"), "joal.ui.path.prefix");
        fails(start(config("token", "utorrent-3.5.0_43916.client"), availablePort(), true, "valid", ""), "joal.ui.secret-token");
    }

    private Path config(String name, String client) throws IOException {
        final Path conf = Files.createDirectory(temp.resolve(name));
        Files.createDirectories(conf.resolve("torrents"));
        Files.createDirectories(conf.resolve("clients"));
        try (var clients = Files.list(Path.of("resources/clients"))) {
            for (Path source : clients.filter(p -> p.toString().endsWith(".client")).toList()) {
                Files.copy(source, conf.resolve("clients").resolve(source.getFileName()));
            }
        }
        final ObjectNode settings = (ObjectNode) json.readTree(Path.of("resources/config.json").toFile());
        settings.put("client", client);
        json.writeValue(conf.resolve("config.json").toFile(), settings);
        return conf;
    }

    private App start(Path conf, int port, boolean ui, String prefix, String token, String... systemProperties) throws IOException {
        final Path artifact = Path.of(System.getProperty("joal.test.application")).toAbsolutePath();
        final List<String> command = new ArrayList<>();
        final boolean jar = artifact.toString().endsWith(".jar");
        if (jar) command.add(Path.of(System.getProperty("java.home"), "bin/java").toString());
        else command.add(artifact.toString());
        command.add("-Dhttp.proxyHost=127.0.0.1");
        command.add("-Dhttp.proxyPort=" + proxy.getAddress().getPort());
        command.addAll(List.of(systemProperties));
        if (jar) { command.add("-jar"); command.add(artifact.toString()); }
        if (ui) command.add("--spring.main.web-environment=true");
        command.addAll(List.of("--joal-conf=" + conf, "--server.address=127.0.0.1", "--server.port=" + port,
                "--joal.ui.path.prefix=" + prefix,
                "--joal.ui.secret-token=" + token));
        final Path log = temp.resolve("process-" + apps.size() + ".log");
        final ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile());
        for (String variable : List.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "SPRING_PROFILES_ACTIVE")) {
            builder.environment().remove(variable);
        }
        final App app = new App(builder.start(), log);
        apps.add(app);
        return app;
    }

    private void fails(App app, String error) throws Exception {
        assertThat(app.process.waitFor(20, TimeUnit.SECONDS)).as(app.log()).isTrue();
        assertThat(app.process.exitValue()).as(app.log()).isNotZero();
        assertThat(app.log()).contains(error);
    }

    private HttpResponse<String> get(int port, String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) { return socket.getLocalPort(); }
    }

    private static void await(BooleanSupplier condition, App app) throws Exception {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!condition.getAsBoolean() && app.process.isAlive() && System.nanoTime() < deadline) Thread.sleep(50);
        assertThat(condition.getAsBoolean()).as(app.log()).isTrue();
    }

    private byte[] torrent() {
        return torrent("http://127.0.0.1:" + proxy.getAddress().getPort() + "/announce");
    }

    private byte[] torrent(String tracker) {
        return ("d8:announce" + tracker.length() + ":" + tracker
                + "4:infod6:lengthi1e4:name9:local.txt12:piece lengthi16384e6:pieces20:00000000000000000000ee")
                .getBytes(StandardCharsets.US_ASCII);
    }

    private Stomp connect(int port, String prefix, String token) throws Exception {
        final Stomp stomp = new Stomp();
        stomp.socket = http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(5))
                .buildAsync(URI.create("ws://127.0.0.1:" + port + "/" + prefix), stomp).get(5, TimeUnit.SECONDS);
        stomp.send("CONNECT\naccept-version:1.2\nhost:localhost\nX-Joal-Username:test\nX-Joal-Auth-Token:" + token + "\n\n");
        return stomp;
    }

    private static class Stomp implements WebSocket.Listener, AutoCloseable {
        private final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        private final StringBuilder buffer = new StringBuilder();
        private WebSocket socket;
        @Override public void onOpen(WebSocket socket) { socket.request(1); }
        @Override public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
            buffer.append(data);
            int end;
            while ((end = buffer.indexOf("\0")) >= 0) {
                frames.add(buffer.substring(0, end).stripLeading());
                buffer.delete(0, end + 1);
            }
            socket.request(1);
            return null;
        }
        void send(String frame) { socket.sendText(frame + "\0", true).join(); }
        String next() throws InterruptedException {
            String frame = frames.poll(10, TimeUnit.SECONDS);
            assertThat(frame).as("STOMP response").isNotNull();
            return frame;
        }
        @Override public void close() { if (socket != null) socket.abort(); }
    }

    private static class App implements AutoCloseable {
        private final Process process;
        private final Path output;
        App(Process process, Path output) { this.process = process; this.output = output; }
        String log() {
            try { return Files.readString(output); }
            catch (IOException e) { throw new IllegalStateException(e); }
        }
        void ready() throws Exception { await(() -> log().contains("JOAL is ready"), this); }
        @Override public void close() throws Exception {
            if (!process.isAlive()) return;
            process.destroy();
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                fail("Process did not terminate gracefully: " + log());
            }
        }
    }
}
