# AlgoMentor — An Interactive Algorithm Visualizer with a Live AI Tutor

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-27-orange">
  <img alt="JavaFX" src="https://img.shields.io/badge/UI-JavaFX%20%2B%20FXML-blue">
  <img alt="Build" src="https://img.shields.io/badge/Build-Maven-c71a36">
  <img alt="Database" src="https://img.shields.io/badge/DB-SQLite-003b57">
  <img alt="AI" src="https://img.shields.io/badge/AI-Groq%20%7C%20OpenAI-8a2be2">
  <img alt="JSON" src="https://img.shields.io/badge/JSON-hand--written%20parser-success">
</p>

# 📖 Overview

**AlgoMentor** is a desktop learning application, written in **Java + JavaFX**, that teaches data-structures-and-algorithms by letting a student *watch* an algorithm run, *read* the exact code that is running, and *talk* to an AI tutor about it — all in one window.

Ten classic algorithms (six sorts, binary search, BFS, DFS and Dijkstra) are executed by real Java classes that do not draw anything themselves. Instead, each algorithm records a **trace** — an ordered list of immutable `Step` objects — and a single, algorithm-agnostic **playback engine** replays that trace on a canvas. Because the trace is data, the student can play, pause, single-step **forwards and backwards**, and scrub to any moment with a slider.

Around the visualizer sit the pieces of a complete application: **email-verified user accounts** (real SMTP delivery), a **SQLite** persistence layer with full CRUD, an editable **attempt-history** screen, a **live AI Mentor Chat** that always knows which algorithm is open, an **AI-written Complexity Analysis** that is *grounded* in a hand-verified Big-O table (so the model can explain but never invent the numbers), and a **Practice Problems** tab combining a curated bank of real LeetCode links with AI-generated original problems.

Two design decisions shape the whole codebase and are worth stating up front:

* **No third-party JSON library.** Every JSON request and response — to Groq, OpenAI and Anthropic — is built and read by a **hand-written recursive-descent parser** (`JsonParser` + `JsonValue`) included in the project.
* **The UI never blocks.** All database, network and trace-computation work runs on one shared, bounded thread pool and returns to the JavaFX Application Thread through `Platform.runLater(...)`.

> 📄 For a full academic treatment — requirements, design, algorithm analysis, measured results, testing and critique — see the **Project Report** (`docs/AlgoMentor_Technical_Report.docx`).

---

# ✨ Key Features

## 🎬 Algorithm Visualizer
- 10 algorithms across 4 categories (Sorting, Binary Search, Graph Traversal, Shortest Path)
- Play / Pause, **Step Forward, Step Back**, progress-slider scrubbing, and a 0.25×–3× speed slider
- Custom input (`8,3,5,1,9,2`) or random data of size 4–30
- Colour-coded bars and graph nodes (compare, swap, found; frontier vs. visited)
- Step-by-step caption for every frame (e.g. *"Relaxed edge 2 -> 5 (weight 3): new candidate distance 5"*)
- Fully resizable canvas that redraws when the window changes

## 💻 Source-Code Panel
- Shows the **actual Java logic** of the selected algorithm beside the animation
- **"Ask AI Mentor"** button sends a pre-written "walk me through this code" question to the chat
- Collapsible (`−` / `+`) with automatic re-layout of the split pane

## 📘 Raw Algorithm & Tips Panel (works offline)
- Plain-English numbered steps for all 10 algorithms
- A **"Picture it"** analogy (fizzy-drink bubbles, playing cards, pond ripples, GPS…)
- A **"Watch for"** hint telling the student what to look for in the animation

## 🤖 AI Mentor Chat
- Always-visible right sidebar with speech bubbles and right-click **Copy text**
- Context-aware: the system prompt is told which algorithm and complexity are open
- One-click chips: **Explain it**, **Example**, **Why this speed?**
- Sends only the most recent 12 turns to stay inside free-tier token limits
- Provider is switchable with **zero code changes**: **Groq (free, no credit card, default)** or **OpenAI**

## 📊 Complexity Analysis
- AI writes **WHAT** each algorithm does, **WHY** its speed is what it is, a **HEAD-TO-HEAD** comparison, a **VERDICT**, and where *your current algorithm* ranks
- The Big-O figures come from `AlgorithmGuide.java`, **not** from the model's memory — they are injected into the prompt as *ground truth*
- A summary table (Best / Average / Worst / Space / Key trait) follows, with the current algorithm highlighted
- Binary Search is compared against a **Linear Search baseline**

## 🧩 Practice Problems
- **13 hand-curated, real LeetCode problems** across 4 topics (links verified by hand, never AI-generated)
- **"Generate with AI"** writes 3 original problems of rising difficulty
- The topic auto-selects to match the algorithm currently open

## 🔐 Accounts & Persistence
- Registration → **6-digit emailed code** → account created only after verification
- Salted SHA-256 password hashes; no plaintext ever stored
- Every visualizer run is logged as an **Attempt**; the History screen lets you **edit notes in place (UPDATE)** and **delete rows (DELETE)**
- Welcome email after successful verification

## ⚙️ Engineering Quality
- Startup **configuration report** in the console (which keys are `SET` / `MISSING`)
- Every AI / email failure surfaces its **real reason** (wrong key, no quota, bad model) instead of silently pretending to be offline
- Automatic **network retry** (3 attempts, back-off) for dropped connections
- Colourful glassmorphism theme where every control is styled in its *default* state (no "white-on-white until hover")

---

# 🏗️ System Architecture

