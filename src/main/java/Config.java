import java.io.File;

public class Config {
    static final File WEB_ROOT = new File(CN2HTTPServer.class.getResource("/webroot").getFile());
    static final String INDEX_HTML = "index.html";
    static final String NOT_FOUND_FILE = "404.html";
    static final String UNSUPPORTED_FILE = "unsupported.html";
    static final int PORT = 8080;

    public enum MimeTypes {

        HTML("text/html"),
        PLAIN("text/plain");

        private final String mime;

        MimeTypes(String mime) {
            this.mime = mime;
        }

        public String get() {
            return mime;
        }
    }

    public enum Headers {

        SERVER("Server: CN2HTTPServer"),
        DATE("Date: %s"),
        CONTENT_TYPE("Content-type: %s"),
        CONtENT_LENGTH("Content-length: %s");

        private final String header;

        Headers(String header) {
            this.header = header;
        }

        public String get(Object... args) {
            return String.format(header, args);
        }

    }

    public enum Methods {
        GET("GET"),
        HEAD("HEAD");

        private final String method;

        Methods(String method) {
            this.method = method;
        }

        public String get() {
            return method;
        }
    }

    public enum Codes {
        OK("HTTP/1.1 200 OK"),
        NOT_IMPLEMENTED("HTTP/1.1 501 Not Implemented"),
        NOT_FOUND("HTTP/1.1 404 File Not Found");

        private final String code;

        Codes(String code) {
            this.code = code;
        }

        public String get() {
            return code;
        }
    }
}
