package com.lhs.service.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lhs.common.config.OAuth2Properties;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.exception.TclApiException;
import com.lhs.entity.dto.user.PatIssueRequestDTO;
import com.lhs.entity.dto.user.PatPayloadDTO;
import com.lhs.entity.dto.user.TclOperatorListVO;
import com.lhs.service.survey.impl.OperatorDataServiceImpl;
import com.lhs.service.util.CharacterTableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** No Spring application, Redis, database or real HTTP requests are started. */
class PatIntegrationTest {
    private final ObjectMapper json = new ObjectMapper();
    private HttpClient http;
    private HttpResponse<String> response;
    private TclMigrateClient issuer;
    private TclGameDataClient consumer;
    private HttpRequest sent;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() throws Exception {
        OAuth2Properties properties = new OAuth2Properties();
        properties.setBaseUrl("https://tcl.invalid/");
        properties.setClientId("must-not-send-client");
        properties.setClientSecret("must-not-send-secret");
        http = mock(HttpClient.class);
        response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(call -> {
            sent = call.getArgument(0);
            return response;
        });
        issuer = new TclMigrateClient(properties);
        consumer = new TclGameDataClient(properties);
        ReflectionTestUtils.setField(issuer, "httpClient", http);
        ReflectionTestUtils.setField(consumer, "httpClient", http);
    }

    @Test
    void signsPatWithJsonAndServerCredentialWithoutClientSecrets() throws Exception {
        when(response.body()).thenReturn("""
                {"code":200,"msg":"ok","data":{"id":8,"token":"tlc_pat_fixture",
                "expiresIn":315360000,"expireTime":"2036-11-19T00:00:00",
                "payload":{"game_uid":"123","game_name":"明日方舟"},"futureField":true}}
                """);
        var result = issuer.issuePatFromRefresh("server-refresh", request());
        assertEquals("tlc_pat_fixture", result.getToken());
        assertEquals("123", result.getPayload().getGameUid());
        assertEquals("/oauth2/pat", sent.uri().getPath());
        assertNull(sent.uri().getQuery());
        assertEquals("POST", sent.method());
        assertEquals("application/json", sent.headers().firstValue("Content-Type").orElseThrow());
        assertTrue(sent.headers().firstValue("Authorization").isEmpty());
        JsonNode body = json.readTree(bodyText(sent));
        assertEquals("server-refresh", body.path("refresh_token").asText());
        assertEquals(315360000L, body.path("expires_in").asLong());
        assertEquals("ak-data.read", body.path("scope").asText());
        assertEquals("123", body.path("payload").path("game_uid").asText());
        assertFalse(body.has("client_id"));
        assertFalse(body.has("client_secret"));
        assertFalse(body.has("expiresIn"));
    }

    @Test
    void rejectsMalformedOrIncompleteIssueResponsesAndPreservesCredentialErrors() {
        for (String body : List.of("not-json", "null", "{\"code\":200,\"data\":{}}")) {
            when(response.body()).thenReturn(body);
            assertThrows(ServiceException.class, () -> issuer.issuePatFromRefresh("secret", request()));
        }
        when(response.body()).thenReturn("{\"code\":90009,\"msg\":\"refresh invalid\"}");
        assertEquals(90009, assertThrows(TclApiException.class,
                () -> issuer.issuePatFromRefresh("secret", request())).getTclCode());
    }

    @Test
    void readsOnlyPatConsumerWithoutAccountQueryAndAcceptsResponseMetadata() {
        when(response.body()).thenReturn("""
                {"code":200,"data":{"akUid":"123","source":"skland","items":[],"payload":{}}}
                """);
        assertEquals("123", consumer.listPatOperators("tlc_pat_fixture").getAkUid());
        assertEquals("/pat/ak-accounts/operators", sent.uri().getPath());
        assertNull(sent.uri().getQuery());
        assertEquals("GET", sent.method());
        assertEquals("Bearer tlc_pat_fixture", sent.headers().firstValue("Authorization").orElseThrow());
    }