AlgoMentor follows an **MVC-style layered design**. Dependencies point downwards only: the UI knows about services and models; models know about nothing.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER (View)                       │
│   login.fxml        main.fxml            history.fxml     style.css    │
└───────────────┬────────────────────────────────────────────────────────┘
                │ @FXML bindings / event handlers
┌───────────────▼────────────────────────────────────────────────────────┐
│                       CONTROLLER LAYER (Controller)                    │
│  LoginController   MainController(801 lines)   HistoryController       │
│                    │                                                   │
│                    ├── CanvasRenderer  (draws Steps, algorithm-blind)  │
│                    └── Playback engine (Timeline over List<Step>)      │
└───────┬───────────────────┬──────────────────────┬─────────────────────┘
        │                   │                      │
┌───────▼─────────┐ ┌───────▼──────────┐ ┌─────────▼──────────────────┐
│  ALGORITHM      │ │  SERVICES        │ │  PERSISTENCE               │
│  Traceable      │ │  OpenAiChatService│ │  DatabaseManager          │
│  ArrayAlgorithm │ │  EmailService    │ │  (SQLite via JDBC,         │
│  GraphAlgorithm │ │  AiAnalysisService│ │   PreparedStatements)     │
│  + 10 concrete  │ │  ApiClient       │ └────────────────────────────┘
│  AlgorithmFactory│ │  JsonParser/Value│
└───────┬─────────┘ └───────┬──────────┘
        │                   │
┌───────▼───────────────────▼────────────────────────────────────────────┐
│                         MODEL & UTILITY LAYER                          │
│ Step · StepType · GraphNode · DemoGraph · User · Attempt ·             │
│ PendingRegistration · ChatMessage                                      │
│ AppConfig · AppExecutors · SessionContext · PasswordUtil ·             │
│ AlgorithmGuide · AlgorithmCodeSnippets · ProblemBank                   │
└────────────────────────────────────────────────────────────────────────┘
```

### The central idea: *trace, then replay*

```
   ┌──────────────┐   run()    ┌──────────────────────┐   replay    ┌──────────────┐
   │ Traceable    │ ─────────► │ List<Step> (a trace) │ ──────────► │ CanvasRenderer│
   │ (any of 10)  │  records   │ COMPARE, SWAP, VISIT │  one frame  │ switch(type)  │
   └──────────────┘  Steps     │ + array snapshot     │  at a time  └──────────────┘
                               └──────────────────────┘
                                         ▲
                     Timeline · Step ⏮ ⏭ · Slider scrub · Speed slider
```

The algorithm **never touches the UI**, and the renderer **never knows which algorithm** produced a step. That separation is what lets one playback engine drive ten algorithms and makes step-*back* trivial.

---

# 🔄 Execution Workflows

## 1️⃣ Visualization workflow

```
      Select algorithm            Generate / Reset
             │                          │
             ▼                          ▼
   onAlgorithmChanged()        parse custom input OR random 4–30
   ├─ update complexity        (Binary Search: input is sorted)
   ├─ load source snippet                │
   ├─ load raw-algorithm guide           ▼
   ├─ update chat context          Static bars / graph drawn
   └─ auto-pick practice topic           │
                                          ▼  click START
                        AppExecutors thread: build Traceable via Factory
                                          │  algorithm.run()  →  List<Step>
                                          ▼  Platform.runLater
                        onTraceReady(): slider range set, jumpToStep(0),
                                        startPlayback(), logAttempt() → SQLite
                                          │
                       ┌──────────────────┴───────────────────┐
                       ▼                                      ▼
             Timeline advanceStep()                  ⏮ / ⏭ / slider scrub
             every 500ms ÷ speed                     (graph: replay 0..n to
                                                      rebuild cumulative state)
```

## 2️⃣ Registration & email-verification workflow

```
 Register click ──► validate (user ≥3, valid email, password ≥4)
        │
        ▼
 users table has username/email? ──yes──► "already registered"
        │ no
        ▼
 salt = SecureRandom(16B) · hash = SHA-256(salt‖password) · code = 6 digits
        │
        ▼
 INSERT into pending_registrations  (expires in 10 min)   ← NOT a real account
        │
        ├──► show verification card
        └──► EmailService sends the code (Jakarta Mail / SMTP, async)
                     │
        user types code ──► completeRegistration():
                              code matches AND not expired?
                                 ├─ yes → INSERT users, DELETE pending → sign in → welcome email
                                 └─ no  → "Incorrect or expired code" (username stays free)
```

> **Why two tables?** An earlier version wrote a `users` row *before* verification, so an abandoned sign-up locked that username/email forever. Registration now only touches `pending_registrations`; a real account exists only after the correct code is entered.

## 3️⃣ AI request workflow

```
 Chat / Analyze / Generate click
        │
        ▼
 OpenAiChatService.keyProblem()  ── missing / wrong-provider key? ──► show precise fix message
        │ ok
        ▼
 build messages[] (system prompt + history) with JsonValue → toJson()
        │
        ▼
 ApiClient.postJson(url, body, "Authorization: Bearer …")   ← HTTP/1.1, 90 s timeout
        │   IOException? → retry up to 3× (700 ms × attempt)   HTTP 4xx/5xx → NOT retried
        ▼
 JsonParser.parse(response) → choices[0].message.content → cleanReply() (strip markdown)
        │
        ▼
 Platform.runLater → speech bubble / analysis text area
