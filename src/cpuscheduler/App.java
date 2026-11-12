package cpuscheduler;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * CPU SCHEDULING VISUALIZER - Java backend (JDK only, no external libraries).
 *
 * Routes
 *   GET  /               the web interface (templates/index.html, served unchanged)
 *   POST /               same page (kept for compatibility with the old Flask route)
 *   GET  /static/...     CSS / JS / images from the static folder
 *   GET  /health         JSON status
 *   POST /api/schedule   JSON simulation API (form fields: algorithm, action,
 *                        process_count, quantum, pid, arrival, burst, priority)
 *
 * Run from the project folder (the one containing templates/ and static/).
 */
public final class App {

    static final String HOST = "127.0.0.1";
    static final String VERSION = "2.0";

    private static final Path BASE = Paths.get(System.getProperty("app.dir", ".")).toAbsolutePath().normalize();
    private static final Path INDEX = BASE.resolve("templates").resolve("index.html");
    private static final Path STATIC = BASE.resolve("static");

    private static final String[] EDGE_PATHS = {
            "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
            "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
    };

    private App() {
    }

    // ============================================================
    // Entry point
    // ============================================================

    public static void main(String[] args) throws IOException {

        int port = 5000;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isBlank()) {
            try {
                port = Integer.parseInt(envPort.strip());
            } catch (NumberFormatException e) {
                System.err.println("Ignoring invalid PORT value: " + envPort);
            }
        }

        boolean openBrowser = System.getenv("NO_BROWSER") == null;
        for (String arg : args) {
            if (arg.equals("--no-browser")) {
                openBrowser = false;
            }
        }

        if (!Files.isRegularFile(INDEX)) {
            System.err.println("Cannot find " + INDEX);
            System.err.println("Start the app from the project folder, or pass -Dapp.dir=<project folder>.");
            System.exit(1);
        }

        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getByName(HOST), port), 0);
        } catch (java.net.BindException e) {
            System.err.println("Port " + port + " is already in use. Close the other program or set PORT=<other port>.");
            System.exit(1);
            return;
        }

        server.createContext("/", App::handleIndex);
        server.createContext("/static/", App::handleStatic);
        server.createContext("/health", App::handleHealth);
        server.createContext("/api/schedule", App::handleSchedule);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        String url = "http://" + HOST + ":" + port + "/";
        System.out.println(" * CPU Scheduling Visualizer running on " + url);
        System.out.println(" * Press Ctrl+C to stop");

        if (openBrowser) {
            Timer timer = new Timer("open-browser", true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    openBrowser(url);
                }
            }, 1000);
        }
    }

    /** Opens the app in Microsoft Edge, or the default browser if Edge is not found. */
    private static void openBrowser(String url) {
        try {
            for (String path : EDGE_PATHS) {
                if (Files.exists(Paths.get(path))) {
                    new ProcessBuilder(path, url).start();
                    return;
                }
            }
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception | java.lang.Error e) {
            System.out.println(" * Could not open a browser automatically. Open " + url + " yourself.");
        }
    }

    // ============================================================
    // Handlers
    // ============================================================

    /** "/" - the main page. Anything other than exactly "/" is a 404. */
    private static void handleIndex(HttpExchange ex) throws IOException {
        try (ex) {
            if (!ex.getRequestURI().getPath().equals("/")) {
                send(ex, 404, "text/plain; charset=utf-8", "Not Found");
                return;
            }
            String method = ex.getRequestMethod();
            if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
                methodNotAllowed(ex, "GET, HEAD, POST");
                return;
            }
            drain(ex);
            send(ex, 200, "text/html; charset=utf-8", Files.readAllBytes(INDEX));
        }
    }

    /** "/static/..." - files from the static folder (path traversal is blocked). */
    private static void handleStatic(HttpExchange ex) throws IOException {
        try (ex) {
            String method = ex.getRequestMethod();
            if (!method.equals("GET") && !method.equals("HEAD")) {
                methodNotAllowed(ex, "GET, HEAD");
                return;
            }

            String rel = URLDecoder.decode(ex.getRequestURI().getRawPath().substring("/static/".length()),
                    StandardCharsets.UTF_8);
            Path file = STATIC.resolve(rel).normalize();

            if (!file.startsWith(STATIC) || !Files.isRegularFile(file)) {
                send(ex, 404, "text/plain; charset=utf-8", "Not Found");
                return;
            }
            send(ex, 200, contentType(file), Files.readAllBytes(file));
        }
    }

    /** "/health" - JSON status. */
    private static void handleHealth(HttpExchange ex) throws IOException {
        try (ex) {
            String method = ex.getRequestMethod();
            if (!method.equals("GET") && !method.equals("HEAD")) {
                methodNotAllowed(ex, "GET, HEAD");
                return;
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "running");
            body.put("application", "CPU Scheduling Visualizer");
            body.put("algorithms", Scheduler.ALGORITHMS);
            body.put("version", VERSION);
            send(ex, 200, "application/json", Json.write(body));
        }
    }

    /** "/api/schedule" - run a simulation from form-encoded input, reply with JSON. */
    private static void handleSchedule(HttpExchange ex) throws IOException {
        try (ex) {
            if (!ex.getRequestMethod().equals("POST")) {
                methodNotAllowed(ex, "POST");
                return;
            }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, List<String>> form = parseForm(body);
            send(ex, 200, "application/json", Json.write(ScheduleService.run(form)));
        }
    }

    // ============================================================
    // Helpers
    // ============================================================

    static Map<String, List<String>> parseForm(String body) {
        Map<String, List<String>> form = new LinkedHashMap<>();
        if (body == null || body.isEmpty()) {
            return form;
        }
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
            String val = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            form.computeIfAbsent(key, k -> new ArrayList<>()).add(val);
        }
        return form;
    }

    private static void drain(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            in.readAllBytes();
        }
    }

    private static void methodNotAllowed(HttpExchange ex, String allow) throws IOException {
        ex.getResponseHeaders().set("Allow", allow);
        send(ex, 405, "text/plain; charset=utf-8", "Method Not Allowed");
    }

    private static void send(HttpExchange ex, int status, String type, String body) throws IOException {
        send(ex, status, type, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        if (ex.getRequestMethod().equals("HEAD")) {
            ex.getResponseHeaders().set("Content-Length", String.valueOf(body.length));
            ex.sendResponseHeaders(status, -1);
            return;
        }
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1);
        return switch (ext) {
            case "css" -> "text/css; charset=utf-8";
            case "js", "mjs" -> "text/javascript; charset=utf-8";
            case "json" -> "application/json";
            case "html", "htm" -> "text/html; charset=utf-8";
            case "svg" -> "image/svg+xml";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "ico" -> "image/x-icon";
            case "webp" -> "image/webp";
            case "woff" -> "font/woff";
            case "woff2" -> "font/woff2";
            case "ttf" -> "font/ttf";
            case "txt" -> "text/plain; charset=utf-8";
            default -> "application/octet-stream";
        };
    }
}
