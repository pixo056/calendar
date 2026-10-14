package ru.top105.calendar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:api-tests;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "calendar.group-links-file=${java.io.tmpdir}/calendar-api-${random.uuid}.properties"
})
class EventApiTests {
    @LocalServerPort int port;
    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private String payload(String title) {
        return """
                {"title":"%s","subject":"Java","type":"TEST","date":"2026-10-20",
                 "time":"10:30","location":"101","description":"Массивы"}
                """.formatted(title);
    }

    @Test void createsListsUpdatesAndDeletesPersistentEvents() throws Exception {
        var created = request("POST", "/api/events", payload("Контрольная"));
        assertEquals(201, created.statusCode(), created.body());
        assertTrue(created.body().contains("\"date\":\"2026-10-20\""), created.body());
        assertTrue(created.body().contains("\"time\":\"10:30:00\""), created.body());
        var matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(created.body());
        assertTrue(matcher.find(), created.body());
        String path = "/api/events/" + matcher.group(1);
        assertTrue(request("GET", "/api/events", null).body().contains("Контрольная"));
        var updated = request("PUT", path, payload("Новая тема"));
        assertEquals(200, updated.statusCode());
        assertTrue(updated.body().contains("Новая тема"));
        assertEquals(204, request("DELETE", path, null).statusCode());
        assertFalse(request("GET", "/api/events", null).body().contains("Новая тема"));
        assertEquals(404, request("PUT", path, payload("Нет события")).statusCode());
        assertEquals(404, request("DELETE", path, null).statusCode());
    }

    @Test void validatesFieldsAndRejectsInvalidDatesAndTypes() throws Exception {
        assertEquals(400, request("POST", "/api/events", payload(" ")).statusCode());
        assertEquals(400, request("POST", "/api/events", payload("x".repeat(121))).statusCode());
        assertEquals(400, request("POST", "/api/events", payload("Тест").replace("TEST", "OTHER")).statusCode());
        assertEquals(400, request("POST", "/api/events", payload("Тест").replace("2026-10-20", "2026-99-99")).statusCode());
        assertEquals(400, request("POST", "/api/events", payload("Тест").replace("2026-10-20", "0001-01-01")).statusCode());
        assertEquals(400, request("POST", "/api/events", "{}").statusCode());
    }

    @Test void acceptsAllEventTypesAndOptionalTime() throws Exception {
        for (String type : new String[]{"TEST", "CREDIT", "EXAM", "ACTIVITY", "DEADLINE"}) {
            var result = request("POST", "/api/events", payload("Тип " + type).replace("\"TEST\"", "\"" + type + "\"").replace("\"10:30\"", "null"));
            assertEquals(201, result.statusCode(), result.body());
        }
    }

    @Test void servesRussianWebInterface() throws Exception {
        var page = request("GET", "/", null);
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("Студенческий календарь"));
        assertEquals(200, request("GET", "/app.js", null).statusCode());
        assertEquals(200, request("GET", "/styles.css", null).statusCode());
        assertEquals(200, request("GET", "/responsive.css", null).statusCode());
        assertFalse(page.body().contains("Локальная версия"));
    }

    @Test void separatesGroupsAndPreventsEditingThroughAnotherGroup() throws Exception {
        String second = "?group=" + java.net.URLEncoder.encode("ТОП-106Б", java.nio.charset.StandardCharsets.UTF_8);
        var created = request("POST", "/api/events" + second, payload("Только для второй группы"));
        assertEquals(201, created.statusCode(), created.body());
        var matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(created.body());
        assertTrue(matcher.find());
        String path = "/api/events/" + matcher.group(1);
        assertFalse(request("GET", "/api/events", null).body().contains("Только для второй группы"));
        assertTrue(request("GET", "/api/events" + second, null).body().contains("Только для второй группы"));
        assertEquals(404, request("PUT", path, payload("Попытка изменить чужое событие")).statusCode());
        assertEquals(404, request("DELETE", path, null).statusCode());
        assertEquals(200, request("PUT", path + second, payload("Изменено во второй группе")).statusCode());
        assertEquals(204, request("DELETE", path + second, null).statusCode());
    }

    @Test void exposesOfficialCatalogAndRejectsUnknownGroups() throws Exception {
        var catalog = request("GET", "/api/groups", null);
        assertEquals(200, catalog.statusCode());
        assertTrue(catalog.body().contains("ТОП-105Б"));
        assertTrue(catalog.body().contains("ТОП-106Б"));
        assertTrue(catalog.body().contains("https://isu.uust.ru/class_schedule/"));
        assertEquals(400, request("GET", "/api/events?group=unknown", null).statusCode());
        assertEquals(400, request("POST", "/api/events?group=unknown", payload("Неверная группа")).statusCode());
    }
}