```

---

# 📂 Project Structure

```
algomentor56/
├── SETUP_NOTES.md                      # Change log & troubleshooting narrative
└── algomentor/
    ├── pom.xml                         # Maven build (JavaFX, SQLite, Jakarta Mail, shade)
    ├── algomentor.properties.example   # Config template (safe to commit)
    ├── algomentor.properties           # Your real secrets (MUST be git-ignored)
    └── src/main/
        ├── java/com/algomentor/
        │   ├── Main.java               # JavaFX entry: config → DB → login screen
        │   ├── Launcher.java           # Non-Application entry for fat-jar `java -jar`
        │   ├── model/                  # 8 files  – pure data (Step, User, Attempt, graph…)
        │   ├── algorithm/              # 14 files – Traceable + 10 algorithms + Factory
        │   ├── controller/             # 4 files  – Login, Main, History, CanvasRenderer
        │   ├── db/                     # 1 file   – DatabaseManager (all SQL)
        │   ├── network/                # 6 files  – ApiClient, JSON, AI, Email
        │   └── util/                   # 7 files  – config, threads, hashing, teaching data
        └── resources/
            ├── fxml/   login.fxml · main.fxml · history.fxml
            └── css/    style.css
```

## 🗂️ Which file does what

### Entry points & build

| File | Responsibility |
|---|---|
| `Main.java` | Extends `Application`. Prints the config report, creates `~/.algomentor/algomentor.db`, honours `RESET_DB_ON_START`, builds `DatabaseManager`, initialises `SessionContext`, loads `login.fxml` (900×600, min 760×520), and on close shuts down the DB and thread pool. |
| `Launcher.java` | Plain `main` that calls `Main.main`. Needed because a shaded jar whose `Main-Class` extends `Application` refuses to start without JavaFX on the module path. |
| `pom.xml` | Declares JavaFX (controls, fxml), `sqlite-jdbc`, `jakarta.mail`; plugins: compiler, `javafx-maven-plugin` (`mvn javafx:run`), `maven-shade-plugin` (fat jar). |
| `algomentor.properties(.example)` | Runtime configuration (AI provider/keys, SMTP credentials, DB reset flag). |
| `SETUP_NOTES.md` | Round-by-round record of bugs fixed and design changes. |

### `model/` — plain data

| File | Responsibility |
|---|---|
| `Step.java` | **Immutable** trace frame: `type`, `indices`, optional cloned `arraySnapshot`, human `description`. Defensive copies on construction *and* on read. |
| `StepType.java` | Vocabulary enum: `COMPARE, SWAP, OVERWRITE, VISIT, ENQUEUE, DEQUEUE, MARK_FOUND, PARTITION, DONE`. |
| `GraphNode.java` | Node with fixed `(x, y)` layout, neighbour list, and a weight map (default weight 1). |
| `DemoGraph.java` | The fixed 8-node, 8-edge weighted undirected demo graph used by BFS/DFS/Dijkstra. |
| `User.java` | `users` row (id, username, email, hash, salt, createdAt). |
| `PendingRegistration.java` | Record for a not-yet-verified sign-up (includes code and expiry). |
| `Attempt.java` | `attempts` row: user FK, algorithm, input size, duration, editable note, timestamp. |
| `ChatMessage.java` | Record `(role, content)` for one chat turn. |

### `algorithm/` — the ten algorithms

| File | Responsibility |
|---|---|
| `Traceable.java` | Interface: `run()`, `getName()`, `getComplexity()`, `getDescription()`. |
| `ArrayAlgorithm.java` | Abstract **template-method** base for array algorithms: owns the working copy, `final run()`, and helpers `recordCompare / recordSwap / recordOverwrite / recordPartition / recordFound`. |
| `GraphAlgorithm.java` | Abstract base for graph algorithms: `recordEnqueue / recordDequeue / recordVisit`. |
| `BubbleSortAlgorithm.java` | Adjacent swaps with early-exit flag. |
| `SelectionSortAlgorithm.java` | Scan for minimum, ≤ 1 swap per pass. |
| `InsertionSortAlgorithm.java` | Slide each element left into the sorted prefix. |
| `MergeSortAlgorithm.java` | Recursive halving, merge via `OVERWRITE` steps. |
| `QuickSortAlgorithm.java` | Lomuto partition, last element as pivot. |
| `HeapSortAlgorithm.java` | Build max-heap, repeatedly swap root to end and `siftDown`. |
| `BinarySearchAlgorithm.java` | `PARTITION` for the live range, `COMPARE` at mid, `MARK_FOUND` on hit. |
| `BFSAlgorithm.java` | Queue-based, level-by-level. |
| `DFSAlgorithm.java` | Explicit stack, lazy visited check. |
| `DijkstraAlgorithm.java` | `PriorityQueue` with stale-entry skipping; edge relaxation captions include the distances. |
| `AlgorithmFactory.java` | Name → concrete instance; category helpers (`categoryOf`, `isGraphAlgorithm`, …) and the name lists shown in the UI. |

### `controller/` — behaviour

| File | Responsibility |
|---|---|
| `MainController.java` | The workspace: algorithm list, input handling, background trace computation, playback engine, step-back replay, Practice Problems, AI chat, Complexity Analysis table, collapsible panels, navigation. |
| `CanvasRenderer.java` | Draws one `Step`. Bars for arrays; nodes/edges for graphs with **cumulative** visited/frontier sets. Scales the graph from a 700×460 design space to any canvas size. |
| `LoginController.java` | Login, registration, verification card, resend, cancel. Enforces the pending → verified flow. |
| `HistoryController.java` | Loads the user's attempts, in-place note editing (UPDATE), confirmed delete (DELETE), back navigation. |

### `db/`

| File | Responsibility |
|---|---|
| `DatabaseManager.java` | One SQLite connection, `PRAGMA foreign_keys = ON`, schema creation, and **every** SQL statement (all via `PreparedStatement`). |

### `network/` — HTTP, JSON, AI, email

| File | Responsibility |
|---|---|
| `ApiClient.java` | Async `HttpClient` wrapper (HTTP/1.1, 15 s connect, 90 s POST timeout, 3-attempt retry on `IOException` only). |
| `JsonParser.java` | Hand-written **recursive-descent** JSON parser (objects, arrays, strings with escapes incl. `\uXXXX`, numbers, booleans, null). |
| `JsonValue.java` | Tagged-union JSON tree with typed accessors, builders and `toJson()` serializer. |
| `OpenAiChatService.java` | All AI features (chat, complexity analysis, problem generation) for Groq **or** OpenAI (OpenAI-compatible API); key validation; markdown clean-up. |
| `EmailService.java` | Real SMTP (Jakarta Mail, STARTTLS/587) for verification and welcome emails; console fallback when unconfigured; explicit Gmail-auth error message. |
| `AiAnalysisService.java` | *Legacy* Anthropic Messages API client (the old "AI tip" box). Still initialised by `SessionContext` but **not called by the current UI**. |

### `util/` — shared infrastructure & teaching content

| File | Responsibility |
|---|---|
| `AppConfig.java` | Reads `algomentor.properties` first, environment variables second; prints the startup report; warns on swapped keys. |
| `AppExecutors.java` | Shared **fixed pool of 4 daemon threads** (`algomentor-worker-N`). |
| `SessionContext.java` | Static holder for the DB, AI/email services and current user. |
| `PasswordUtil.java` | 16-byte random salt, salted SHA-256, verification, 6-digit code generation. |
| `AlgorithmGuide.java` | Offline plain-English guides **and** the verified Big-O `Facts` table used for the summary table and to ground AI prompts. |
| `AlgorithmCodeSnippets.java` | The literal Java logic shown in the Source-Code panel (10 snippets). |
| `ProblemBank.java` | 13 curated real LeetCode links in 4 topics. |

### `resources/`

| File | Responsibility |
|---|---|
| `login.fxml` | `StackPane` holding two swap-able cards: login/register and email-verification. |
| `main.fxml` | `BorderPane` shell: toolbar, algorithm sidebar, 3-tab centre (Visualizer / Practice / Complexity), AI chat sidebar, playback bar. |
| `history.fxml` | Editable `TableView` of attempts with Delete and Back. |
| `style.css` | 351-line JavaFX theme: gradient panels, glow effects, chat bubbles, explicit default-state styling for every control. |

---

# 🌟 Highlights

* One **algorithm-agnostic** playback engine for ten algorithms
* **Reversible** execution (step back / scrub) thanks to snapshot-carrying `Step`s
* Real Java source shown beside the animation — *not* pseudocode
* **AI grounded on verified data** instead of trusting model memory
* **Hand-written JSON parser** — no Gson/Jackson/org.json anywhere
* Provider-agnostic AI layer: free **Groq** by default, **OpenAI** by config
* **Two-table registration** that cannot orphan a username
* **Bounded thread pool** + `Platform.runLater` discipline for a never-frozen UI
* Automatic retry logic that separates *network* failures (retried) from *HTTP* answers (not retried)
* Transparent failure messages instead of silent "offline mode"
* Fully offline teaching layer (guides, snippets, Big-O table) — the app is useful with no API key at all
* Responsive layout: canvas bound to its container, panels collapsible, minimum-size safe

---

# 🧠 Module Deep-Dives

## 🔹 Algorithm Module

The whole package rests on **one interface and two abstract classes**:

```
                    «interface» Traceable
           run() · getName() · getComplexity() · getDescription()
                  ▲                                   ▲
      ArrayAlgorithm (abstract)             GraphAlgorithm (abstract)
      array[] + steps[] + record*()         graph + startId + steps[] + record*()
        ▲   ▲   ▲   ▲   ▲   ▲   ▲                ▲        ▲          ▲
   Bubble Sel Ins Merge Quick Heap Binary       BFS      DFS     Dijkstra
