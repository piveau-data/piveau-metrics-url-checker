package io.piveau.metrics.url_checker;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {

    private HttpServer callbackServer;
    private WebClient webClient;

    private static final int TEST_SERVER_PORT = 8081;
    private static final int CALLBACK_PORT = 8082;

    @BeforeAll
    void setup(Vertx vertx, VertxTestContext testContext) {
        WebClientOptions clientOptions = new WebClientOptions()
            .setDefaultHost("localhost")
            .setDefaultPort(ApplicationConfig.DEFAULT_APPLICATION_PORT);
        webClient = WebClient.create(vertx, clientOptions);

        // spawn server offering mock endpoint for testing
        Router router = Router.router(vertx);
        router.head("/valid_url").handler(validRoute ->
            validRoute.response().setStatusCode(200).putHeader("Content-Type", "application/json").end());

        JsonObject testConfig = new JsonObject()
            .put(ApplicationConfig.ENV_APPLICATION_PORT, ApplicationConfig.DEFAULT_APPLICATION_PORT);

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(TEST_SERVER_PORT, testContext.succeeding(mockServer ->
                vertx.deployVerticle(MainVerticle.class.getName(), new DeploymentOptions().setConfig(testConfig), testContext.completing())));
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        callbackServer.close(testContext.completing());
    }

    @Test
    void testReachableUrl(Vertx vertx, VertxTestContext testContext) {
        String testUrl = "http://localhost:" + TEST_SERVER_PORT + "/valid_url";

        JsonObject checkRequest = new JsonObject()
            .put("callback", "http://localhost:" + CALLBACK_PORT + "/valid_url_callback")
            .put("urls", new JsonArray().add(testUrl));

        JsonArray expectedResult = new JsonArray().add(
            new JsonObject()
                .put("url", testUrl)
                .put("statusCode", 200)
                .put("mimeType", "application/json")
                .put("message", "OK"));

        Router router = Router.router(vertx);
        router.post("/valid_url_callback").handler(callback -> {
            testResponse(testContext, callback, expectedResult);
            callback.response().setStatusCode(200).putHeader("Content-Type", "application/json").end();
        });

        // create custom callback server
        callbackServer = vertx.createHttpServer()
            .requestHandler(router)
            .listen(CALLBACK_PORT, testContext.succeeding(callbackServer ->
                webClient.post("/check")
                    .expect(ResponsePredicate.SC_ACCEPTED)
                    .sendJson(checkRequest, testContext.succeeding())));
    }

    @Test
    void testUnreachableUrl(Vertx vertx, VertxTestContext testContext) {
        // this endpoint doesn't exist
        String testUrl = "http://localhost:" + TEST_SERVER_PORT + "/invalid_url";

        JsonObject checkRequest = new JsonObject()
            .put("callback", "http://localhost:" + CALLBACK_PORT + "/invalid_url_callback")
            .put("urls", new JsonArray().add(testUrl));

        JsonArray expectedResult = new JsonArray().add(
            new JsonObject()
                .put("url", testUrl)
                .put("statusCode", 404)
                .putNull("mimeType")
                .put("message", "Not Found"));

        Router router = Router.router(vertx);
        router.post("/invalid_url_callback").handler(callback -> {
            testResponse(testContext, callback, expectedResult);
            callback.response().setStatusCode(200).putHeader("Content-Type", "application/json").end();
        });

        // create custom callback server
        callbackServer = vertx.createHttpServer()
            .requestHandler(router)
            .listen(CALLBACK_PORT, testContext.succeeding(callbackServer ->
                webClient.post("/check")
                    .expect(ResponsePredicate.SC_ACCEPTED)
                    .sendJson(checkRequest, testContext.succeeding())));
    }

    private void testResponse(VertxTestContext testContext, RoutingContext context, JsonArray expectedResult) {
        testContext.verify(() ->
            context.request().bodyHandler(body -> {
                assertNotNull(body);

                try {
                    assertEquals(expectedResult, body.toJsonArray());
                    testContext.completeNow();
                } catch (Exception e) {
                    fail(e);
                }
            }));
    }
}
