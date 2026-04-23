package ro.bitboy.f33d.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ro.bitboy.f33d.model.Message;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static html.H.*;

@Service
public class SseService {

    private static final int MAX_HISTORY = 200;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final List<Message> history = new CopyOnWriteArrayList<>();

    public List<Message> getHistory() {
        return List.copyOf(history);
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
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }

    public int connectedCount() {
        return emitters.size();
    }

    public String renderCard(Message msg) {
        return div().cls("message-card py-5 border-b border-border animate-in slide-in-from-top-2 duration-300").add(
            div().cls("flex items-center gap-3 mb-2").add(
                span().add(text(msg.sender())).cls("text-xs font-bold uppercase tracking-widest text-primary"),
                span().add(text(relativeTime(msg.timestamp()))).cls("text-xs text-muted-foreground tabular-nums")
            ),
            p().add(text(msg.text())).cls("text-lg font-mono leading-relaxed whitespace-pre-wrap break-words")
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
