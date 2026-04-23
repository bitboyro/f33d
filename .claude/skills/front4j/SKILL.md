---
name: front4j
description: Build server-side rendered HTML using the front4j library — controllers, pages, components, fragments, forms, tables, chat, modals, or any HTML output in this Java project. Do not trigger for pure backend code (services, repositories, domain logic) that produces no HTML.
---

## Role and rules

You are an expert front4j developer. Apply these rules without exception:

1. **Never read source files to find API.** Everything is documented below. Use `H.el("tag")` / `H.emptyEl("tag")` for any tag not listed, or ask the user.
2. **Never generate HTML strings.** Use the fluent Java API exclusively — no template strings, no concatenation.
3. **Never call constructors directly.** Use factory methods: `H.div()`, not `new Div()`.
4. **Never call `.render()` mid-chain.** Only at the final step.
5. **User input always goes through `text()`.** Never pass user data to element constructors or `raw()`.
6. **Always add `import static html.H.*;`** as the primary import.
7. **Prefer built-in components** (Toast, Modal, DataTable, LlmChat, etc.) over hand-building the same thing.
8. **For Spring endpoints**, extend `HtmxController` and return `fragment()` for htmx requests, `page()` for full-page requests.

---

## Setup

```java
// Primary import — gives access to all element factories
import static html.H.*;

// Other common imports
import html.Page;
import html.BaseStyles;
import html.Component;
import html.elements.EmptyBodyElement;
import html.elements.Elements;
import html.components.*;
import html.components.elicit.*;
import html.spring.*;
import html.css.ShadcnTheme;
```

Maven dependency:
```xml
<dependency>
    <groupId>ro.bitboy</groupId>
    <artifactId>front4j</artifactId>
    <version>0.4.4</version>
</dependency>
```

---

## Element factory — H.*

### Elements with content (closing tag)
```java
div(), div("text")         span(), span("text")       p(), p("text")
a(), a("text")             button(), button("Click")  form(), form(content)
h1("Title"), h2(), h3(), h4()
ul(), ol(), li("item")     table(), tr(), td("cell"), th("header")
nav(), header(), footer(), main(), section(), article()
details(), summary("text") code("x"), pre("x")
label("text"), select(), option("text"), textarea()
script(), script("js")     style(), style("css")      title("App")
iframe(), i("icon"), small("text"), progress()
body(), head(), html()

// Added in 0.3.0
strong(), strong("bold")   em(), em("italic")         b(), b("bold")
u(), u("underline")        mark(), mark("highlight")
aside(), figure(), figcaption(), figcaption("caption")
video(), audio(), picture(), canvas()
dl(), dt(), dt("term")     dd(), dd("definition")
fieldset(), legend(), legend("title")  optgroup(), optgroup("label")
tbody(), thead(), tfoot()  caption(), caption("text")
time(), time("date")       abbr(), abbr("abbr")       address(), address("addr")
sub(), sub("sub")          sup(), sup("sup")
```

### Void elements (no closing tag)
```java
input(), br(), hr(), img(), source()
```

### Generic / dynamic tags
```java
el("tag-name")             // <tag-name>...</tag-name>
el("tag-name", content)
emptyEl("meta")            // <meta ...>  (void)
```

### Text and collections
```java
text("user input")         // ALWAYS use this for user-supplied data — HTML-escapes automatically
raw("<b>trusted</b>")      // NEVER pass user input — bypasses all escaping
elements(el1, el2, el3)    // Elements collection
```

---

## Fluent attribute API

Every element returns `SELF` for chaining:

```java
.id("my-id")
.cls("tailwind classes here")
.style("color: red;")
.attr("data-x", "value")          // arbitrary attribute
.href("/url")                      .src("/img.png")
.name("fieldName")                 .type("text")
.value("default")                  .placeholder("hint...")
.disabled()                        .target("_blank")
.rel("noopener")                   .role("button")
.forAttr("input-id")               // HTML 'for' attribute
```

`href()` and `src()` reject `javascript:`, `data:`, and `vbscript:` schemes automatically.

## Fluent htmx API

