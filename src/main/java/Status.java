public enum Status {
    OK(200, "OK"),
    BAD_REQUEST(400, "Bad Request"),
    ;

    private final int status;
    private final String reasonPhrase;

    Status(int status, String reasonPhrase) {
        this.status = status;
        this.reasonPhrase = reasonPhrase;
    }

    public int getStatus() {
        return status;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    @Override
    public String toString() {
        return "HTTP/1.1 " + status + " " + reasonPhrase;
    }
}