```

`run()` is **`final`** in both bases: it clears the list, calls the subclass's `execute()`, then appends a terminal `DONE` step. Subclasses therefore implement *only* what makes them unique (**Template Method** pattern). `ArrayAlgorithm.recordSwap(i, j)` performs the swap **and** records it, so the trace can never disagree with what the algorithm actually did.

### Algorithm catalogue

| # | Algorithm | Category | Best | Average | Worst | Space | Step types emitted |
|---|---|---|---|---|---|---|---|
| 1 | Bubble Sort | Sorting | O(n) | O(n²) | O(n²) | O(1) | COMPARE, SWAP |
| 2 | Selection Sort | Sorting | O(n²) | O(n²) | O(n²) | O(1) | COMPARE, SWAP |
| 3 | Insertion Sort | Sorting | O(n) | O(n²) | O(n²) | O(1) | COMPARE, SWAP |
| 4 | Merge Sort | Sorting | O(n log n) | O(n log n) | O(n log n) | O(n) | PARTITION, COMPARE, OVERWRITE |
| 5 | Quick Sort | Sorting | O(n log n) | O(n log n) | O(n²) | O(log n) | PARTITION, COMPARE, SWAP |
| 6 | Heap Sort | Sorting | O(n log n) | O(n log n) | O(n log n) | O(1) | COMPARE, SWAP |
| 7 | Binary Search | Searching | O(1) | O(log n) | O(log n) | O(1) | PARTITION, COMPARE, MARK_FOUND |
| 8 | Breadth-First Search | Graph | O(V+E) | O(V+E) | O(V+E) | O(V) | ENQUEUE, DEQUEUE, VISIT |
| 9 | Depth-First Search | Graph | O(V+E) | O(V+E) | O(V+E) | O(V) | ENQUEUE, DEQUEUE, VISIT |
| 10 | Dijkstra's Algorithm | Shortest path | O((V+E) log V) | O((V+E) log V) | O((V+E) log V) | O(V) | ENQUEUE (relax), DEQUEUE, VISIT (finalize) |

*(Linear Search exists only as a comparison baseline inside `AlgorithmGuide`; it has no visualizer.)*

### The demo graph

```
                 (0)
             4 /    \ 2
             (1)     (2)
          5 /  \ 1  3 /  \ 6
          (3)  (4)  (5)  (6)
                 \ 2  / 4
                  (7)