```java
.hxGet("/url")             .hxPost("/url")         .hxPut("/url")
.hxDelete("/url")          .hxPatch("/url")
.hxTarget("#selector")     .hxSwap("outerHTML")    .hxTrigger("click")
.hxInclude("#form")        .hxIndicator("#spinner").hxPushUrl(true)
.hxExt("json-enc")         .hxEncoding("multipart/form-data")

// Added in 0.3.0
.hxVals("{\"key\":\"val\"}")   .hxBoost(true)          .hxPrompt("Are you sure?")
.hxSelect("#fragment")         .hxSelectOob("#other")  .hxSwapOob("outerHTML:#target")
.hxDisable()                   .hxDisabledElt("#btn")  .hxPreserve()
.hxSync("#form:queue")         .hxHistory("false")     .hxConfirm("Delete?")
.hxReplaceUrl(true)            .hxOn("click", "alert('hi')")
```

Swap values: `innerHTML` | `outerHTML` | `beforebegin` | `afterbegin` | `beforeend` | `afterend` | `delete` | `none`

Trigger examples: `"click"` | `"change"` | `"blur"` | `"submit"` | `"revealed"` | `"load"` | `"every 5s"`

## Fluent form/media attributes

```java
.action("/submit")         .method("POST")         .autocomplete("off")
.maxLength(100)            .minLength(3)           .min("0")
.max("100")                .step("0.5")            .pattern("[A-Z]+")
.readonly()                .checked()              .selected()
.autofocus()               .novalidate()           .multiple()
.required()                .accept("image/*")      .alt("description")
.width("640")              .height("480")          .tabindex(1)
.draggable(true)           .contenteditable(true)
```

---

## Composing elements

```java
// Inline chain
button("Save").cls("btn").hxPost("/save").hxSwap("outerHTML")

// Build then add
Div card = div().cls("card p-4 border rounded");
card.add(
    h3("Title").cls("font-bold text-lg"),
    p("Description").cls("text-muted-foreground"),
    button("Action").hxPost("/action").hxTarget("#result").hxSwap("innerHTML")
);

// Varargs
div().add(h1("Title"), p("Body"), button("Go"))

// Collection
Elements items = Elements.of(li("One"), li("Two"), li("Three"));
ul().cls("list-disc pl-4").add(items)
```

## Rendering

```java
element.render()      // compact HTML — use for production / controller returns
element.beautify()    // indented — use only for debugging
```

---

## Page builder

```java
// Standard page — htmx + Tailwind + ShadCN CSS variables included
Page.defaults()
    .title("My App")
    .lang("en")
    .headFirst(
        ThemeToggle.tailwindConfig(),  // MUST precede Tailwind CDN — use headFirst()
        ThemeToggle.darkStyles(),
        ThemeToggle.initScript()       // prevents dark-mode flash on navigation
    )
    .head(
        Toast.script(),              // register toast listener (if using Toast)
        BaseStyles.defaults()        // opt-in: body, typography, lists, forms, interactive
    )
    .body(content)
    .render();

// No CDN at all
Page.bare()
    .head(emptyEl("link").rel("stylesheet").href("/app.css"))
    .body(content)
    .render();

// Enable SSE extension (required for LlmChat)
Page.defaults().sseExt(true).body(LlmChat.of(...).render()).render();

// Enable json-enc (required for ElicitForm / ElicitTabs)
Page.defaults().jsonEnc(true).body(ElicitForm.of(...).render()).render();

// Custom CDN URLs (air-gapped / version-pinned)
Page.defaults()
    .htmxUrl("/assets/htmx.min.js")
    .tailwindUrl("/assets/tailwind.js")
    .render();

// Toggle individual CDN flags
Page.defaults().htmx(false).tailwind(true).shadcn(false).render();
```

---

## Component interface

```java
// Lambda (stateless inline)
Component card = () -> div().cls("card p-4")
        .add(h3("Title"), p("Body"));

// Class (parameterized / reusable)
public class ItemCard implements Component {
    private final String name;
    private final String description;
    public ItemCard(String name, String description) {
        this.name = name;
        this.description = description;
    }
    @Override
    public EmptyBodyElement<?> render() {
        return div().cls("rounded-lg border bg-card p-4").add(
            h3().add(text(name)).cls("font-semibold"),
            p().add(text(description)).cls("text-muted-foreground")
        );
    }
}

// Use in Elements
Elements cards = Elements.of(
    new ItemCard("Widget A", "First item"),
    new ItemCard("Widget B", "Second item")
);
div().cls("grid grid-cols-2 gap-4").add(cards)
```

