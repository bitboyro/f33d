package ro.bitboy.f33d.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ro.bitboy.f33d.model.Message;
import ro.bitboy.f33d.service.SseService;
import ro.bitboy.f33d.service.TokenService;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class MessageController {

    private final SseService sseService;
    private final TokenService tokenService;

    public MessageController(SseService sseService, TokenService tokenService) {
        this.sseService = sseService;
        this.tokenService = tokenService;
    }

    @PostMapping("/message")
    public ResponseEntity<Map<String, String>> postMessage(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Token", required = false) String xToken,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String sender = resolveSender(authHeader, xToken, authentication);
        if (sender == null) {
            return ResponseEntity.status(401).body(Map.of("error", "missing or invalid token"));
        }

        String text = body.getOrDefault("message", body.get("text"));
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message is required"));
        }

        Message msg = new Message(UUID.randomUUID().toString(), sender, text.trim(), Instant.now());
        sseService.broadcast(msg);
        return ResponseEntity.ok(Map.of("status", "delivered", "id", msg.id()));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return sseService.createEmitter();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok", "connected", sseService.connectedCount());
    }

    private String resolveSender(String authHeader, String xToken, Authentication authentication) {
        // Keycloak JWT path — principal is a Jwt
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String username = jwt.getClaimAsString("preferred_username");
            return username != null ? username : jwt.getSubject();
        }

        // Local token path
        String token = extractBearerToken(authHeader);
        if (token == null) token = xToken;
        if (token == null) return null;

        Optional<String> name = tokenService.resolveName(token);
        return name.orElse(null);
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }
}
