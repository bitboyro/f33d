package ro.bitboy.f33d.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ro.bitboy.f33d.model.Message;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static html.H.*;

@Service
public class SseService {

    private static final int MAX_HISTORY = 200;

    private static final Map<String, String> LEVEL_COLOR = Map.of(
        "info",    "hsl(var(--foreground))",
        "success", "#00e676",
        "warn",    "#ffd600",
        "error",   "#ff4560"
    );
    private static final Map<String, String> LEVEL_GLYPH = Map.of(
        "info",    "·",
        "success", "✓",
        "warn",    "⚠",
        "error",   "✕"
    );
    private static final List<String> PALETTE = List.of(
        "#00e676", "#448aff", "#ffd600", "#ff4560", "#aa66ff",
        "#00bcd4", "#ff9800", "#e040fb"
    );

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final List<Message> history = new CopyOnWriteArrayList<>();

    public List<Message> getHistory() {
        return List.copyOf(history);
    }

    public Map<String, Long> getSourceCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Message msg : history) {
            counts.merge(msg.sender(), 1L, Long::sum);
        }
        return counts;
    }

    public List<String> getUniqueSenders() {
        return history.stream().map(Message::sender).distinct().collect(Collectors.toList());
    }

    public static String senderColor(String sender) {
        return PALETTE.get(Math.abs(sender.hashCode()) % PALETTE.size());
    }

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        Runnable remove = () -> emitters.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        return emitter;
    }

    public void broadcast(Message msg) {
        history.add(0, msg);
        if (history.size() > MAX_HISTORY) {
            history.subList(MAX_HISTORY, history.size()).clear();
        }
        String html = renderCard(msg);
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("message").data(html));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }

    public int connectedCount() {
        return emitters.size();
    }

    public String renderCard(Message msg) {
        String level = msg.level() != null ? msg.level() : "info";
        String glyph = LEVEL_GLYPH.getOrDefault(level, "·");
        String msgColor = LEVEL_COLOR.getOrDefault(level, "hsl(var(--foreground))");
        String srcColor = senderColor(msg.sender());

        return div()
            .attr("data-feed-row", "1")
            .attr("data-source", msg.sender().toLowerCase())
            .attr("style", "display:grid;grid-template-columns:120px 76px 1fr;gap:0 16px;padding:10px 20px;border-bottom:1px solid hsl(var(--border));animation:slideIn 0.2s ease")
            .add(
                span().add(text(msg.sender().toUpperCase()))
                    .attr("style", "color:" + srcColor + ";font-weight:500;font-size:12px;letter-spacing:0.05em;overflow:hidden;text-overflow:ellipsis;white-space:nowrap"),
                span().add(text(relativeTime(msg.timestamp())))
                    .attr("style", "color:hsl(var(--muted-foreground));font-size:11px;white-space:nowrap;padding-top:1px"),
                span()
                    .attr("style", "color:" + msgColor + ";font-size:13px;word-break:break-word;line-height:1.55").add(
                        span().add(text(glyph)).attr("style", "margin-right:8px;opacity:0.7;font-size:11px"),
                        text(msg.text())
                    )
            ).render();
    }

    static String relativeTime(Instant ts) {
        long secs = Duration.between(ts, Instant.now()).getSeconds();
        if (secs < 60) return "just now";
        if (secs < 3600) return (secs / 60) + "m ago";
        if (secs < 86400) return (secs / 3600) + "h ago";
        return (secs / 86400) + "d ago";
    }
}
