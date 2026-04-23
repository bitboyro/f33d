package ro.bitboy.f33d.controller;

import html.Page;
import html.components.ThemeToggle;
import html.css.ShadcnTheme;
import html.spring.HtmxController;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ro.bitboy.f33d.service.SseService;
import ro.bitboy.f33d.service.TokenService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static html.H.*;

@Controller
public class FeedController extends HtmxController {

    private final SseService sseService;
    private final TokenService tokenService;
    private final String feedCss;
    private final String feedJs;

    public FeedController(SseService sseService, TokenService tokenService) throws IOException {
        this.sseService = sseService;
        this.tokenService = tokenService;
        this.feedCss = new ClassPathResource("feed.css").getContentAsString(StandardCharsets.UTF_8);
        this.feedJs  = new ClassPathResource("feed.js").getContentAsString(StandardCharsets.UTF_8);
    }

    @GetMapping(value = "/", produces = "text/html")
    public ResponseEntity<String> index() {
        ShadcnTheme darkTheme = ShadcnTheme.defaultDark()
            .primary("142 71% 45%")
            .background("0 0% 4%")
            .foreground("0 0% 91%")
            .card("0 0% 7%")
            .border("0 0% 14%")
            .mutedForeground("0 0% 35%");

        var content = div()
            .attr("style", "display:flex;flex-direction:column;height:100vh;overflow:hidden")
            .add(
                appHeader(),
                div().attr("style", "display:flex;flex:1;overflow:hidden").add(
                    sidebar(sseService, tokenService),
                    feedMain(sseService)
                ),
                script(feedJs)
            );

        return page(
            Page.defaults()
                .sseExt(true)
                .shadcn(darkTheme)
                .headFirst(
                    ThemeToggle.tailwindConfig(),
                    ThemeToggle.darkStyles(darkTheme),
                    ThemeToggle.initScript()
                )
                .head(style(feedCss))
                .title("f33d")
                .body(content)
        );
    }

    private static html.elements.EmptyBodyElement<?> appHeader() {
        return el("header")
            .attr("style", "height:48px;border-bottom:1px solid hsl(var(--border));padding:0 20px;display:flex;align-items:center;gap:20px;flex-shrink:0;background:hsl(var(--background))")
            .add(
                // Logo
                el("h1")
                    .attr("style", "font-weight:700;font-size:20px;letter-spacing:-0.02em;user-select:none;line-height:1;flex-shrink:0")
                    .add(raw("f<span style='color:hsl(var(--primary))'>33</span>d")),
                // Search
                div()
                    .attr("style", "flex:1;max-width:360px;position:relative")
                    .add(
                        span("/").attr("style", "position:absolute;left:10px;top:50%;transform:translateY(-50%);color:hsl(var(--muted-foreground));font-size:11px;pointer-events:none"),
                        input().type("text").id("search-input").placeholder("filter messages…")
                            .attr("style", "width:100%;background:hsl(var(--card));border:1px solid hsl(var(--border));color:hsl(var(--foreground));font-family:inherit;font-size:12px;padding:5px 10px 5px 24px;transition:border-color 0.15s")
                    ),
                // Spacer
                div().attr("style", "flex:1"),
                // Count
                span().id("msg-count")
                    .attr("style", "font-size:11px;color:hsl(var(--muted-foreground));white-space:nowrap"),
                // Pause button
                el("button").id("pause-btn")
                    .attr("style", "background:none;border:1px solid hsl(var(--border));color:hsl(var(--muted-foreground));font-family:inherit;font-size:10px;letter-spacing:0.12em;padding:4px 10px;cursor:pointer;text-transform:uppercase;transition:all 0.15s")
                    .add(text("PAUSE")),
                // Live indicator
                div()
                    .attr("style", "display:flex;align-items:center;gap:6px;font-size:11px;color:hsl(var(--primary));letter-spacing:0.12em;flex-shrink:0")
                    .add(
                        span().id("live-dot")
                            .attr("style", "width:7px;height:7px;border-radius:50%;background:hsl(var(--muted-foreground));flex-shrink:0"),
                        span().id("live-label").add(text("CONNECTING"))
                            .attr("style", "color:hsl(var(--muted-foreground))")
                    )
            );
    }

