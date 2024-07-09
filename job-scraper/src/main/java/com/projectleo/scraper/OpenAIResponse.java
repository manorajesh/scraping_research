package com.projectleo.scraper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAIResponse {
    public String id;
    public String object;
    public long created;
    public String model;
    public List<Choice> choices;
    public Usage usage;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        public int index;
        public Message message;
        public String finishReason;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Message {
            public String role;
            public String content;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        public int promptTokens;
        public int completionTokens;
        public int totalTokens;
    }
}