package com.lhs.service.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lhs.common.config.OAuth2Properties;
import com.lhs.common.context.UserContext;
import com.lhs.common.enums.ResultCode;
import com.lhs.common.exception.GlobalExceptionHandler;
import com.lhs.common.exception.ServiceException;
import com.lhs.common.exception.TclApiException;
import com.lhs.common.util.JsonMapper;
import com.lhs.common.util.RedisKeyUtil;
import com.lhs.controller.UserController;
import com.lhs.entity.dto.user.TclTokenVO;
import com.lhs.entity.po.user.OAuthUserInfo;
import com.lhs.interceptor.UserInterceptor;
import com.lhs.mapper.user.OAuthUserInfoMapper;
import com.lhs.mapper.user.UserExternalAccountBindingMapper;
import com.lhs.service.user.impl.UserSessionServiceImpl;
import com.lhs.service.util.TencentCloudService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** 使用真实客户端和服务编排，HTTP、Redis、数据库均为 mock，不启动 Spring 应用。 */
class AccountDeletionTest {
    private static final long UID = 42L;
    private static final String TOKEN = "local-session-token-with-sufficient-length";
    private TclMigrateClient client;
    private TclTokenMigrateService tokens;
    private UserSessionServiceImpl sessions;
    private RedisTemplate<String, String> redis;
    private ValueOperations<String, String> values;
    private OAuthUserInfoMapper users;
    private HttpClient http;
    private HttpResponse<String> response;
    private HttpRequest sent;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() throws Exception {
        OAuth2Properties properties = new OAuth2Properties();
        properties.setBaseUrl("https://tcl.invalid/");
        properties.setClientId("must-not-send-client");
        properties.setClientSecret("must-not-send-secret");
        client = new TclMigrateClient(properties);
        http = mock(HttpClient.class);
        response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"code\":200,\"msg\":\"ok\",\"data\":null}");
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(call -> {
            sent = call.getArgument(0);
            return response;
        });
        ReflectionTestUtils.setField(client, "httpClient", http);
        redis = mock(RedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        TclTokenVO cached = new TclTokenVO();
        cached.setUid(UID);
        cached.setAccessToken("server-access");
        cached.setRefreshToken("server-refresh");
        when(values.get(RedisKeyUtil.tclToken(UID))).thenReturn(JsonMapper.toJSONString(cached));
        when(values.get(RedisKeyUtil.loginToken(TOKEN))).thenReturn(Long.toString(UID));
        tokens = new TclTokenMigrateService(properties, client, redis);
        users = mock(OAuthUserInfoMapper.class);
        OAuthUserInfo user = new OAuthUserInfo();
        user.setId(UID);
        user.setStatus(1);
        when(users.selectById(UID)).thenReturn(user);
        when(users.updateById(any(OAuthUserInfo.class))).thenReturn(1);
        sessions = new UserSessionServiceImpl(users, redis, mock(TencentCloudService.class),
                mock(UserExternalAccountBindingMapper.class), tokens);
        UserContext.set(UID, TOKEN, user);
    }

    @AfterEach
    void cleanup() {
        UserContext.clear();
    }

    @Test
    void sendsCodeUsingOnlyServerRefreshTokenAndKeepsSession() throws Exception {
        tokens.sendAccountDeletionCodeByUid(UID);
        JsonNode body = assertRequest("/oauth2/account/send-delete-code");
        assertEquals(1, body.size());
        verify(redis, never()).delete(anyString());
        verifyNoInteractions(users);
    }

    @Test
    void deletesAccountOnlyAfterUpstreamSuccessAndBlocksOtherSessions() throws Exception {
        sessions.deleteAccount("012345");
        JsonNode body = assertRequest("/oauth2/account/delete");
        assertEquals("012345", body.path("code").asText());
        assertEquals(2, body.size());
        var deleted = ArgumentCaptor.forClass(OAuthUserInfo.class);
        verify(users).updateById(deleted.capture());
        assertEquals(UID, deleted.getValue().getId());
        assertEquals(-1, deleted.getValue().getStatus());
        assertTrue(deleted.getValue().getDeleteFlag());
        verify(redis).delete(RedisKeyUtil.tclToken(UID));
        verify(redis).delete(RedisKeyUtil.loginToken(TOKEN));

        // 即使另一设备的 Redis 登录映射仍存在，也会被资料状态检查拒绝。
        when(users.selectById(UID)).thenReturn(deleted.getValue());
        assertEquals(ResultCode.USER_FORBIDDEN, assertThrows(ServiceException.class,
                () -> sessions.getUserInfoPOByToken(TOKEN)).getResultCode());
    }

    @Test
    void wrongOrExpiredCodeAndUpstreamErrorsPreserveLocalState() {
        for (int code : List.of(30001, 30002, 30003, 20006, 40002)) {
            when(response.body()).thenReturn("{\"code\":" + code + "}");
            TclApiException failure = assertThrows(TclApiException.class,
                    () -> sessions.deleteAccount("123456"));
            assertEquals(code, failure.getTclCode());
            assertFalse(failure.getMessage().contains("未知"));
        }
        verify(redis, never()).delete(anyString());
        verify(users, never()).updateById(any(OAuthUserInfo.class));
    }

