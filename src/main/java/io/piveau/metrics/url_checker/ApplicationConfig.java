package io.piveau.metrics.url_checker;

final class ApplicationConfig {

    static final String MSG_URL_CHECK_ADDR = "url.check";

    static final String ENV_APPLICATION_PORT = "PORT";
    static final Integer DEFAULT_APPLICATION_PORT = 8080;

    static final String ENV_WORKER_COUNT = "WORKER_COUNT";
    static final Integer DEFAULT_WORKER_COUNT = 30;

    static final String ENV_TIMEOUT_MILLIS = "TIMEOUT_IN_MILLIS";
    static final Integer DEFAULT_TIMEOUT_IN_MILLIS = 10000;

    static final String ENV_CONNECTION_EXCEPTION_STATUS = "CONNECTION_EXCEPTION_STATUS";
    static final Integer DEFAULT_STATUS_CODE_EXCEPTION = 1100;

    static final String ENV_INVALID_URL_STATUS = "INVALID_URL_STATUS";
    static final Integer DEFAULT_INVALID_STATUS_ = 1300;

    static final String ENV_HTTP_USER_AGENT = "HTTP_USER_AGENT";
    static final String DEFAULT_HTTP_USER_AGENT = "Mozilla/5.0 (European Data Portal) Gecko/20100101 Firefox/40.1";

    static final String ENV_INVALID_URL_MESSAGE = "INVALID_URL_MESSAGE";
    static final String DEFAULT_INVALID_URL_MESSAGE = "Invalid URL";
}
