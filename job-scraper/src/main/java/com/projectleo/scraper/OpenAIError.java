package com.projectleo.scraper;

public class OpenAIError {
    public static class Error {
        private String message;
        private String type;
        private Object param;
        private String code;

        // Getters and setters
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Object getParam() {
            return param;
        }

        public void setParam(Object param) {
            this.param = param;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return "Error{" +
                    "message='" + message + '\'' +
                    ", type='" + type + '\'' +
                    ", param=" + param +
                    ", code='" + code + '\'' +
                    '}';
        }
    }

    private Error error;

    // Getters and setters
    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "OpenAIError{" +
                "error=" + error.toString() +
                '}';
    }
}