```

Edges: `0-1:4, 0-2:2, 1-3:5, 1-4:1, 2-5:3, 2-6:6, 4-7:2, 5-7:4`. The search always starts at node `0`.

## 🔹 Rendering Module (`CanvasRenderer`)

| Situation | Colour |
|---|---|
| Array bar (default) | 🔵 blue `#60a5fa` |
| Being compared / overwritten / partition bounds | 🟡 amber `#fbbf24` |
| Being swapped | 🔴 red `#f87171` |
| Binary-search target found | 🟢 green `#4ade80` |
| Graph node — unvisited | 🔵 blue |
| Graph node — in frontier | 🟡 amber |
| Graph node — visited / finalized | 🟢 green |

Array steps carry a full snapshot, so any step draws independently. Graph steps do **not** carry snapshots, so the renderer keeps cumulative `visited` / `frontier` sets — and **step-back is implemented by resetting those sets and replaying steps `0..n-1`**.

## 🔹 Persistence Module (`DatabaseManager`)

```
┌─────────────────────┐        ┌──────────────────────────┐        ┌──────────────────────────┐
│ pending_registrations│       │ users                    │        │ attempts                 │
├─────────────────────┤        ├──────────────────────────┤        ├──────────────────────────┤
│ id PK               │        │ id PK                    │ 1    * │ id PK                    │
│ username UNIQUE     │ code   │ username UNIQUE          │────────│ user_id FK → users(id)   │
│ email UNIQUE        │verified│ email UNIQUE             │ CASCADE│ algorithm_name           │
│ password_hash, salt │ ─────► │ password_hash, salt      │ DELETE │ input_size               │
│ code, expires_at    │        │ created_at               │        │ duration_millis          │
└─────────────────────┘        └──────────────────────────┘        │ note (editable)          │
                                                                    │ created_at               │
                                                                    └──────────────────────────┘
```

| Operation | Methods |
|---|---|
| **Create** | `savePendingRegistration`, `createUser` (only from `completeRegistration`), `createAttempt` |
| **Read** | `findUserByUsername`, `findUserByEmail`, `findPendingByUsername`, `getAttemptsForUser` (newest first) |
| **Update** | `updateAttemptNote` (History screen), `updateUserPassword` |
| **Delete** | `deleteAttempt` (History screen), `deleteUser` (cascades), `deletePendingRegistration` |

The database file lives at **`~/.algomentor/algomentor.db`** — in your *home* folder, so replacing the project files never resets accounts.

## 🔹 Networking & JSON Module

* **`ApiClient`** — `sendAsync` on the shared executor; HTTP/1.1 is forced because the JDK's HTTP/2 client can die with *"EOF reached while reading"* when a proxy drops a pooled connection. Retries apply only to `IOException` (connection problems); a `401` or `429` is a real answer and is surfaced immediately.
* **`JsonParser`** — a 167-line recursive-descent parser: `parseValue()` dispatches on the first character to `parseObject / parseArray / parseString / parseNumber / parseBoolean / parseNull`, each consuming exactly its own grammar production.
* **`JsonValue`** — immutable-style tagged union (`OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL`) with `toJson()` for requests, so the same class both **writes** and **reads** JSON.
* **`OpenAiChatService`** — one `complete(messages, maxTokens)` method serves chat (1500 tokens), analysis (3000) and problem generation (1500). For Groq's `openai/gpt-oss-*` reasoning models it adds `reasoning_effort: "low"` so hidden thinking tokens don't consume the answer budget.

## 🔹 AI Grounding Strategy

```
 AlgorithmGuide.FACTS  ──►  verified table (best/avg/worst/space/trait)
                                   │  injected as "ground truth"
                                   ▼
        system prompt: "never contradict the table, never invent numbers"
                                   │
                                   ▼
          model explains WHAT / WHY / HEAD-TO-HEAD / VERDICT / YOUR ALGORITHM
```

The model supplies *explanation*; the application supplies *facts*. This removes the most common failure of LLM tutors — confidently wrong Big-O claims.

---

# 🔬 Design Patterns & Concepts Behind the Code