    private static html.elements.EmptyBodyElement<?> sidebar(SseService sseService, TokenService tokenService) {
        Map<String, Long> counts = sseService.getSourceCounts();
        List<String> senders = sseService.getUniqueSenders();
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        Map<String, String> nameToToken = tokenService.getNameToToken();

        var sourcesSection = div()
            .attr("style", "padding:14px 16px 10px;border-bottom:1px solid hsl(var(--border))")
            .add(
                div("SOURCES").attr("style", "font-size:9px;letter-spacing:0.2em;color:hsl(var(--muted-foreground));text-transform:uppercase;margin-bottom:10px"),
                div()
                    .attr("class", "src-item active")
                    .attr("data-source-filter", "")
                    .add(
                        span("ALL").attr("class", "src-label").attr("style", "font-size:11px;color:hsl(var(--foreground))"),
                        span(String.valueOf(total)).attr("style", "font-size:10px;color:hsl(var(--muted-foreground))")
                    )
            );

        for (String sender : senders) {
            String color = SseService.senderColor(sender);
            long count = counts.getOrDefault(sender, 0L);
            String truncated = sender.length() > 10 ? sender.substring(0, 9) + "…" : sender;
            sourcesSection.add(
                div()
                    .attr("class", "src-item")
                    .attr("data-source-filter", sender.toLowerCase())
                    .add(
                        span(truncated).attr("class", "src-label").attr("style", "font-size:11px;color:" + color + ";overflow:hidden;text-overflow:ellipsis;white-space:nowrap;max-width:120px"),
                        span(String.valueOf(count)).attr("style", "font-size:10px;color:hsl(var(--muted-foreground))")
                    )
            );
        }

        var levelsSection = div()
            .attr("style", "padding:14px 16px")
            .add(
                div("LEVELS").attr("style", "font-size:9px;letter-spacing:0.2em;color:hsl(var(--muted-foreground));text-transform:uppercase;margin-bottom:10px"),
                levelRow("·", "hsl(var(--foreground))", "info"),
                levelRow("✓", "#00e676", "success"),
                levelRow("⚠", "#ffd600", "warn"),
                levelRow("✕", "#ff4560", "error")
            );

        var tokensSection = div()
            .attr("style", "padding:14px 16px;border-top:1px solid hsl(var(--border))")
            .add(div("TOKENS").attr("style", "font-size:9px;letter-spacing:0.2em;color:hsl(var(--muted-foreground));text-transform:uppercase;margin-bottom:10px"));

        for (Map.Entry<String, String> entry : nameToToken.entrySet()) {
            String name = entry.getKey();
            String token = entry.getValue();
            String color = SseService.senderColor(name);
            tokensSection.add(
                div().attr("style", "margin-bottom:8px").add(
                    div(name).attr("style", "font-size:9px;color:" + color + ";letter-spacing:0.05em;margin-bottom:2px"),
                    div(token.substring(0, Math.min(12, token.length())) + "…")
                        .attr("style", "font-size:9px;color:hsl(var(--muted-foreground));letter-spacing:0.03em;cursor:pointer")
                        .attr("title", "Click to copy token")
                        .attr("data-token", token)
                        .attr("onclick", "var el=this,t=el.getAttribute('data-token'),p=el.textContent;navigator.clipboard.writeText(t).then(function(){el.textContent='copied!';setTimeout(function(){el.textContent=p},1200)})")
                )
            );
        }

        return div()
            .attr("style", "width:200px;border-right:1px solid hsl(var(--border));display:flex;flex-direction:column;flex-shrink:0;overflow-y:auto")
            .add(sourcesSection, levelsSection, div().attr("style", "flex:1"), tokensSection);
    }

    private static html.elements.EmptyBodyElement<?> levelRow(String glyph, String color, String label) {
        return div()
            .attr("style", "display:flex;align-items:center;gap:8px;margin-bottom:7px;font-size:11px;color:hsl(var(--muted-foreground))")
            .add(
                span(glyph).attr("style", "color:" + color + ";font-size:12px;width:12px"),
                text(label)
            );
    }

