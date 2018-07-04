import java.io.IOException;
import java.net.ServerSocket;

public interface ServerListener {
    void onServerConnect(ServerSocket socket) throws IOException;
}