    @Test
    void disablesAccountBeforeCleaningCredentialsEvenIfRedisCleanupFails() {
        when(redis.delete(RedisKeyUtil.tclToken(UID))).thenThrow(new IllegalStateException("unavailable"));
        assertThrows(IllegalStateException.class, () -> sessions.deleteAccount("123456"));
        var order = inOrder(users, redis);
        order.verify(users).updateById(any(OAuthUserInfo.class));
        order.verify(redis).delete(RedisKeyUtil.tclToken(UID));
    }

    @Test
    void invalidRefreshTokenClearsOnlyCredentialCacheForBothEndpoints() {
        when(response.body()).thenReturn("{\"code\":90009}");
        assertThrows(TclApiException.class, () -> tokens.sendAccountDeletionCodeByUid(UID));
        assertThrows(TclApiException.class, () -> sessions.deleteAccount("123456"));
        verify(redis, times(2)).delete(RedisKeyUtil.tclToken(UID));
        verify(redis, never()).delete(RedisKeyUtil.loginToken(TOKEN));
        verifyNoInteractions(users);
    }

    @Test
    void rejectsInvalidCodesBeforeAnyUpstreamOrCacheAccess() {
        for (String code : List.of("", " ", "12345", "1234567", "abcdef", "１２３４５６")) {
            assertThrows(ServiceException.class, () -> sessions.deleteAccount(code));
        }
        assertThrows(ServiceException.class, () -> sessions.deleteAccount(null));
        verifyNoInteractions(http, users);
        verify(values, never()).get(anyString());
    }

    @Test
    void requiresLoginAndStoredRefreshTokenWithoutMigratingAnotherCredential() {
        UserContext.clear();
        assertThrows(ServiceException.class, () -> sessions.deleteAccount("123456"));
        assertThrows(ServiceException.class, () -> tokens.sendAccountDeletionCodeByUid(null));
        when(values.get(RedisKeyUtil.tclToken(UID))).thenReturn(null);
        assertThrows(ServiceException.class, () -> tokens.sendAccountDeletionCodeByUid(UID));
        assertThrows(ServiceException.class, () -> tokens.deleteAccountByUid(UID, "123456"));
        when(values.get(RedisKeyUtil.tclToken(UID))).thenReturn("{\"access_token\":\"only-access\"}");
        assertThrows(ServiceException.class, () -> tokens.deleteAccountByUid(UID, "123456"));
        verifyNoInteractions(http, users);
    }

    @Test
    void malformedResponsesAndNetworkFailuresNeverCompleteLocalDeletion() throws Exception {
        for (String body : List.of("not-json", "null", "{}", "{\"code\":\"wrong\"}")) {
            when(response.body()).thenReturn(body);
            assertThrows(ServiceException.class, () -> sessions.deleteAccount("123456"));
        }
        when(response.statusCode()).thenReturn(503);
        assertThrows(ServiceException.class, () -> sessions.deleteAccount("123456"));
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.io.IOException("unavailable"));
        assertThrows(ServiceException.class, () -> sessions.deleteAccount("123456"));
        verify(redis, never()).delete(anyString());
        verify(users, never()).updateById(any(OAuthUserInfo.class));
    }

    @Test
    void routesRequireLocalLoginAndIgnoreClientCredentialOverrides() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new UserController(sessions, tokens))
                .addInterceptors(new UserInterceptor(sessions))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        for (String path : List.of("/auth/user/account/send-delete-code", "/auth/user/account/delete")) {
            mvc.perform(post(path)).andExpect(jsonPath("$.code").value(ResultCode.USER_NOT_LOGIN.code()));
        }
        verifyNoInteractions(http);
        mvc.perform(post("/auth/user/account/send-delete-code").header("Authorization", "Authorization" + TOKEN))
                .andExpect(jsonPath("$.code").value(200));
        assertRequest("/oauth2/account/send-delete-code");
        mvc.perform(post("/auth/user/account/delete").header("Authorization", "Authorization" + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"012345\",\"uid\":99,\"refresh_token\":\"attacker-refresh\"}"))
                .andExpect(jsonPath("$.code").value(200));
        assertRequest("/oauth2/account/delete");
    }

    @Test
    void missingBodyAndMissingCodeReturnBusinessValidationError() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new UserController(sessions, tokens))
                .addInterceptors(new UserInterceptor(sessions))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/auth/user/account/delete").header("Authorization", "Authorization" + TOKEN))
                .andExpect(jsonPath("$.code").value(ResultCode.VERIFICATION_CODE_NOT_ENTER.code()));
        mvc.perform(post("/auth/user/account/delete").header("Authorization", "Authorization" + TOKEN)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.code").value(ResultCode.VERIFICATION_CODE_NOT_ENTER.code()));
        verifyNoInteractions(http);
    }

    private JsonNode assertRequest(String path) throws Exception {
        assertEquals(path, sent.uri().getPath());
        assertNull(sent.uri().getQuery());
        assertEquals("POST", sent.method());
        assertEquals("application/json", sent.headers().firstValue("Content-Type").orElseThrow());
        assertTrue(sent.headers().firstValue("Authorization").isEmpty());
        JsonNode body = new ObjectMapper().readTree(bodyText(sent));
        assertEquals("server-refresh", body.path("refresh_token").asText());
        assertFalse(body.has("client_id"));
        assertFalse(body.has("client_secret"));
        assertFalse(body.has("uid"));
        return body;
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
