import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.google.common.truth.Truth.assertThat;

public class ServerTest {
    private final CountDownLatch latch = new CountDownLatch(1);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> server;

    @BeforeEach
    public void setUp() throws Exception {
        server = executor.submit(() -> {
            try {
                Server.builder()
                        .withPort(8080)
                        .withRequestHandler(req -> Response.builder().withStatus(Status.OK).build())
                        .addServerListener(new ServerListener() {
                            @Override
                            public void onStart() {
                                latch.countDown();
                            }
                        })
                        .build()
                        .start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        latch.await();
    }

    @AfterEach
    public void tearDown() {
        server.cancel(true);
        executor.shutdown();
    }

    @Test
    public void getRequest() throws Exception {
        HttpResponse res = Unirest.get("http://localhost:8080/").asString();
        assertThat(res.getStatus()).isEqualTo(200);
    }
}
