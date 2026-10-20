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


@Test
void canSetCartQuantityAndRemoveWithZero() throws Exception {
    DatabaseManager db = new DatabaseManager(
            tempDir.resolve("set-cart.db").toString());
    HttpServer server = new PosWebServer(db).start(0);

    try {
        int port = server.getAddress().getPort();

        assertEquals(200, post(port, "/api/cart/add",
                "barcode=10000001&quantity=2").statusCode());

        HttpResponse<String> updated = post(port, "/api/cart/set",
                "barcode=10000001&quantity=4");

        assertEquals(200, updated.statusCode());
        assertTrue(updated.body().contains("\"count\":4"));
        assertTrue(updated.body().contains("\"cartQuantity\":4"));

        HttpResponse<String> removed = post(port, "/api/cart/set",
                "barcode=10000001&quantity=0");

        assertEquals(200, removed.statusCode());
        assertTrue(removed.body().contains("\"count\":0"));

    } finally {
        server.stop(0);
    }
}

@Test
void canRemoveOneItemWithoutRemovingOthers() throws Exception {
    DatabaseManager db = new DatabaseManager(
            tempDir.resolve("remove-cart.db").toString());
    HttpServer server = new PosWebServer(db).start(0);

    try {
        int port = server.getAddress().getPort();

        post(port, "/api/cart/add", "barcode=10000001&quantity=1");
        post(port, "/api/cart/add", "barcode=10000002&quantity=2");

        HttpResponse<String> response = post(port, "/api/cart/remove",
                "barcode=10000001");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"count\":2"));
        assertFalse(response.body().contains("\"barcode\":\"10000001\""));
        assertTrue(response.body().contains("\"barcode\":\"10000002\""));

    } finally {
        server.stop(0);
    }
}

@Test
void canClearCartWithoutChangingStock() throws Exception {
    DatabaseManager db = new DatabaseManager(
            tempDir.resolve("clear-cart.db").toString());
    HttpServer server = new PosWebServer(db).start(0);

    try {
        int port = server.getAddress().getPort();

        post(port, "/api/cart/add", "barcode=10000001&quantity=3");

        HttpResponse<String> cleared =
                post(port, "/api/cart/clear", "");

        assertEquals(200, cleared.statusCode());
        assertTrue(cleared.body().contains("\"count\":0"));

        assertEquals(24,
                new by.zahrain.pos.database.ProductRepository(db)
                        .findByBarcode("10000001").getQuantity());

    } finally {
        server.stop(0);
    }
}

@Test
void rejectsInvalidCartRequests() throws Exception {
    DatabaseManager db = new DatabaseManager(
            tempDir.resolve("invalid-cart.db").toString());
    HttpServer server = new PosWebServer(db).start(0);

    try {
        int port = server.getAddress().getPort();

        assertEquals(400, post(port, "/api/cart/set",
                "barcode=10000001&quantity=-1").statusCode());

        assertEquals(409, post(port, "/api/cart/set",
                "barcode=10000001&quantity=999").statusCode());

        assertEquals(400, post(port, "/api/cart/remove",
                "").statusCode());

        assertEquals(400, post(port, "/api/cart/add",
                "barcode=10000001&quantity=abc").statusCode());

        assertTrue(get(port, "/api/cart").body()
                .contains("\"count\":0"));

    } finally {
        server.stop(0);
    }
}

@Test
void servesStaticAssetsAndRejectsUnknownRoutes() throws Exception {
    DatabaseManager db = new DatabaseManager(
            tempDir.resolve("assets.db").toString());
    HttpServer server = new PosWebServer(db).start(0);

    try {
        int port = server.getAddress().getPort();

        assertEquals(200, get(port, "/styles.css").statusCode());
        assertEquals(200, get(port, "/app.js").statusCode());
        assertEquals(204, get(port, "/favicon.ico").statusCode());

        HttpResponse<String> missing =
                get(port, "/api/unknown");

        assertEquals(404, missing.statusCode());
        assertTrue(missing.body().contains("Адрес не найден"));

    } finally {
        server.stop(0);
    }
}

}
