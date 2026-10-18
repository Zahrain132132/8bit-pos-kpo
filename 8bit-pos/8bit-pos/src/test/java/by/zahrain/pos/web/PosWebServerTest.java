package by.zahrain.pos.web;

import by.zahrain.pos.database.DatabaseManager;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PosWebServerTest {
    @TempDir Path tempDir;
    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(int port, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(int port, String path, String form) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void servesHtmlAndProductApi() throws Exception {
        DatabaseManager db = new DatabaseManager(tempDir.resolve("web.db").toString());
        HttpServer server = new PosWebServer(db).start(0);
        try {
            int port = server.getAddress().getPort();
            HttpResponse<String> page = get(port, "/");
            assertEquals(200, page.statusCode());
            assertTrue(page.body().contains("8BIT POS"));
            HttpResponse<String> products = get(port, "/api/products");
            assertEquals(200, products.statusCode());
            assertTrue(products.body().contains("10000001"));
        } finally { server.stop(0); }
    }

    @Test
    void canAddCheckoutAndPersistUpdatedStock() throws Exception {
        DatabaseManager db = new DatabaseManager(tempDir.resolve("web.db").toString());
        HttpServer server = new PosWebServer(db).start(0);
        try {
            int port = server.getAddress().getPort();
            assertEquals(200, post(port, "/api/cart/add", "barcode=10000001&quantity=2").statusCode());
            assertTrue(get(port, "/api/cart").body().contains("\"count\":2"));
            HttpResponse<String> purchase = post(port, "/api/checkout", "");
            assertEquals(200, purchase.statusCode());
            assertTrue(purchase.body().contains("\"total\":15.80"));
            assertEquals(22, new by.zahrain.pos.database.ProductRepository(db)
                    .findByBarcode("10000001").getQuantity());
            assertTrue(get(port, "/api/cart").body().contains("\"count\":0"));
        } finally { server.stop(0); }
    }

    @Test
    void invalidBarcodeAndOversellDoNotChangeStock() throws Exception {
        DatabaseManager db = new DatabaseManager(tempDir.resolve("web.db").toString());
        HttpServer server = new PosWebServer(db).start(0);
        try {
            int port = server.getAddress().getPort();
            assertEquals(400, post(port, "/api/cart/add", "barcode=unknown&quantity=1").statusCode());
            assertEquals(409, post(port, "/api/cart/add", "barcode=10000001&quantity=999").statusCode());
            assertEquals(24, new by.zahrain.pos.database.ProductRepository(db)
                    .findByBarcode("10000001").getQuantity());
        } finally { server.stop(0); }
    }
}