    private static html.elements.EmptyBodyElement<?> feedMain(SseService sseService) {
        var messages = sseService.getHistory();

        var columnHeaders = div()
            .attr("style", "display:grid;grid-template-columns:120px 76px 1fr;gap:0 16px;padding:8px 20px;border-bottom:1px solid hsl(var(--border));flex-shrink:0;background:hsl(var(--background))")
            .add(
                span("SOURCE").attr("style", "font-size:9px;letter-spacing:0.2em;color:hsl(var(--muted-foreground));text-transform:uppercase"),
                span("TIME").attr("style", "font-size:9px;letter-spacing:0.2em;color:hsl(var(--muted-foreground));text-transform:uppercase"),
                span("MESSAGE").attr("style", "font-size:9px;letter-spacing:0.2em;color:hsl(var(--muted-foreground));text-transform:uppercase")
            );

        var feed = div().id("feed")
            .attr("sse-swap", "message")
            .hxSwap("afterbegin");

        if (messages.isEmpty()) {
            feed.add(div().id("empty-state")
                .attr("style", "padding:60px 20px;text-align:center;color:hsl(var(--muted-foreground));font-size:12px")
                .add(
                    div("—").attr("style", "font-size:32px;margin-bottom:12px"),
                    span("waiting for messages").attr("style", "display:block;margin-bottom:8px"),
                    code("POST /api/message  Authorization: Bearer <token>")
                        .attr("style", "font-size:11px;background:hsl(var(--card));border:1px solid hsl(var(--border));padding:6px 12px;display:inline-block;margin-top:8px")
                ));
        } else {
            messages.forEach(msg -> feed.add(raw(sseService.renderCard(msg))));
        }

        var scrollable = div()
            .id("feed-container")
            .attr("style", "flex:1;overflow-y:auto;display:flex;flex-direction:column")
            .add(feed);

        return div()
            .attr("hx-ext", "sse")
            .attr("sse-connect", "/api/stream")
            .attr("style", "flex:1;display:flex;flex-direction:column;overflow:hidden")
            .add(columnHeaders, scrollable);
    }

    @GetMapping(value = "/login", produces = "text/html")
    public ResponseEntity<String> loginPage(@RequestParam(required = false) String error,
                                            @RequestParam(required = false) String logout) {
        ShadcnTheme darkTheme = ShadcnTheme.defaultDark()
            .primary("142 71% 45%")
            .background("0 0% 4%")
            .foreground("0 0% 91%")
            .card("0 0% 7%")
            .border("0 0% 14%")
            .mutedForeground("0 0% 35%");

        var formContent = div().cls("min-h-screen flex items-center justify-center font-mono p-4").add(
            div().cls("w-full max-w-sm space-y-8").add(
                div().cls("text-center").add(
                    h1().cls("text-6xl font-black tracking-tighter").add(
                        raw("f<span style='color:hsl(var(--primary))'>3</span><span style='color:hsl(var(--primary))'>3</span>d")
                    ),
                    p("your personal feed").cls("text-sm text-muted-foreground mt-2 tracking-widest uppercase")
                ),
                error != null
                    ? div("invalid credentials").cls("text-sm text-center text-destructive bg-destructive/10 rounded-md p-3 border border-destructive/20")
                    : raw(""),
                logout != null
                    ? div("signed out").cls("text-sm text-center text-muted-foreground bg-muted/20 rounded-md p-3 border border-border")
                    : raw(""),
                form().action("/login").method("POST").cls("space-y-4").add(
                    div().cls("space-y-2").add(
                        label("username").forAttr("username").cls("text-xs uppercase tracking-widest text-muted-foreground"),
                        input().type("text").name("username").id("username")
                            .cls("w-full px-4 py-3 bg-card border border-border rounded-md focus:outline-none focus:ring-1 focus:ring-primary text-sm font-mono")
                            .autofocus()
                    ),
                    div().cls("space-y-2").add(
                        label("password").forAttr("password").cls("text-xs uppercase tracking-widest text-muted-foreground"),
                        input().type("password").name("password").id("password")
                            .cls("w-full px-4 py-3 bg-card border border-border rounded-md focus:outline-none focus:ring-1 focus:ring-primary text-sm font-mono")
                    ),
                    button("sign in").type("submit")
                        .cls("w-full py-3 bg-primary text-primary-foreground font-bold uppercase tracking-widest text-sm rounded-md hover:bg-primary/90 transition-colors")
                )
            )
        );

        return page(
            Page.defaults()
                .shadcn(darkTheme)
                .headFirst(
                    ThemeToggle.tailwindConfig(),
                    ThemeToggle.darkStyles(darkTheme),
                    ThemeToggle.initScript()
                )
                .head(style("body { background: hsl(var(--background)); color: hsl(var(--foreground)); }"))
                .title("f33d — sign in")
                .body(formContent)
        );
    }

}
