package io.piveau.metrics.url_checker;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class UrlCheckerVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(UrlCheckerVerticle.class);

    private UrlValidator validator =
        new UrlValidator(UrlValidator.ALLOW_ALL_SCHEMES + UrlValidator.ALLOW_2_SLASHES + UrlValidator.ALLOW_LOCAL_URLS);

    private WebClient webClient;

    private CircuitBreakerOptions circuitBreakerOptions;


    @Override
    public void start(Promise<Void> startPromise) {

        int timeOutInMillis = config().getInteger(ApplicationConfig.ENV_TIMEOUT_MILLIS, ApplicationConfig.DEFAULT_TIMEOUT_IN_MILLIS);

        WebClientOptions clientOptions = new WebClientOptions()
            .setUserAgent(config().getString(ApplicationConfig.ENV_HTTP_USER_AGENT, ApplicationConfig.DEFAULT_HTTP_USER_AGENT))
            .setConnectTimeout(timeOutInMillis)
            .setIdleTimeout(timeOutInMillis)
            .setIdleTimeoutUnit(TimeUnit.MILLISECONDS)
            .setLogActivity(true);

        webClient = WebClient.create(vertx, clientOptions);

        circuitBreakerOptions = new CircuitBreakerOptions()
            .setMaxRetries(5)
            .setTimeout(10000)
            .setResetTimeout(60000);

        vertx.eventBus().<String>consumer(ApplicationConfig.MSG_URL_CHECK_ADDR, message -> {

            // decode request and check each url
            UrlCheckRequest urlCheckRequest = Json.decodeValue(message.body(), UrlCheckRequest.class);

            CompositeFuture.all(urlCheckRequest.getUrls().stream().map(this::checkUrl).collect(Collectors.toList())).setHandler(handler ->
                CircuitBreaker.create("callback-circuit-breaker", vertx, circuitBreakerOptions).execute(circuitBreakerFuture -> {
                    try {
                        webClient.postAbs(urlCheckRequest.getCallback())
                            .expect(ResponsePredicate.SC_OK)
                            .sendJson(handler.result().list(), response -> {
                                if (response.succeeded()) {
                                    circuitBreakerFuture.complete();
                                } else {
                                    circuitBreakerFuture.fail(response.cause());
                                }
                            });
                    } catch (Exception e) {
                        circuitBreakerFuture.fail(e);
                    }
                }).setHandler(circuitBreakerResult -> {
                    if (circuitBreakerResult.failed())
                        LOG.error("Failed to send URL callback to [{}]", urlCheckRequest.getCallback(), circuitBreakerResult.cause());
                })
            );
        });

        startPromise.complete();
    }

    private Future<UrlCheckResponse> checkUrl(String url) {

        return Future.future(checkUrl -> {
            if (url == null || !validator.isValid(encodeUrl(url))) {
                UrlCheckResponse urlCheckResponse = new UrlCheckResponse(url);
                urlCheckResponse.setStatusCode(config().getInteger(ApplicationConfig.ENV_INVALID_URL_STATUS, ApplicationConfig.DEFAULT_INVALID_STATUS_));
                urlCheckResponse.setMessage(config().getString(ApplicationConfig.ENV_INVALID_URL_MESSAGE, ApplicationConfig.DEFAULT_INVALID_URL_MESSAGE));

                LOG.debug("Encountered invalid URL: {}", urlCheckResponse.toString());
                checkUrl.complete(urlCheckResponse);
            } else {
                webClient.headAbs(url).send(handler -> {
                    UrlCheckResponse urlCheckResponse = new UrlCheckResponse(url);

                    if (handler.succeeded()) {
                        urlCheckResponse.setStatusCode(handler.result().statusCode());
                        urlCheckResponse.setMessage(handler.result().statusMessage());
                        urlCheckResponse.setMimeType(handler.result().getHeader("Content-Type"));
                    } else {
                        urlCheckResponse.setMessage(handler.cause().getMessage());
                        urlCheckResponse.setStatusCode(config().getInteger(ApplicationConfig.ENV_CONNECTION_EXCEPTION_STATUS, ApplicationConfig.DEFAULT_STATUS_CODE_EXCEPTION));
                    }

                    LOG.debug("Finished checking URL: {}", urlCheckResponse.toString());
                    checkUrl.complete(urlCheckResponse);
                });
            }
        });
    }

    private String encodeUrl(String url) {
        return url.replaceAll(" ", "%20");
    }
}
