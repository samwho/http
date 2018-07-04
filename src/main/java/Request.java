import com.google.common.base.*;
import com.google.common.collect.ArrayListMultimap;
import org.apache.commons.lang.builder.EqualsBuilder;

import java.io.*;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.*;

public final class Request {
    private static final Splitter SPACE_SPLITTER = Splitter.on(" ");
    private static final Splitter HEADER_SPLITTER = Splitter.on(": ");

    private final String httpVersion;
    private final String method;
    private final String requestUri;
    private final ArrayListMultimap<String, String> queryParams;
    private final ArrayListMultimap<String, String> headers;
    private final String body;

    public Request(
            String method,
            String requestUri,
            String httpVersion,
            ArrayListMultimap<String, String> headers,
            ArrayListMultimap<String, String> queryParams,
            String body
    ) {
        this.method = method;
        this.requestUri = requestUri;
        this.httpVersion = httpVersion;
        this.headers = headers;
        this.queryParams = queryParams;
        this.body = body;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        Request rhs = (Request) obj;
        return new EqualsBuilder()
            .append(method, rhs.method)
            .append(requestUri, rhs.requestUri)
            .append(httpVersion, rhs.httpVersion)
            .append(headers, rhs.headers)
            .append(queryParams, rhs.queryParams)
            .append(body, rhs.body)
            .isEquals();
    }

    public static Request parse(InputStream is) throws IOException, RequestParseException {
        String requestLine = new String(readLine(is), Charsets.UTF_8);
        List<String> parts = SPACE_SPLITTER.splitToList(requestLine);

        if (parts.size() != 3) {
            throw new RequestParseException(String.format("invalid request line: \"%s\"", requestLine));
        }

        String method = parts.get(0);
        String requestUri = parts.get(1);
        String httpVersion = parts.get(2);

        ArrayListMultimap<String, String> headers = ArrayListMultimap.create();
        String headerLine;
        while (true) {
            headerLine = new String(readLine(is), Charsets.UTF_8);
            if (Strings.isNullOrEmpty(headerLine)) {
                break;
            }

            List<String> headerParts = HEADER_SPLITTER.splitToList(headerLine);
            Preconditions.checkArgument(headerParts.size() == 2, "invalid header line");

            headers.put(headerParts.get(0), headerParts.get(1));
        }

        String encoding;
        try {
            encoding = headers.get("Content-Encoding").get(0);
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            encoding = "UTF-8";
        }

        ArrayListMultimap<String, String> queryParams = ArrayListMultimap.create();
        int queryStart = requestUri.indexOf('?');
        if (queryStart != -1) {
            String queryParamsString = requestUri.substring(queryStart + 1, requestUri.length());
            requestUri = requestUri.substring(0, queryStart);

            for (String queryPart : Splitter.on('&').split(queryParamsString)) {
                List<String> keyValue = Splitter.on('=').splitToList(queryPart);
                Preconditions.checkArgument(keyValue.size() == 2);

                String key;
                String value;
                try {
                    key = URLDecoder.decode(keyValue.get(0), encoding);
                    value = URLDecoder.decode(keyValue.get(1), encoding);
                } catch (UnsupportedEncodingException e) {
                    throw new RequestParseException(e);
                }

                queryParams.put(key, value);
            }
        }

        String body = null;
        List<String> contentLengthHeaders = headers.get("Content-Length");
        if (!contentLengthHeaders.isEmpty()) {
            int contentLength = Integer.valueOf(contentLengthHeaders.get(0));

            byte[] buffer = new byte[contentLength];
            int bytesRead = is.read(buffer);
            if (bytesRead != contentLength) {
                // do something drastic
            }

            CharsetDecoder decoder = Charset.forName(encoding).newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);

            body = decoder.decode(ByteBuffer.wrap(buffer)).toString();
        }

        return new Request(method, requestUri, httpVersion, headers, queryParams, body);
    }

    private static byte[] readLine(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        while (true) {
            int nextByte = is.read();
            if (nextByte == -1) {
                break;
            } else if (nextByte == '\r') {
                int backslashN = is.read(); // read the \n
                if (backslashN != '\n') {
                    // this shouldn't happen, do something
                }
                break;
            } else if (nextByte == '\n') {
               // this also shouldn't happen, do something
            }
            buffer.write(nextByte);
        }

        return buffer.toByteArray();
    }

    public String getMethod() {
        return method;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public ArrayListMultimap<String, String> getQueryParams() {
        return queryParams;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public ArrayListMultimap<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("method", method)
                .add("requestUri", requestUri)
                .add("queryParams", queryParams)
                .add("httpVersion", httpVersion)
                .add("headers", headers)
                .add("body", body)
                .toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String method = "GET";
        private String requestUri = "/";
        private String httpVersion = "HTTP/1.1";
        private ArrayListMultimap<String, String> headers = ArrayListMultimap.create();
        private ArrayListMultimap<String, String> queryParams = ArrayListMultimap.create();
        private String body = null;

        public Builder withMethod(String method) {
            this.method = method;
            return this;
        }

        public Builder withRequestUri(String requestUri) {
            this.requestUri = requestUri;
            return this;
        }

        public Builder withHttpVersion(String httpVersion) {
            this.httpVersion = httpVersion;
            return this;
        }

        public Builder addHeader(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public Builder addQueryParam(String key, String value) {
            this.queryParams.put(key, value);
            return this;
        }

        public Builder withBody(String body) {
            this.body = body;
            return this;
        }

        public Request build() {
            return new Request(method, requestUri, httpVersion, headers, queryParams, body);
        }
    }
}