    @Test
    void invalidPatIsNotRefreshedOrRetried() throws Exception {
        when(response.body()).thenReturn("{\"code\":80001,\"msg\":\"invalid\"}");
        assertEquals(80001, assertThrows(TclApiException.class,
                () -> consumer.listPatOperators("expired")).getTclCode());
        verify(http, times(1)).send(any(), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void rejectsBadConsumerProtocolAndHttpFailures() {
        when(response.body()).thenReturn("not-json");
        assertThrows(ServiceException.class, () -> consumer.listPatOperators("fixture"));
        when(response.statusCode()).thenReturn(503);
        assertThrows(ServiceException.class, () -> consumer.listPatOperators("fixture"));
        assertThrows(ServiceException.class, () -> issuer.issuePatFromRefresh("fixture", request()));
    }

    @Test
    void validatesExpiryScopeNameAndUtf8PayloadBeforeUpstream() {
        PatIssueRequestDTO value = request();
        value.setExpiresIn(318038400L);
        assertDoesNotThrow(() -> TclTokenMigrateService.validatePatParams(value));
        value.setExpiresIn(318038401L);
        assertThrows(ServiceException.class, () -> TclTokenMigrateService.validatePatParams(value));
        value.setExpiresIn(0L);
        assertThrows(ServiceException.class, () -> TclTokenMigrateService.validatePatParams(value));
        value.setExpiresIn(1L);
        value.setScope("a".repeat(257));
        assertThrows(ServiceException.class, () -> TclTokenMigrateService.validatePatParams(value));
        value.setScope("a".repeat(256));
        value.setName("n".repeat(51));
        assertThrows(ServiceException.class, () -> TclTokenMigrateService.validatePatParams(value));
        value.setName("n".repeat(50));
        assertDoesNotThrow(() -> TclTokenMigrateService.validatePatParams(value));
        value.getPayload().setSource("中".repeat(1400));
        assertThrows(ServiceException.class, () -> TclTokenMigrateService.validatePatParams(value));
    }

    @Test
    void refusesIssuingAnUnusablePatWithoutTargetAccount() {
        PatIssueRequestDTO value = request();
        value.getPayload().setGameUid(" ");
        assertThrows(ServiceException.class, () -> TclTokenMigrateService.validatePatParams(value));
        value.setPayload(null);
        assertThrows(ServiceException.class, () -> TclTokenMigrateService.validatePatParams(value));
        assertThrows(ServiceException.class, () -> TclTokenMigrateService.validatePatParams(null));
    }

    @Test
    void publicWrapperSupportsBearerAndLegacyRawTokensWithoutForwardingAccountOverrides() {
        TclGameDataClient client = mock(TclGameDataClient.class);
        CharacterTableService table = mock(CharacterTableService.class);
        when(table.getCharacterTable()).thenReturn(Map.of());
        TclOperatorListVO result = new TclOperatorListVO();
        result.setItems(List.of());
        when(client.listPatOperators(any())).thenReturn(result);
        var service = new OperatorDataServiceImpl(client, table);
        for (String header : List.of("tlc_pat_fixture", " Bearer tlc_pat_fixture ", "bearer tlc_pat_fixture")) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", header);
            request.addParameter("akUid", "other-account");
            assertTrue(service.openApiGetOperatorData(request).isEmpty());
        }
        verify(client, times(3)).listPatOperators("tlc_pat_fixture");
        assertThrows(ServiceException.class,
                () -> service.openApiGetOperatorData(new MockHttpServletRequest()));
        verifyNoMoreInteractions(client);
    }

    private static PatIssueRequestDTO request() {
        PatIssueRequestDTO request = new PatIssueRequestDTO();
        request.setExpiresIn(315360000L);
        request.setScope("ak-data.read");
        request.setName("只读 PAT");
        PatPayloadDTO payload = new PatPayloadDTO();
        payload.setGameUid("123");
        payload.setGameName("明日方舟");
        payload.setSource("web");
        request.setPayload(payload);
        return request;
    }

    private static String bodyText(HttpRequest request) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompletableFuture<String> done = new CompletableFuture<>();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer item) {
                byte[] chunk = new byte[item.remaining()];
                item.get(chunk);
                bytes.writeBytes(chunk);
            }
            public void onError(Throwable error) { done.completeExceptionally(error); }
            public void onComplete() { done.complete(bytes.toString(StandardCharsets.UTF_8)); }
        });
        return done.get();
    }
}
