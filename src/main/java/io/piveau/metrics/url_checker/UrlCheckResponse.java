package io.piveau.metrics.url_checker;

public class UrlCheckResponse {

    private String url;

    private Integer statusCode;

    private String mimeType;

    private String message;

    public UrlCheckResponse(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "UrlCheckResponse{" +
                "url='" + url + '\'' +
                ", statusCode=" + statusCode +
                ", mimeType='" + mimeType + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
