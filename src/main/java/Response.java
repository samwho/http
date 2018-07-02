import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;

public final class Response {
    private static final byte[] CRLF = "\r\n".getBytes();

    private final Status status;
    // TODO implement own multimap, Stan said so.
    private final Multimap<String, String> headers;
    private final String body;

    private Response(Status status, Multimap<String, String> headers, String body) {
        this.status = status;
        this.headers = headers;
        this.body = body;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Status status = Status.OK;
        private Multimap<String, String> headers = ArrayListMultimap.create();
        private String body = null;

        public Builder withStatus(Status status) {
            this.status = status;
            return this;
        }

        public Builder addHeader(String key, String value) {
            headers.put(key, value);
            return this;
        }

        public Builder withBody(String body) {
           this.body = body;

           Collection<String> contentLength = headers.get("Content-Length");
           if (contentLength.isEmpty()) {
               headers.put("Content-Length", String.valueOf(body.getBytes().length));
           }

           return this;
        }

        public Response build() {
            return new Response(status, headers, body);
        }
    }

    public void writeTo(OutputStream os) throws IOException {
        os.write(status.toString().getBytes());
        os.write(CRLF);

        for (Map.Entry<String, String> entry : headers.entries()) {
            String s = entry.getKey() + ": " + entry.getValue();
            os.write(s.getBytes());
            os.write(CRLF);
        }

        if (!Strings.isNullOrEmpty(body)) {
            os.write(CRLF);
            os.write(body.getBytes());
        }
    }
}