| Concept | Where it appears | Why it matters |
|---|---|---|
| **MVC** | FXML (view) · Controllers · model package | Separates layout, behaviour and data |
| **Template Method** | `ArrayAlgorithm.run()` / `GraphAlgorithm.run()` | Shared bookkeeping, unique `execute()` |
| **Factory** | `AlgorithmFactory` | Controller never names a concrete class |
| **Polymorphism / interface segregation** | `Traceable` | Array and graph algorithms are interchangeable to the UI |
| **Command-log / event sourcing (lightweight)** | `List<Step>` trace | Execution becomes replayable, scrub-able data |
| **Immutability & defensive copying** | `Step`, `List.copyOf`, `array.clone()` | A recorded frame can never be mutated later |
| **Service locator / singleton** | `SessionContext`, `AppConfig` | Simple app-wide access without DI framework |
| **Observer** | JavaFX property listeners (slider, canvas size, tab selection) | UI reacts to state changes declaratively |
| **Producer/consumer via futures** | `CompletableFuture`, `Platform.runLater` | Off-thread work, on-thread UI updates |
| **Bounded thread pool** | `AppExecutors` (4 daemon threads) | Prevents thread explosion and UI starvation |
| **Recursive-descent parsing** | `JsonParser` | Grammar → one method per production |
| **Salted hashing** | `PasswordUtil` | Identical passwords hash differently; rainbow tables are useless |
| **Prepared statements** | every SQL call | Blocks SQL injection |
| **Retry with linear back-off** | `ApiClient.attemptPost` | Resilience to transient network drops |
| **Prompt grounding** | `analyzeComplexity` | Trustworthy AI output |
| **Graceful degradation** | offline guides, console-email fallback | App remains usable with no keys |
| **Two-phase commit (business-level)** | pending → verified registration | No orphaned reservations |

### Algorithm concepts illustrated

* **Comparison-sort lower bound** — the analysis tab shows why O(n log n) is the floor for comparison sorts and why quadratic sorts lose on big data.
* **Stability & in-place-ness** — each `Facts` record states whether a sort is stable and how much extra memory it needs.
* **Divide and conquer** — merge sort's recursion tree is visible through `PARTITION` steps.
* **Adaptive algorithms** — bubble/insertion sort collapse to ~n steps on sorted input (see measured results).
* **Frontier data structures** — queue (BFS) vs. stack (DFS) vs. priority queue (Dijkstra) produce three different visit orders on the *same* graph.
* **Greedy correctness** — Dijkstra finalizes the closest unsettled node; it requires non-negative weights.

---

# ✅ Measured Verification

The `model` and `algorithm` packages have no JavaFX dependency, so they were compiled and executed directly on **OpenJDK 27** with a small harness. The numbers below are **measured from the project's own classes**, not estimated.

### Correctness — final snapshot equals `Arrays.sort` on 300 random arrays (sizes 4–30)

| Bubble | Selection | Insertion | Merge | Quick | Heap |
|:-:|:-:|:-:|:-:|:-:|:-:|
| 300/300 | 300/300 | 300/300 | 300/300 | 300/300 | 300/300 |

### Trace length — total steps / comparisons / moves (swaps + overwrites), mean of 50 random arrays

| Algorithm | n = 10 | n = 20 | n = 30 |
|---|---|---|---|
| Bubble | 63 / 41 / 20 | 271 / 180 / 90 | 631 / 419 / 211 |
| Selection | 53 / 45 / 7 | 207 / 190 / 16 | 462 / 435 / 26 |
| Insertion | 49 / 28 / 20 | 197 / 106 / 90 | 449 / 237 / 211 |
| Merge | 67 / 23 / 34 | 172 / 64 / 88 | 289 / 111 / 148 |
| Quick | 41 / 25 / 9 | 113 / 70 / 29 | 195 / 122 / 52 |
| Heap | 68 / 39 / 27 | 188 / 115 / 72 | 328 / 204 / 122 |

### Best/worst-case behaviour at n = 20

| Algorithm | Already sorted | Reverse sorted |
|---|---|---|
| Bubble | 20 steps (19 compares, **0 swaps** – early exit) | 381 steps (190 cmp, 190 swaps) |
| Insertion | 20 steps (**0 swaps**) | 381 steps (190 cmp, 190 swaps) |
| Selection | 191 steps (**still 190 compares**) | 201 steps |
| Quick (last-element pivot) | 210 steps (**degrades toward n²**) | 220 steps |
| Merge | 156 steps | 148 steps (stable ≈ n log n) |

These reproduce the textbook behaviour the app teaches: bubble/insertion are adaptive, selection always scans everything, Lomuto-quicksort with a last-element pivot degrades on sorted input, and merge/heap are input-insensitive.

### Search and graph traces

* **Binary Search**, n = 30, every element as target: mean **4.13** comparisons (124 total), max **5** (⌊log₂30⌋ + 1 = 5).
* **BFS** from node 0 → visit order `0, 1, 2, 3, 4, 5, 6, 7` (25 steps).
* **DFS** from node 0 → visit order `0, 2, 6, 5, 7, 4, 1, 3` (27 steps).
* **Dijkstra** from node 0 → finalized order `0, 2, 1, 5, 4, 7, 6, 3` with shortest distances **0, 2, 4, 5, 5, 7, 8, 9** for nodes `0, 2, 1, 5, 4, 7, 6, 3` (hand-checked against the graph). The trace has 9 ENQUEUE steps (the start node plus 8 successful relaxations) but only 8 finalizations, because node 7 is first reached at distance 9 (via 5) and later improved to 7 (via 4) — the outdated queue entry is correctly skipped.