---

## Spring controller

```java
@RestController
public class PageController extends HtmxController {

    @GetMapping(value = "/", produces = "text/html")
    public ResponseEntity<String> index(HttpServletRequest request) {
        var content = div().id("main").cls("container mx-auto p-8").add(
            h1("Welcome").cls("text-3xl font-bold"),
            p("Built with front4j").cls("text-muted-foreground mt-2")
        );

        if (isHtmxRequest(request)) return fragment(content);
        return page(Page.defaults().title("Home").body(content));
    }

    @PostMapping(value = "/save", produces = "text/html")
    public ResponseEntity<String> save() {
        return HtmxResponse.ok()
            .body(p("Saved successfully!").cls("text-green-600"))
            .trigger("showToast", Toast.success("Item saved").toJson())
            .pushUrl("/dashboard")
            .build();
    }

    @GetMapping(value = "/items", produces = "text/html")
    public ResponseEntity<String> items(HttpServletRequest request) {
        var fragment = div().id("items").add(/* ... */);
        return pageOrFragment(request,
            Page.defaults().title("Items").body(fragment),
            fragment);
    }
}
```

### HtmxResponse

```java
HtmxResponse.ok()
    .body(element)                              // or .body("raw html")
    .trigger("myEvent")                         // HX-Trigger: myEvent  (validated: [a-zA-Z0-9_-]+)
    .trigger("showToast", Toast.ok().toJson())  // HX-Trigger with JSON detail
    .redirect("/url")                           // HX-Redirect
    .pushUrl("/url")                            // HX-Push-Url
    .replaceUrl("/url")                         // HX-Replace-Url
    .refresh()                                  // HX-Refresh: true
    .retarget("#other")                         // HX-Retarget
    .reswap("innerHTML")                        // HX-Reswap
    .reselect("#inner")                         // HX-Reselect
    .location("/url")                           // HX-Location
    .build();
```

---

## Built-in components

### BaseStyles

Opt-in base stylesheet using ShadCN CSS variables — adapts to any theme.

```java
Page.defaults()
    .headFirst(
        ThemeToggle.tailwindConfig(),
        ThemeToggle.darkStyles(),
        ThemeToggle.initScript()
    )
    .head(BaseStyles.defaults())
    .body(content)
    .render();
```

### Toast

Requires `Toast.script()` in page head (once). Auto-dismisses after 3s.

```java
Page.defaults().head(Toast.script()).body(...).render();

.trigger("showToast", Toast.success("Saved!").toJson())
.trigger("showToast", Toast.error("Failed").toJson())
.trigger("showToast", Toast.info("Note: ...").toJson())
.trigger("showToast", Toast.warning("Careful!").toJson())
```

### Modal

```java
Modal.trigger("Open Dialog", "/modal/load").renderTrigger()

Modal.create()
    .title("Confirm Delete")
    .body(div().add(
        p("This action cannot be undone."),
        div().cls("flex gap-2 mt-4").add(
            button("Delete").cls("px-4 py-2 bg-destructive text-destructive-foreground rounded-md")
                .hxPost("/items/delete").hxTarget("#items").hxSwap("innerHTML"),
            button("Cancel").cls("px-4 py-2 border rounded-md")
                .hxDelete("/modal/close").hxTarget("#modal-overlay").hxSwap("outerHTML")
        )
    ))
    .closeUrl("/modal/close")
    .render()

// DELETE /modal/close
HtmxResponse.ok().build()
```

### Tabs

```java
Tabs.of(
    Tabs.Tab.of("Overview", div().add(p("Overview content"))),
    Tabs.Tab.of("Settings", div().add(p("Settings content"))),
    Tabs.Tab.of("Activity", div().add(p("Activity log")))
)
.hxRoute("/tabs")
.contentId("tab-content")
.render();
```

### ValidatedForm

Field names must match `[a-zA-Z0-9_-]+`. Each field validates on blur via `POST {action}/validate/{fieldName}`.

```java
ValidatedForm.post("/register")
    .field(ValidatedForm.FormField.text("username")
        .label("Username").placeholder("Enter username").required().minLength(3))
    .field(ValidatedForm.FormField.email("email")
        .label("Email address").placeholder("you@example.com").required())
    .field(ValidatedForm.FormField.password("password")
        .label("Password").required().minLength(8))
    .submit("Create Account")
    .render();
```

