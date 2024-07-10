package com.projectleo.scraper.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
        @JsonProperty("finish_reason")
        public String finishReason;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Message {
            public String role;
            public String content;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        @JsonProperty("prompt_tokens")
        public int promptTokens;
        @JsonProperty("completion_tokens")
        public int completionTokens;
        @JsonProperty("total_tokens")
        public int totalTokens;
    }

    public int getPromptTokens() {
        return usage.promptTokens;
    }

    public int getCompletionTokens() {
        return usage.completionTokens;
    }
}