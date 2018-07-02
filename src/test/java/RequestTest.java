import com.google.common.base.Charsets;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.MalformedInputException;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestTest {
    @Test
    public void validGetRequest() throws Exception {
        String requestStr = "GET / HTTP/1.1\r\n\r\n";
        InputStream input = new ByteArrayInputStream(requestStr.getBytes());

        Request result = Request.parse(input);
        Request expected = Request.builder()
                .withMethod("GET")
                .withRequestUri("/")
                .withHttpVersion("HTTP/1.1")
                .build();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void validPostRequestWithBody() throws Exception {
        // TODO: something something content-length header required?
        String requestStr = "POST / HTTP/1.1\r\nContent-Length: 13\r\n\r\nHello, world!";
        InputStream input = new ByteArrayInputStream(requestStr.getBytes());

        Request result = Request.parse(input);
        Request expected = Request.builder()
                .withMethod("POST")
                .withBody("Hello, world!")
                .withRequestUri("/")
                .withHttpVersion("HTTP/1.1")
                .addHeader("Content-Length", "13")
                .build();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void totalGibberish() {
        String requestStr = "tvyun854tyu84932ynvbt839y48t";
        InputStream input = new ByteArrayInputStream(requestStr.getBytes());

        assertThrows(RequestParseException.class, () -> Request.parse(input));
    }

    @Test
    public void parsesQueryParams() throws Exception {
        String requestStr = "GET /foo?foo=hello%20world HTTP/1.1\r\n\r\n";
        InputStream input = new ByteArrayInputStream(requestStr.getBytes());

        Request request = Request.parse(input);
        assertThat(request.getQueryParams()).containsExactly("foo", "hello world");
    }

    @Test
    public void requestBodyWrongEncoding() throws Exception {
        byte[] requestTop = "POST / HTTP/1.1\r\nContent-Length: 26\r\nContent-Encoding: UTF-8\r\n\r\n".getBytes(Charsets.UTF_8);
        byte[] requestBottom = "Hello, world!".getBytes(Charsets.UTF_16);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(requestTop);
        os.write(requestBottom);
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        assertThrows(MalformedInputException.class, () -> Request.parse(is));
    }
}