package io.piveau.metrics.url_checker;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.handler.StaticHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MainVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(MainVerticle.class);

    private JsonObject config;

    @Override
    public void start(Promise<Void> startPromise) {
        log.info("Launching URL-Checker...");

        // startup is only successful if no step failed
        Future<Void> steps = loadConfig()
                .compose(handler -> bootstrapVerticles())
                .compose(handler -> startServer());

        steps.setHandler(handler -> {
            if (handler.succeeded()) {
                log.info("URL-Checker successfully launched");
                startPromise.complete();
            } else {
                log.error("Failed to launch Transformer: " + handler.cause());
                startPromise.fail(handler.cause());
                vertx.close();
            }
        });
    }

    private Future<Void> loadConfig() {
        return Future.future(loadConfig -> {
            ConfigRetriever.create(vertx).getConfig(handler -> {
                if (handler.succeeded()) {
                    config = handler.result();
                    log.debug(config.encodePrettily());
                    loadConfig.complete();
                } else {
                    loadConfig.fail(handler.cause());
                }
            });
        });
    }

    private Future<Void> bootstrapVerticles() {
        DeploymentOptions options = new DeploymentOptions()
                .setInstances(config.getInteger(ApplicationConfig.ENV_WORKER_COUNT, ApplicationConfig.DEFAULT_WORKER_COUNT))
                .setConfig(config)
                .setWorker(true);

        return Future.future(deployVerticle -> {
            vertx.deployVerticle(UrlCheckerVerticle.class.getName(), options, handler -> {
                if (handler.succeeded()) {
                    deployVerticle.complete();
                } else {
                    deployVerticle.fail("Failed to deploy [" + UrlCheckerVerticle.class.getName() + "] : " + handler.cause());
                }
            });
        });
    }

    private Future<Void> startServer() {
        return Future.future(startServer -> {
            Integer port = config.getInteger(ApplicationConfig.ENV_APPLICATION_PORT, ApplicationConfig.DEFAULT_APPLICATION_PORT);

            OpenAPI3RouterFactory.create(vertx, "webroot/openapi.yaml", handler -> {
                if (handler.succeeded()) {
                    OpenAPI3RouterFactory routerFactory = handler.result();

                    routerFactory.addHandlerByOperationId("checkUrl", this::checkUrl);

                    Router router = routerFactory.getRouter();
                    router.route("/*").handler(StaticHandler.create());

                    HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(port));
                    server.requestHandler(router).listen();

                    log.info("Server successfully launched on port [{}]", port);
                    startServer.complete();
                } else {
                    startServer.fail(handler.cause());
                }
            });
        });
    }


    // passes urlCheckRequest to worker and handles response
    private void checkUrl(RoutingContext routingContext) {
        if (routingContext.getBodyAsString() != null) {
            // attempt decoding to detect malformed requests
            try {
                UrlCheckRequest request = Json.decodeValue(routingContext.getBodyAsString(), UrlCheckRequest.class);
                log.debug("Received valid {}", request.toString());

                vertx.eventBus().send(ApplicationConfig.MSG_URL_CHECK_ADDR, routingContext.getBodyAsString());

                routingContext.response()
                        .setStatusCode(202) // accepted
                        .putHeader("Content-Type", "application/json")
                        .end(routingContext.getBodyAsString());

            } catch (DecodeException e) {
                log.warn("Exception thrown while decoding request {}", routingContext.getBodyAsString(), e);
                routingContext.response().setStatusCode(400).end("Malformed request");
            }
        } else {
            routingContext.response().setStatusCode(400).end("No request body provided");
        }
    }
}
