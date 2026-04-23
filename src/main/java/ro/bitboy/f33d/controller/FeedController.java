package ro.bitboy.f33d.controller;

import html.Page;
import html.components.ThemeToggle;
import html.css.ShadcnTheme;
import html.spring.HtmxController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ro.bitboy.f33d.service.SseService;

import static html.H.*;

@Controller
public class FeedController extends HtmxController {

    private final SseService sseService;

    public FeedController(SseService sseService) {
        this.sseService = sseService;
    }

    @GetMapping(value = "/", produces = "text/html")
    public ResponseEntity<String> index(HttpServletRequest request) {
        ShadcnTheme darkTheme = ShadcnTheme.defaultDark()
            .primary("142 71% 45%")       // green accent
            .background("0 0% 4%")
            .foreground("0 0% 91%")
            .card("0 0% 7%")
            .border("0 0% 14%")
            .mutedForeground("0 0% 35%");

        var content = div().cls("min-h-screen flex flex-col font-mono").add(
            header(),
            feedSection(sseService),
            statusScript()
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
                .head(
                    style("""
                        body { background: hsl(var(--background)); color: hsl(var(--foreground)); }
                        .message-card { transition: opacity 0.2s; }
                        @keyframes slideIn {
                          from { opacity: 0; transform: translateY(-8px); }
                          to   { opacity: 1; transform: translateY(0); }
                        }
                        .message-card { animation: slideIn 0.25s ease-out; }
                    """)
                )
                .title("f33d")
                .body(content)
        );
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

    private static html.elements.EmptyBodyElement<?> header() {
        return el("header").cls("border-b border-border px-4 sm:px-8 py-4 flex items-center justify-between").add(
            div().cls("flex items-baseline gap-3").add(
                h1().cls("text-5xl sm:text-7xl font-black tracking-tighter leading-none").add(
                    raw("f<span style='color:hsl(var(--primary))'>3</span><span style='color:hsl(var(--primary))'>3</span>d")
                ),
                span("feed").cls("text-xs uppercase tracking-widest text-muted-foreground hidden sm:inline")
            ),
            div().cls("flex items-center gap-2").add(
                div().id("status-dot")
                    .cls("w-2 h-2 rounded-full bg-muted-foreground transition-colors duration-300"),
                span().id("status-label").add(text("connecting"))
                    .cls("text-xs uppercase tracking-widest text-muted-foreground")
            )
        );
    }

    private static html.elements.EmptyBodyElement<?> feedSection(SseService sseService) {
        var messages = sseService.getHistory();

        var feed = div().id("feed")
            .attr("sse-swap", "message")
            .hxSwap("afterbegin")
            .cls("max-w-3xl mx-auto");

        if (messages.isEmpty()) {
            feed.add(div().id("empty-state")
                .cls("flex flex-col items-center justify-center py-24 text-muted-foreground gap-3").add(
                    div("—").cls("text-4xl"),
                    span("waiting for messages").cls("text-sm tracking-wide"),
                    code("POST /api/message  Authorization: Bearer <token>")
                        .cls("text-xs bg-card border border-border px-3 py-2 rounded mt-2")
                ));
        } else {
            messages.forEach(msg -> feed.add(raw(sseService.renderCard(msg))));
        }

        return div()
            .attr("hx-ext", "sse")
            .attr("sse-connect", "/api/stream")
            .cls("flex-1 px-4 sm:px-8 py-4").add(feed);
    }

    private static html.elements.EmptyBodyElement<?> statusScript() {
        return script("""
            (function() {
              var dot = document.getElementById('status-dot');
              var lbl = document.getElementById('status-label');

              document.body.addEventListener('htmx:sseOpen', function() {
                dot.style.backgroundColor = 'hsl(var(--primary))';
                lbl.textContent = 'live';
              });
              document.body.addEventListener('htmx:sseError', function() {
                dot.style.backgroundColor = 'hsl(var(--destructive))';
                lbl.textContent = 'reconnecting';
              });
              document.body.addEventListener('htmx:sseMessage', function() {
                var e = document.getElementById('empty-state');
                if (e) e.remove();
              });
            })();
        """);
    }
}
