import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class ServerTest {
    private final CountDownLatch latch = new CountDownLatch(1);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> server;
    private int port;

    @BeforeEach
    public void setUp() throws Exception {
        AtomicReference<Exception> e = new AtomicReference<>(null);
        AtomicInteger p = new AtomicInteger();
        server = executor.submit(() -> {
            try {
                Server.builder()
                        .withRequestHandler(req -> Response.builder().withStatus(Status.OK).build())
                        .addServerListener(new ServerListener() {
                            @Override
                            public void onServerConnect(ServerSocket serverSocket) {
                                p.set(serverSocket.getLocalPort());
                                latch.countDown();
                            }
                        })
                        .build()
                        .start();
            } catch (IOException ex) {
                e.set(ex);
                latch.countDown();
            }
        });
        latch.await();
        port = p.get();
        if (e.get() != null) {
            fail("failed to start server", e.get());
        }
    }

    @AfterEach
    public void tearDown() {
        server.cancel(true);
        executor.shutdown();
    }

    @Test
    public void getRequest() throws Exception {
        HttpResponse res = Unirest.get("http://localhost:" + port + "/").asString();
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    public void postRequest() throws Exception {
        HttpResponse res = Unirest.post("http://localhost:" + port + "/").asString();
        assertThat(res.getStatus()).isEqualTo(200);
    }
}