> ℹ️ The JavaFX UI, SQLite layer, SMTP and AI calls were **not** executed in this verification (they need a desktop session, JavaFX runtime and credentials). The repository currently contains **no automated tests** (`src/test/java` is empty) — see *Future Enhancements*.

---

# 💻 Technical Implementation

| Layer | Technology |
|---|---|
| Language | Java 27 (code uses records, switch expressions, text blocks; nothing newer than JDK 21 is required) |
| UI | JavaFX 27 (`javafx-controls`, `javafx-fxml`), FXML, CSS, `Canvas` |
| Build | Maven 3, `maven-compiler-plugin 3.13.0`, `javafx-maven-plugin 0.0.8`, `maven-shade-plugin 3.5.3` |
| Database | SQLite via `org.xerial:sqlite-jdbc 3.47.1.0` |
| Email | `com.sun.mail:jakarta.mail 2.0.2` (SMTP + STARTTLS) |
| HTTP | `java.net.http.HttpClient` (async, HTTP/1.1) |
| JSON | **Own implementation** (`JsonParser`, `JsonValue`) |
| AI | Groq Chat Completions (default, `openai/gpt-oss-20b`) or OpenAI (`gpt-4o-mini`) |

### Codebase statistics

| Metric | Value |
|---|---|
| Java source files | 42 |
| Java lines of code | ≈ 4,150 |
| Algorithms implemented | 10 |
| FXML views | 3 (324 lines) |
| Database tables | 3 |
| Curated practice problems | 13 (4 topics) |
| Built-in teaching guides / code snippets | 10 / 10 |
| Verified Big-O fact records | 11 |
| Worker threads | 4 |

---

# ⚙️ Configuration Reference

Create `algomentor.properties` next to `pom.xml` (copy from `algomentor.properties.example`). Values are read from this file first, then from environment variables.

| Key | Purpose | Default |
|---|---|---|
| `AI_PROVIDER` | `groq` or `openai` | `groq` |
| `GROQ_API_KEY` | Free key from console.groq.com (no credit card) | — |
| `GROQ_MODEL` | Any model listed in Groq's docs | `openai/gpt-oss-20b` |
| `OPENAI_API_KEY` | Needed only if `AI_PROVIDER=openai` | — |
| `OPENAI_MODEL` | OpenAI model | `gpt-4o-mini` |
| `MAIL_USERNAME` | Gmail address that sends emails | — |
| `MAIL_PASSWORD` | Gmail **App Password** (spaces auto-stripped) | — |
| `MAIL_SMTP_HOST` / `MAIL_SMTP_PORT` | SMTP server override | `smtp.gmail.com` / `587` |
| `ANTHROPIC_API_KEY` | Optional; only the legacy `AiAnalysisService` uses it | — |
| `RESET_DB_ON_START` | `true` for **one** run to delete the database | `false` |

**Behaviour with missing keys** — the app never crashes: no AI key → the chat/analysis/problem buttons show *exactly* how to fix it; no mail credentials → the verification email is **printed to the console** so you can still copy the code.

---

# ▶️ Installation & Execution

## Prerequisites
* **JDK 27** (matches `maven.compiler.release` in `pom.xml`) and **Maven 3.8+**
* Internet access for Maven dependencies (and, optionally, AI + SMTP)

## Steps

```bash
# 1. Enter the project folder (the one containing pom.xml)
cd algomentor7/algomentor

# 2. Create your config
cp algomentor.properties.example algomentor.properties
#    then edit it: GROQ_API_KEY, MAIL_USERNAME, MAIL_PASSWORD

# 3. Run
mvn clean javafx:run
```

Watch the console first — the **configuration report** prints before the window opens:

```
==================== AlgoMentor configuration ====================
  MAIL_USERNAME        [SET]     -> Real welcome/verification emails
  MAIL_PASSWORD        [SET]     -> Real welcome/verification emails
  GROQ_API_KEY         [SET]     -> AI Mentor Chat, Complexity Analysis, AI-generated problems
  OPENAI_API_KEY       [MISSING] -> Same AI features if AI_PROVIDER=openai instead
====================================================================
```

### If Maven cannot resolve a version
`pom.xml` targets JDK/JavaFX **27**. The code needs nothing newer than JDK 21, so if resolution fails, lower `maven.compiler.release` and `javafx.version` (and the two JavaFX dependency versions) to a release you have, e.g. **21**.

### Getting a Gmail App Password
Google Account → Security → **2-Step Verification** (must be ON) → **App passwords** → generate → paste the 16 characters as `MAIL_PASSWORD`. Google rejects normal account passwords from third-party apps.

### Building a runnable jar
```bash
mvn clean package
```
> ⚠️ `Launcher.java` exists precisely so a shaded jar can start, but `pom.xml`'s shade `ManifestResourceTransformer` currently names `com.algomentor.Main`. For `java -jar` to work, change that `<mainClass>` to `com.algomentor.Launcher`. For everyday use, `mvn javafx:run` is the supported path.

---

# 🧭 Using the App

1. **Sign up** → enter username, email, password → type the 6-digit code from your inbox.
2. **Pick an algorithm** in the left list. The complexity, source code, guide and chat context all update.
3. **Enter input** (`8,3,5,1,9,2`) or leave blank and choose a size; for Binary Search optionally set a **Target**.
4. Press **Start**. Use ▶/⏸, ⏮/⏭, the progress slider and the speed slider.
5. Read the **Source Code** and **Raw Algorithm & Tips** panels; fold them with `−`.
6. Ask the **AI Mentor** anything, or tap *Explain it / Example / Why this speed?*.
7. Open **Complexity Analysis** → *Analyze & compare* to get the AI study note plus the summary table.
8. Open **Practice Problems** → *Find Problems* (curated links) or *Generate with AI*.
9. Open **History** to review runs, edit notes (double-click) and delete rows.

