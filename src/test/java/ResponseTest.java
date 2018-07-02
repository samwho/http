import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static com.google.common.truth.Truth.assertThat;

class ResponseTest {
    @Test
    public void canWriteRawResponse() throws Exception {
        Response r = Response.builder()
                .withStatus(Status.OK)
                .withBody("Hello, world!")
                .build();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        r.writeTo(os);

        assertThat(os.toString()).isEqualTo("HTTP/1.1 200 OK\r\nContent-Length: 13\r\n\r\nHello, world!");
    }
}