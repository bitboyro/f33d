package ro.bitboy.f33d.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    @Value("${f33d.tokens-file:#{null}}")
    private String tokensFile;

    @Value("${f33d.clients:#{null}}")
    private String clients;

    private final Map<String, String> tokenToName = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (tokensFile != null && !tokensFile.isBlank()) {
            loadFromFile(Path.of(tokensFile));
        } else if (clients != null && !clients.isBlank()) {
            autoGenerate(clients.split(","));
        } else {
            autoGenerate(new String[]{"default"});
        }
    }

    private void loadFromFile(Path path) {
        try {
            Files.lines(path)
                .map(String::trim)
                .filter(l -> !l.isBlank() && !l.startsWith("#"))
                .forEach(line -> {
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String token = line.substring(0, eq).trim();
                        String name = line.substring(eq + 1).trim();
                        tokenToName.put(token, name);
                    }
                });
            System.out.println("f33d: loaded " + tokenToName.size() + " token(s) from " + path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load tokens file: " + path, e);
        }
    }

    private void autoGenerate(String[] names) {
        Map<String, String> generated = new LinkedHashMap<>();
        for (String name : names) {
            String n = name.trim();
            if (n.isEmpty()) continue;
            String token = UUID.randomUUID().toString().replace("-", "");
            tokenToName.put(token, n);
            generated.put(n, token);
        }
        int nameWidth = generated.keySet().stream().mapToInt(String::length).max().orElse(8);
        String sep = "─".repeat(nameWidth + 38);
        System.out.println("┌─ f33d  API tokens ─" + "─".repeat(Math.max(0, sep.length() - 20)) + "┐");
        generated.forEach((name, token) ->
            System.out.printf("│  %-" + nameWidth + "s   %s  │%n", name, token));
        System.out.println("└" + sep + "┘");
    }

    public Optional<String> resolveName(String token) {
        return Optional.ofNullable(tokenToName.get(token));
    }
}