---

# 🔒 Security Notes

* **Never commit `algomentor.properties`.** It holds your mail password and API keys. A `.gitignore` ships, but it only excludes `target/`, so add:
  ```gitignore
  algomentor7/algomentor/algomentor.properties
  target/
  *.db
  .idea/
  ```
  If the file is already tracked, stop tracking it (this keeps your local copy):
  ```bash
  git rm --cached algomentor7/algomentor/algomentor.properties
  git commit -m "Stop tracking local credentials"
  ```
* **If real credentials were ever committed or shared, rotate them** (regenerate the Gmail App Password and the Groq/OpenAI keys).
* Passwords are stored as **salted SHA-256**. That is far better than plaintext, but a single fast hash is not the modern standard for a public service — see *Future Enhancements*.
* All SQL uses **prepared statements**.
* Verification codes are 6 digits from `SecureRandom` and expire after 10 minutes.

---

# ⚠️ Known Limitations

* **Dijkstra's edge weights and distance labels are not drawn on the canvas.** They appear in the step captions (e.g. *"Finalized node 5 with shortest distance 5"*), but the graph picture itself shows only node colours.
* **Custom input is unbounded**: very large arrays (hundreds of elements) can exhaust memory because every step stores a snapshot. Keep custom input under ~60 numbers.
* Dragging the progress slider forward on BFS/DFS/Dijkstra can show incomplete visited state; stepping normally or backwards is correct.
* The demo graph is **fixed** (8 nodes) and traversals always start at node `0`.
* The **Duration** column in History records the time to *compute the trace* (from clicking Start until the trace is ready), not the animation length.
* `User.emailVerified` is a Java-side flag; the `users` table has no such column because only verified users are ever stored.
* `updateUserPassword` and `deleteUser` exist in `DatabaseManager` but no screen calls them yet.
* `AiAnalysisService` (Anthropic) is retained but unused by the current UI.
* No automated test suite yet.
* Some FXML/Javadoc comments describe earlier iterations (e.g. login-time verification prompts) and could be tidied.

---

# 🚀 Future Enhancements

### Visualization
- Draw **edge weights** and live **distance labels** on Dijkstra's graph
- Add editable / random graphs, A\*, Kruskal/Prim, topological sort
- Add trees (BST insert/delete, AVL rotations) and linked-list visualizers
- Export a run as an animated GIF

### Learning
- Quizzes generated from the *current trace* ("what will be swapped next?")
- Progress dashboard built on the `attempts` table (streaks, time per topic)
- Persist chat history per user

### Engineering
- Replace SHA-256 with **PBKDF2 / bcrypt / Argon2** and add login rate-limiting
- JUnit tests for every `Traceable` (sortedness, permutation, trace-ends-in-DONE) and for `JsonParser`
- Connection pooling / migrations for the database
- Stream AI replies token-by-token
- Internationalisation of UI text

---

# 🎓 Learning Outcomes

Building AlgoMentor demonstrates practical skill in:

- Object-oriented design (abstraction, polymorphism, template method, factory)
- JavaFX/FXML UI construction, responsive layout, CSS theming, `Canvas` drawing
- Concurrent programming (`ExecutorService`, `CompletableFuture`, UI-thread marshalling)
- Relational modelling and JDBC CRUD with foreign keys and cascades
- HTTP client programming, retries and error surfacing
- Writing a **parser from scratch** (recursive descent)
- Email delivery over SMTP and secure account-verification design
- Prompt engineering with **grounded** facts
- Algorithm analysis: time/space complexity, best/average/worst cases

---

# 🤝 Contributing

1. Fork the repository and create a feature branch.
2. Keep the layering: algorithms emit `Step`s only; drawing stays in `CanvasRenderer`; SQL stays in `DatabaseManager`.
3. To add an algorithm: create a class extending `ArrayAlgorithm` or `GraphAlgorithm`, register it in `AlgorithmFactory`, and add entries to `AlgorithmCodeSnippets` and `AlgorithmGuide` (guide + facts).
4. Never commit secrets.
5. Open a Pull Request describing the change.

---

# 👨‍💻 Author

**Tasmin Rubaiat Rimve**

**Department of Computer Science & Engineering (CSE)**

**Khulna University of Engineering & Technology (KUET)**

---

# 📄 License

This project is developed for **educational and academic purposes**.

You are welcome to study, modify, and improve the project with proper attribution.

---

# ⭐ Support the Project

If you found this project useful or informative:

⭐ Star this repository

🍴 Fork the repository

📢 Share it with others

---


### 🙏 Acknowledgements

This project was developed as part of the **Advanced Programming Laboratory**, with the aim of applying the programming concepts and technologies introduced throughout the course in a complete, practical software application. The project brings together **Java, JavaFX, FXML, JavaFX effects, SQL and database management, JSON processing, APIs, Git, and GitHub**, demonstrating how these individual concepts can be integrated into a structured application.

The development of **AlgoMentor** provided an opportunity to move beyond isolated programming exercises and apply these technologies together in designing an interactive **algorithm visualization and learning platform**, incorporating graphical interfaces, persistent data management, external API communication, structured data processing, version control, and collaborative software development practices.