### ThemeToggle

```java
// Use headFirst() so tailwindConfig and initScript run before Tailwind CDN loads
// (prevents dark-mode flash on page navigation)
Page.defaults()
    .headFirst(
        ThemeToggle.tailwindConfig(),   // MUST precede Tailwind CDN
        ThemeToggle.darkStyles(),
        ThemeToggle.initScript()        // prevents flash of light mode
    )
    .head(BaseStyles.defaults())
    .body(
        nav().cls("flex justify-between items-center p-4 border-b").add(
            a("/").cls("text-xl font-bold").add(text("My App")),
            ThemeToggle.render()
        ),
        main().cls("container mx-auto p-8").add(content)
    )
    .render();

// Custom themes
ShadcnTheme light = ShadcnTheme.defaultLight().primary("262 83% 58%");
ShadcnTheme dark  = ShadcnTheme.defaultDark().primary("262 83% 70%");
Page.defaults()
    .shadcn(light)
    .headFirst(ThemeToggle.tailwindConfig(light), ThemeToggle.darkStyles(dark), ThemeToggle.initScript())
    .render();
```

### DataTable

```java
Elements rows = Elements.of(
    tr().add(td("Alice"), td("Active"), td("2026-01-01")),
    tr().add(td("Bob"),   td("Inactive"), td("2026-01-15"))
);

DataTable.of("/items", List.of("Name", "Status", "Date"), rows)
    .sortable("Name", "Date")
    .paginate(20)
    .currentPage(2)
    .totalRows(85)
    .id("my-table")
    .render()
```

Server endpoint:

```java
@GetMapping("/items")
public ResponseEntity<String> items(
        @RequestParam(defaultValue = "Name") String sort,
        @RequestParam(defaultValue = "1") int page) {
    int pageSize = 20;
    Elements rows = /* fetch Tr elements */;
    int total = /* total row count */;
    return fragment(
        DataTable.of("/items", List.of("Name", "Status", "Date"), rows)
            .sortable("Name", "Date").paginate(pageSize).currentPage(page).totalRows(total).render()
    );
}
```

### SearchableList

```java
SearchableList.of("/search", Elements.of(li("Item 1"), li("Item 2"), li("Item 3")))
    .placeholder("Search items...")
    .inputName("q")
    .resultId("results")
    .debounce(300)
    .render()
```

### InfiniteScroll

```java
InfiniteScroll.of("/items?page=2", Elements.of(li("Item 1"), li("Item 2"))).render()

@GetMapping("/items")
public ResponseEntity<String> items(@RequestParam(defaultValue = "1") int page) {
    Elements batch = /* items for this page */;
    boolean hasMore = page < totalPages;
    if (hasMore) return fragment(InfiniteScroll.of("/items?page=" + (page + 1), batch).render());
    return fragment(div().add(batch));
}
```

---

## AI tools

### LlmChat — SSE-driven chat

```java
Page.defaults()
    .sseExt(true)
    .jsonEnc(true)
    .head(Toast.script())
    .body(
        LlmChat.of("/chat/stream", "/chat/message")
            .placeholder("Ask me anything...")
            .sendLabel("Send")
            .height("h-[600px]")
            .chatId("main")
            .showThinking(true)
            .header(div().cls("p-4 border-b").add(h2("Assistant").cls("font-semibold")))
            .footer(p("Powered by Claude").cls("text-xs text-muted-foreground text-center p-2"))
            .render()
    )
    .render();
```

### LlmApiChat — API-driven chat

Use when the MCP/LLM client lives in a separate service and returns complete string responses.
No SSE extension required — works with plain `Page.defaults()`.

```java
Page.defaults()
    .body(
        LlmApiChat.of("/chat/message")
            .placeholder("Ask me anything...")
            .sendLabel("Send")
            .height("h-[600px]")
            .chatId("main")
            .showThinking(true)
            .header(div().cls("p-4 border-b").add(h2("Assistant").cls("font-semibold")))
            .footer(p("Powered by Claude").cls("text-xs text-muted-foreground text-center p-2"))
            .render()
    )
    .render();

// Message endpoint — receives the user message, returns HTML appended to messages panel
@PostMapping("/chat/message")
public ResponseEntity<String> message(@RequestParam String message) {
    String assistantHtml = mcpClient.chat(message);  // call your external service
    return fragment(elements(
        ChatMessage.user(message),
        ChatMessage.assistant(assistantHtml)
    ));
}
```

