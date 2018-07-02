public final class RequestHandlerException extends Exception {
    private final Status status;
    private final String message;

    private RequestHandlerException(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public Response toResponse() {
        return Response.builder()
                .withStatus(status)
                .withBody(message)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Status status;
        private String message;

        public Builder withStatus(Status status) {
            this.status = status;
            return this;
        }

        public Builder withMessage(String message) {
            this.message = message;
            return this;
        }

        public RequestHandlerException build() {
            return new RequestHandlerException(status, message);
        }
    }
}
