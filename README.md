# MQA URL Checker

## Setup

1. Install Java 11

2. Clone the directory and enter it

        git clone git@gitlab.fokus.fraunhofer.de:viaduct/metrics/metrics-url-checker.git

3. Edit the environment variables in the `Dockerfile` to your liking. Variables and their purpose are listed below:

    |Key|Description|Default|
    |:--- |:---|:---|
    |PORT| The port this service will run on | 8085 |
    |WORKER_COUNT| The number of UrlCheckerVerticles to spawn | 3 |
    |HTTP_USER_AGENT| The user agent to send with the check requests | Mozilla/5.0 (European Data Portal) Gecko/20100101 Firefox/40.1 |
    |TIMEOUT_IN_MILLIS| HTTP request timeout in milliseconds | 10000 |
    |CONNECTION_EXCEPTION_STATUS| The status code to set in case of exceptions | 1100 |
    |INVALID_URL_STATUS| The status code to set if invalid URLs are provided | 1300 |
    |INVALID_URL_MESSAGE| The status message to set if invalid URLs are provided | Invalid URL |

## Run

### Production

Build the project by using the provided Maven wrapper. This ensures everyone this software is provided to can use the exact same version of the maven build tool.
The generated _fat-jar_ can then be found in the `target` directory.

* CLI

        mvn clean package
        java -jar target/application.jar

* Docker

        1. Start your docker daemon
        2. Build the application as described in Windows or Linux
        3. Adjust the port number (`EXPOSE` in the `Dockerfile`)
        4. Build the image: `docker build -t metrics/url-checker .`
        5. Run the image, adjusting the port number as set in step 3: `docker run -i -p 8080:8080 metrics/url-checker`

### Development

## CI

The repository uses the gitlab builtin CI Framework.
The `gitlab-ci.yaml` file contains the appropriate config. With each push a new docker image is built using the `Dockerfile`.
The image is then stored in the gitlab registry, from which it can then be pulled using the following command:

    docker pull registry.gitlab.com/european-data-portal/mqa-url-checker

## API

A formal OpenAPI 3 specification can be found in the `src/main/resources/webroot/openapi.yaml` file.
A visually more appealing version is available at `{url}:{port}` once the application has been started.CHANGELOG.md
