package com.algomentor.model;

/** One message in a live AI chat conversation. role is "user" or "assistant". */
public record ChatMessage(String role, String content) {
}