Key differences from `LlmChat`:
- Only one URL argument (no stream URL)
- Form POSTs and targets the messages panel directly (`hx-target` + `hx-swap="beforeend"`)
- `showThinking(true)` uses htmx's `.htmx-indicator` CSS (hidden by default, visible during request)
- Auto-scrolls to the latest message after each response
- No `Page.defaults().sseExt(true)` needed

@GetMapping(value = "/chat/stream", produces = "text/event-stream")
public SseEmitter stream() {
    SseEmitter emitter = new SseEmitter(0L);
    executor.submit(() -> {
        try {
            emitter.send(SseEmitter.event().name("message")
                .data(ChatSseEvent.message("<p class='text-sm'>Hello!</p>")));

            emitter.send(SseEmitter.event().name("elicit")
                .data(ChatSseEvent.elicit(
                    ElicitForm.of("/chat/elicit").title("What do you need?")
                        .field(ElicitField.select("topic", "Topic", "Code", "Design")).render()
                )));

            emitter.send(SseEmitter.event().name("elicit").data(ChatSseEvent.clearElicit()));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    });
    return emitter;
}

// Raw SSE helpers (for reactive streams):
// ChatSseEvent.rawMessage(html)       → "event: message\ndata: ...\n\n"
// ChatSseEvent.rawElicit(component)   → "event: elicit\ndata: ...\n\n"
// ChatSseEvent.rawClose()             → "event: close\ndata: \n\n"

@PostMapping("/chat/message")
public ResponseEntity<String> message(@RequestParam String message) {
    return HtmxResponse.ok().build();
}

@PostMapping("/chat/elicit")
public ResponseEntity<String> elicit(@RequestBody String body) {
    // body = {"action":"accept","content":{"topic":"Code"}}  or  {"action":"cancel"}
    return HtmxResponse.ok().build();
}
```

### ElicitField

Field names must match `[a-zA-Z0-9_-]+`. All factories return `FieldBuilder` — call `.render()` when used standalone; pass directly to `ElicitForm.field()` / `ElicitTabs.tab()`.

```java
ElicitField.text("name", "Full name")
ElicitField.textarea("notes", "Additional notes")
ElicitField.select("role", "Role", "Developer", "Designer", "Manager")
ElicitField.multiSelect("skills", "Skills", "Java", "Python", "TypeScript")
ElicitField.confirm("agree", "Do you agree to the terms?")
ElicitField.file("resume", "Upload resume")
ElicitField.date("dob", "Date of birth")
ElicitField.time("meeting", "Meeting time")
ElicitField.dateTime("appointment", "Appointment date & time")
ElicitField.dateRange("from", "to", "Availability window")

.required()
.placeholder("hint text")
.defaultValue("default")
.render()   // only when used standalone
```

### ElicitForm — single-step

```java
ElicitForm.of("/chat/elicit")
    .title("Project details")
    .submitLabel("Continue")
    .field(ElicitField.text("name", "Project name").required().placeholder("e.g. My App"))
    .field(ElicitField.select("type", "Type", "Web app", "API", "CLI", "Mobile").required())
    .field(ElicitField.textarea("description", "Description").placeholder("What does it do?"))
    .render()
```

### ElicitTabs — multi-step

```java
ElicitTabs.of("/chat/elicit")
    .title("Onboarding")
    .submitLabel("Finish setup")
    .tab("About you",
        ElicitField.text("name", "Full name").required(),
        ElicitField.text("email", "Work email").required())
    .tab("Your project",
        ElicitField.select("type", "Project type", "Web", "Mobile", "Backend").required(),
        ElicitField.textarea("goals", "Goals").placeholder("What are you trying to achieve?"))
    .tab("Documents",
        ElicitField.file("brief", "Project brief"),
        ElicitField.dateRange("start", "end", "Timeline"))
    .render()
```

### ChatMessage

```java
ChatMessage.user("What is 2 + 2?")                                      // right-aligned; text escaped
ChatMessage.assistant("<p>The answer is <strong>4</strong>.</p>")        // left-aligned; raw HTML
ChatMessage.system("You are a helpful assistant.")                       // centered, muted; text escaped

emitter.send(SseEmitter.event().name("message")
    .data(ChatSseEvent.message(ChatMessage.assistant("<p>Hello!</p>").render())));
```

### ToolCall

```java
ToolCall.of("search_files")
    .input("{\"query\": \"authentication\"}")
    .output("<p>Found 3 results in src/auth/</p>")
    .render()
```

---

## Security rules (enforce always)

| What | Rule |
|------|------|
| User-supplied text | `H.text(userInput)` — always, no exceptions |
| Developer-controlled HTML | `H.raw(trustedHtml)` — never pass user data |
| Element constructors with user data | `p().add(text(userInput))` — NOT `p(userInput)` |
| Form / field names | Only `[a-zA-Z0-9_-]+` — enforced at construction |
| href / src URLs | `javascript:`, `data:`, `vbscript:` rejected automatically |
| HX-Trigger event names | Only `[a-zA-Z0-9_-]+` — enforced in `HtmxResponse.trigger()` |
| Element nesting depth | Default limit 512; configure: `Element.setMaxRenderDepth(N)` or `-Dfront4j.maxRenderDepth=N` |
| A2UI JSON depth | Default limit 64; configure: `A2UIComponent.setMaxJsonDepth(N)` or `-Dfront4j.maxJsonDepth=N` |

```java
// SAFE
String name = request.getParameter("name");
p().add(text(name))
li().add(text(item.getTitle()), span().add(text(item.getCategory())))
div().attr("data-name", userInput)    // attribute values are auto-escaped

// UNSAFE — never do this
p(userInput)           // String constructors don't escape
raw(userInput)         // explicitly bypasses escaping
```

---

## Common patterns

### Fragment vs full-page

```java
if (isHtmxRequest(request)) {
    return fragment(tableRows);
}
return page(Page.defaults().body(fullLayout));
```

### Inline CSS and scripts

```java
Page.bare().head(
    style("""
        .card { border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.1); }
    """)
)

script("""
    document.addEventListener('myEvent', (e) => { console.log(e.detail); });
""")
```

### Custom attributes (data-*, aria-*, Alpine.js, etc.)

```java
div().attr("data-controller", "dropdown").attr("data-action", "click->dropdown#toggle")
div().attr("x-data", "{ open: false }").attr("x-show", "open")
button().attr("aria-expanded", "false").attr("aria-controls", "menu")
```

### htmx polling

```java
div().id("live-count").hxGet("/count").hxTrigger("every 5s").hxSwap("innerHTML").add(text("Loading..."))
```

### Form with JSON encoding

```java
form()
    .hxPost("/api/items").hxExt("json-enc").hxSwap("none")
    .attr("hx-on::after-request", "this.reset()")
    .add(
        input().type("text").name("title").placeholder("Item title"),
        button("Add").type("submit")
    )
```

---

## Common Tailwind patterns

```java
// Layout
div().cls("flex items-center justify-between")
div().cls("grid grid-cols-3 gap-4")
div().cls("container mx-auto px-4 py-8")
div().cls("space-y-4")

// Typography
h1("Title").cls("text-2xl font-bold tracking-tight")
p("desc").cls("text-sm text-muted-foreground")

// Primary button
button("Save").cls("px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90")

// Ghost / outline button
button("Cancel").cls("px-4 py-2 border border-input rounded-md hover:bg-accent hover:text-accent-foreground")

// Card
div().cls("rounded-lg border bg-card p-6 shadow-sm")

// Form input
input().type("text").cls("w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-ring")
```

---

## A2UI Protocol Support (`html.a2ui`)

front4j can consume Google's A2UI protocol — an agent emits declarative JSON; your server converts it to styled HTML; the browser renders it with htmx.

```
Agent ──A2UI JSON──→ Server (front4j) ──HTML──→ Browser (htmx)
```

```java
import html.a2ui.A2UIComponent;
import html.a2ui.A2UIMessage;
import html.a2ui.A2UISurface;
import html.a2ui.A2UIConsumer;
```

### A2UIComponent

```java
A2UIComponent.of("btn1", "Button")
    .property("label", "Click me")
    .property("action", Map.of("event", Map.of("name", "submit_form")))
    .child("icon1")
    .children(List.of("c1", "c2"))

comp.getId()         comp.getComponent()
comp.getProperties() // unmodifiable Map
comp.getChildren()   // unmodifiable List<String>
comp.toJson()        // single-line JSON string

// Configure JSON depth guard (default: 64)
A2UIComponent.setMaxJsonDepth(128);  // or -Dfront4j.maxJsonDepth=128
```

### A2UIMessage

```java
new A2UIMessage.CreateSurface("main", "https://a2ui.org/catalogs/basic/v0.9")
new A2UIMessage.CreateSurface("main", catalogUrl, Map.of("--primary", "#2563eb"), true)
new A2UIMessage.UpdateComponents("main", List.of(comp1, comp2))
new A2UIMessage.UpdateDataModel("main", "/user/name", "Alice")
new A2UIMessage.DeleteSurface("main")

msg.toJson()  // single-line JSON for JSONL
```

### A2UISurface

```java
A2UISurface surface = A2UISurface.create("main")
    .catalog("https://a2ui.org/catalogs/basic/v0.9")
    .theme("--primary", "#2563eb")
    .theme(Map.of("--radius", "0.5rem"))
    .sendDataModel(true)
    .add(A2UIComponent.of("root", "Column").child("t1"))
    .add(A2UIComponent.of("t1", "Text").property("text", "Hello"));

surface.toMessages()  // List<A2UIMessage>: [CreateSurface, UpdateComponents]
surface.toJsonl()     // JSONL string
```

### A2UIConsumer

Takes a flat adjacency list with one component `id="root"`. Returns a styled front4j element tree.

```java
List<A2UIComponent> components = List.of(
    A2UIComponent.of("root", "Column").child("card1"),
    A2UIComponent.of("card1", "Card").property("title", "Welcome").child("msg").child("btn1"),
    A2UIComponent.of("msg", "Text").property("text", "Hello from the agent"),
    A2UIComponent.of("btn1", "Button").property("label", "Continue")
);

EmptyBodyElement<?> html = A2UIConsumer.render(components);
return fragment(html);

EmptyBodyElement<?> html = A2UIConsumer.render(surface);  // from a surface
```

**A2UI → HTML mappings:**

| A2UI type | front4j output | Key properties |
|---|---|---|
| `Text` | `<p>` | `text` |
| `Button` | `<button>` (ShadCN primary) | `label`, `action.event.name` → hx-post |
| `TextField` | `<label>` + `<input type=text>` | `label`, `placeholder`, `bindingPath` |
| `CheckBox` | `<input type=checkbox>` + `<label>` | `label`, `bindingPath` |
| `ChoicePicker` | `<select>` + `<option>`s | `label`, `options`, `bindingPath` |
| `Slider` | `<input type=range>` | `label`, `min`, `max`, `step`, `bindingPath` |
| `DateTimeInput` | `<input type=date/time/datetime-local>` | `label`, `type`, `bindingPath` |
| `Row` | `<div class="flex gap-2">` | children |
| `Column` | `<div class="flex flex-col gap-2">` | children |
| `Card` | bordered `<div>` with optional `<h3>` | `title`, children |
| `List` | `<ul>` with `<li>`-wrapped children | children |
| `Tabs` | tab buttons + panels | `tabLabels`, children |
| `Modal` | fixed overlay dialog | `title`, children |
| `Image` | `<img>` | `src`, `alt` |
| `Icon` | `<i class="...">` | `name` |
| `Divider` | `<hr>` | — |
| `Video` | `<video>` with `<source>` | `src` |
| `AudioPlayer` | `<audio>` with `<source>` | `src` |
| Unknown | dashed-border `<div data-a2ui-type="...">` | children |

### Spring controller pattern for A2UI

```java
@RestController
public class A2UIController extends HtmxController {

    @PostMapping(value = "/a2ui/render", consumes = "application/json")
    public ResponseEntity<String> renderA2UI(@RequestBody List<A2UIComponent> components) {
        return fragment(A2UIConsumer.render(components));
    }

    @GetMapping("/a2ui/stream")
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter();
        // EmptyBodyElement<?> html = A2UIConsumer.render(components);
        // emitter.send(SseEmitter.event().name("message").data(html.render()));
        return emitter;
    }
}
```
