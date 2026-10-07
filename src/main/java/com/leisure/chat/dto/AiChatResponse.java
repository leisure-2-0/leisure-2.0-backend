package com.leisure.chat.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AiChatResponse.Token.class, name = "token"),
        @JsonSubTypes.Type(value = AiChatResponse.Done.class, name = "done")
})
public sealed interface AiChatResponse permits AiChatResponse.Token, AiChatResponse.Done {

    record Token(String content) implements AiChatResponse {}

    record Done(List<Long> postIds) implements AiChatResponse {}
}
