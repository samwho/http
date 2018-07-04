import com.google.common.base.Preconditions;
import com.google.common.flogger.FluentLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public final class Server {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private final int port;
    private final int socketQueueLength;
    private final RequestHandler requestHandler;
    private final Iterable<RequestListener> requestListeners;
    private final Iterable<ServerListener> serverListeners;
    private final ExecutorService executorService;
    private final int numThreads;
    private final BlockingQueue<Socket> queue;

    private Server(int port, int socketQueueLength, RequestHandler requestHandler, Iterable<RequestListener> requestListeners, Iterable<ServerListener> serverListeners, int numThreads, int queueSize) {
        this.port = port;
        this.socketQueueLength = socketQueueLength;
        this.requestHandler = requestHandler;
        this.requestListeners = requestListeners;
        this.serverListeners = serverListeners;
        this.executorService = Executors.newFixedThreadPool(numThreads);
        this.numThreads = numThreads;
        this.queue = new ArrayBlockingQueue<>(queueSize);
    }

    public void start() throws IOException {
        byte[] addr = {0, 0, 0, 0};
        ServerSocket server = new ServerSocket(port, socketQueueLength, InetAddress.getByAddress(addr));
        logger.atInfo().log("server connected on port %d", port);

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

        serverListeners.forEach(ServerListener::onStart);
        while (true) {
            Socket client = server.accept();
            logger.atInfo().log("accepted connection from client %s", client);
            queue.add(client);
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
        private List<ServerListener> serverListeners = new ArrayList<>();
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

        public Builder addServerListener(ServerListener serverListener) {
            this.serverListeners.add(serverListener);
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

            return new Server(port, socketQueueLength, requestHandler, requestListeners, serverListeners, numThreads, queueSize);
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
