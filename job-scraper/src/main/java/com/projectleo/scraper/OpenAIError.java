package com.projectleo.scraper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAIError {
    public String message;
    public String type;
    public String param;
    public String code;

    // Getters and Setters
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

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
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
        return "OpenAIError{" +
                "message='" + message + '\'' +
                ", type='" + type + '\'' +
                ", param='" + param + '\'' +
                ", code='" + code + '\'' +
                '}';
    }
}
