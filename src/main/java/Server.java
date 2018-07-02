import com.google.common.base.Preconditions;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public final class Server {
    private final static int NUM_THREADS = 2;
    private final static int QUEUE_SIZE = 32;

    private final int port;
    private final int socketQueueLength;
    private final RequestHandler requestHandler;
    private final Iterable<RequestListener> requestListeners;
    private final ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
    private final int numThreads;
    private final BlockingQueue<Socket> queue;

    private Server(int port, int socketQueueLength, RequestHandler requestHandler, Iterable<RequestListener> requestListeners, int numThreads, int queueSize) {
        this.port = port;
        this.socketQueueLength = socketQueueLength;
        this.requestHandler = requestHandler;
        this.requestListeners = requestListeners;
        this.numThreads = numThreads;
        this.queue = new ArrayBlockingQueue<>(queueSize);
    }

    public void start() throws IOException {
        byte[] addr = {0, 0, 0, 0};
        ServerSocket server = new ServerSocket(port, socketQueueLength, InetAddress.getByAddress(addr));

        for (int i = 0; i < numThreads; i++) {
            executorService.submit(() -> {
                while (true) {
                    try (Socket client = queue.take()) {
                        Response res;

                        try {
                            Request req = Request.parse(client.getInputStream());
                            requestListeners.forEach(listener -> listener.onRequest(req));
                            res = requestHandler.handle(req);
                        } catch (RequestHandlerException e) {
                            res = e.toResponse();
                        } catch (RequestParseException e) {
                            res = Response.builder()
                                    .withStatus(Status.BAD_REQUEST)
                                    .build();
                        }

                        res.writeTo(client.getOutputStream());
                    }
                }
            });
        }

        while (true) {
            queue.add(server.accept());
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int port = 8080;
        private int socketQueueLength = 10;
        private RequestHandler requestHandler;
        private List<RequestListener> requestListeners = new ArrayList<>();
        private int numThreads = 1;
        private int queueSize = 1;

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withSocketQueueLength(int socketQueueLength) {
            this.socketQueueLength = socketQueueLength;
            return this;
        }

        public Builder withRequestHandler(RequestHandler requestHandler) {
            this.requestHandler = requestHandler;
            return this;
        }

        public Builder addRequestListener(RequestListener requestListener) {
            this.requestListeners.add(requestListener);
            return this;
        }

        public Builder withNumThreads(int numThreads) {
            this.numThreads = numThreads;
            return this;
        }

        public Builder withQueueSize(int queueSize) {
            this.queueSize = queueSize;
            return this;
        }

        public Server build() {
            Preconditions.checkNotNull(requestHandler);

            return new Server(port, socketQueueLength, requestHandler, requestListeners, numThreads, queueSize);
        }
    }

    public static void main(String... args) throws Exception {
        Server.builder()
            .addRequestListener(System.out::println)
            .withRequestHandler(req -> Response.builder().withBody("Hello, world!").build())
            .build()
            .start();
    }
}
