package by.zahrain.pos.web;

import by.zahrain.pos.database.DatabaseManager;
import by.zahrain.pos.database.ProductRepository;
import by.zahrain.pos.database.SaleRepository;
import by.zahrain.pos.model.Cart;
import by.zahrain.pos.model.Product;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Small single-register HTTP server, intended for local coursework/demo use.
 * The cart is shared between visitors: do not expose this demo publicly.
 */
public final class PosWebServer {
    private static final Logger LOG = Logger.getLogger(PosWebServer.class.getName());
    private final ProductRepository products;
    private final SaleRepository sales;
    private final Map<String, Integer> quantities = new LinkedHashMap<>();

    public PosWebServer(DatabaseManager databaseManager) {
        databaseManager.initializeDatabase();
        this.products = new ProductRepository(databaseManager);
        this.sales = new SaleRepository(databaseManager);
        seedProductsIfEmpty();
    }

    public static void main(String[] args) throws IOException {
        Path data = Path.of("data");
        Files.createDirectories(data);
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = new PosWebServer(new DatabaseManager(data.resolve("pos.db").toString()))
                .start(port);
        System.out.println("[8BIT POS] Открывай http://localhost:" + server.getAddress().getPort());
        System.out.println("[8BIT POS] Codespaces → вкладка Ports → открыть порт " + port);
    }

