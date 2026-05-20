package com.ecom.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecom.service.ChatService;
import com.ecom.service.ChatService.ChatResponse;

/**
 * REST endpoint for the Shopping Assistant chatbot.
 * Accessible without login (SecurityConfig permits /**).
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "").trim();
        if (message.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        ChatResponse response = chatService.processMessage(message);
        return ResponseEntity.ok(response);
    }
}
