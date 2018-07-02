@FunctionalInterface
public interface RequestHandler {
    Response handle(Request req) throws RequestHandlerException;
}