    public HttpServer start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        return server;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if (method.equals("GET") && path.equals("/")) {
                staticFile(exchange, "index.html", "text/html; charset=utf-8");
            } else if (method.equals("GET") && path.equals("/styles.css")) {
                staticFile(exchange, "styles.css", "text/css; charset=utf-8");
            } else if (method.equals("GET") && path.equals("/app.js")) {
                staticFile(exchange, "app.js", "text/javascript; charset=utf-8");
            } else if (method.equals("GET") && path.equals("/favicon.ico")) {
                send(exchange, 204, "", "text/plain; charset=utf-8");
            } else if (method.equals("GET") && path.equals("/api/products")) {
                api(exchange, 200, productsJson());
            } else if (method.equals("GET") && path.equals("/api/cart")) {
                api(exchange, 200, cartJson());
            } else if (method.equals("POST") && path.equals("/api/cart/add")) {
                Map<String, String> data = form(exchange);
                add(data.get("barcode"), parseQuantity(data.get("quantity")));
                api(exchange, 200, cartJson());
            } else if (method.equals("POST") && path.equals("/api/cart/set")) {
                Map<String, String> data = form(exchange);
                set(data.get("barcode"), parseQuantity(data.get("quantity")));
                api(exchange, 200, cartJson());
            } else if (method.equals("POST") && path.equals("/api/cart/remove")) {
                remove(form(exchange).get("barcode"));
                api(exchange, 200, cartJson());
            } else if (method.equals("POST") && path.equals("/api/cart/clear")) {
                clear();
                api(exchange, 200, cartJson());
            } else if (method.equals("POST") && path.equals("/api/checkout")) {
                api(exchange, 200, checkoutJson());
            } else {
                api(exchange, 404, "{\"error\":\"Адрес не найден\"}");
            }
        } catch (IllegalArgumentException error) {
            api(exchange, 400, "{\"error\":" + quote(error.getMessage()) + "}");
        } catch (IllegalStateException error) {
            api(exchange, 409, "{\"error\":" + quote(error.getMessage()) + "}");
        } catch (Exception error) {
            LOG.log(Level.SEVERE, "HTTP request failed", error);
            api(exchange, 500, "{\"error\":\"Ошибка сервера. Проверь журнал приложения.\"}");
        } finally {
            exchange.close();
        }
    }

    private synchronized void add(String barcode, int amount) {
        Product product = requireProduct(barcode);
        if (amount < 1) throw new IllegalArgumentException("Количество должно быть положительным");
        long requested = (long) quantities.getOrDefault(barcode, 0) + amount;
        if (requested > product.getQuantity()) throw new IllegalStateException("Недостаточно на складе: " + product.getName());
        if (requested > Integer.MAX_VALUE) throw new IllegalArgumentException("Слишком большое количество");
        quantities.put(barcode, (int) requested);
    }

    private synchronized void set(String barcode, int amount) {
        if (amount < 0) throw new IllegalArgumentException("Количество не может быть отрицательным");
        if (amount == 0) {
            quantities.remove(barcode);
            return;
        }
        Product product = requireProduct(barcode);
        if (amount > product.getQuantity()) throw new IllegalStateException("Недостаточно на складе: " + product.getName());
        quantities.put(barcode, amount);
    }

    private synchronized void remove(String barcode) {
        if (barcode == null || barcode.isBlank()) throw new IllegalArgumentException("Не указан штрихкод");
        quantities.remove(barcode);
    }

    private synchronized void clear() {
        quantities.clear();
    }

    private synchronized String cartJson() {
        Cart cart = new Cart();
        StringBuilder lines = new StringBuilder("[");
        int count = 0;
        for (Map.Entry<String, Integer> line : quantities.entrySet()) {
            Product product = products.findByBarcode(line.getKey());
            if (product == null) continue;
            cart.addProduct(product, line.getValue());
            if (lines.length() > 1) lines.append(',');
            lines.append(productJson(product).replaceFirst("}$", ",\"cartQuantity\":" + line.getValue()
                    + ",\"subtotal\":" + money(product.getPrice() * line.getValue()) + "}"));
            count += line.getValue();
        }
        lines.append(']');
        return "{\"items\":" + lines + ",\"count\":" + count + ",\"total\":" + money(cart.getTotal()) + "}";
    }

    private synchronized String checkoutJson() {
        BigDecimal total = sales.checkout(Map.copyOf(quantities));
        int count = quantities.values().stream().mapToInt(Integer::intValue).sum();
        quantities.clear();
        return "{\"ok\":true,\"total\":" + total.toPlainString() + ",\"count\":" + count
                + ",\"message\":\"Покупка успешно оформлена\"}";
    }

    private String productsJson() {
        List<Product> rows = products.findAll();
        StringBuilder out = new StringBuilder("[");
        for (Product product : rows) {
            if (out.length() > 1) out.append(',');
            out.append(productJson(product));
        }
        return out.append(']').toString();
    }

    private Product requireProduct(String barcode) {
        if (barcode == null || barcode.isBlank()) throw new IllegalArgumentException("Не указан штрихкод");
        Product product = products.findByBarcode(barcode);
        if (product == null) throw new IllegalArgumentException("Товар не найден: " + barcode);
        return product;
    }

    private void seedProductsIfEmpty() {
        if (!products.findAll().isEmpty()) return;
        Product[] demo = {
                new Product("10000001", "Капучино", 7.90, 24),
                new Product("10000002", "Эспрессо", 4.50, 40),
                new Product("10000003", "Матча латте", 9.40, 14),
                new Product("10000004", "Круассан", 6.20, 18),
                new Product("10000005", "Шоколад 70%", 5.80, 30),
                new Product("10000006", "Минеральная вода", 2.70, 48),
                new Product("10000007", "Печенье", 3.40, 26),
                new Product("10000008", "Сэндвич", 11.50, 12)
        };
        for (Product product : demo) products.save(product);
    }

    private static int parseQuantity(String quantity) {
        if (quantity == null || !quantity.matches("-?[0-9]{1,8}")) {
            throw new IllegalArgumentException("Укажи целое количество товара");
        }
        return Integer.parseInt(quantity);
    }

    private static Map<String, String> form(HttpExchange exchange) throws IOException {
        String type = exchange.getRequestHeaders().getFirst("Content-Type");
        if (type == null || !type.toLowerCase().startsWith("application/x-www-form-urlencoded")) {
            throw new IllegalArgumentException("Ожидается HTML-форма");
        }
        byte[] bytes;
        try (InputStream input = exchange.getRequestBody()) {
            bytes = input.readNBytes(4097);
        }
        if (bytes.length > 4096) throw new IllegalArgumentException("Слишком большой запрос");
        Map<String, String> result = new LinkedHashMap<>();
        String body = new String(bytes, StandardCharsets.UTF_8);
        for (String part : body.split("&")) {
            if (part.isEmpty()) continue;
            String[] kv = part.split("=", 2);
            result.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(kv.length > 1 ? kv[1] : "", StandardCharsets.UTF_8));
        }
        return result;
    }

    private static String productJson(Product p) {
        return "{\"barcode\":" + quote(p.getBarcode()) + ",\"name\":" + quote(p.getName())
                + ",\"price\":" + money(p.getPrice()) + ",\"quantity\":" + p.getQuantity() + "}";
    }

    private static String money(double number) {
        return BigDecimal.valueOf(number).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String quote(String value) {
        if (value == null) return "\"\"";
        StringBuilder out = new StringBuilder("\"");
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 0x20) out.append(String.format("\\u%04x", (int) ch));
                    else out.append(ch);
                }
            }
        }
        return out.append('"').toString();
    }

    private static void api(HttpExchange exchange, int status, String json) throws IOException {
        send(exchange, status, json, "application/json; charset=utf-8");
    }

    private static void staticFile(HttpExchange exchange, String file, String mime) throws IOException {
        try (InputStream stream = PosWebServer.class.getResourceAsStream("/public/" + file)) {
            if (stream == null) {
                api(exchange, 404, "{\"error\":\"Файл не найден\"}");
                return;
            }
            byte[] content = stream.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
        }
    }

    private static void send(HttpExchange exchange, int status, String content, String mime) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) exchange.getResponseBody().write(bytes);
    }
}
