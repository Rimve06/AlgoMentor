# AlgoMentor — real credentials, no more "offline mode"

## The one thing you must do: create `algomentor.properties`
Copy `algomentor.properties.example` (project root, next to `pom.xml`) to a new file
named `algomentor.properties`, and fill in real values:

```
MAIL_USERNAME=youraddress@gmail.com
MAIL_PASSWORD=your16digitapppassword
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4o-mini
ANTHROPIC_API_KEY=
```

This file is git-ignored — it will never be committed or pushed, so it's safe to put real
secrets in it. The app reads this file directly (no environment-variable setup needed in
IntelliJ's Run Configuration anymore), and **prints a clear report to the console on
startup** telling you exactly which credentials it found and which features they unlock,
so you'll never be left guessing why something looks offline.

### About MAIL_PASSWORD — please read this part
This is the one piece that trips people up, so to be direct about it: **Gmail's SMTP
server flatly refuses your normal account password from any third-party app — there is no
way around this, it's a Google security policy, not a limitation of this project.** The
fix is free and takes two minutes: Google Account → Security → 2-Step Verification → App
passwords → generate one → paste the 16-character result as `MAIL_PASSWORD`. After that
it behaves exactly like ordinary username+password login — no OAuth screens, no extra
"authenticate this app" popups, nothing more complex than what you already have.

### About OPENAI_API_KEY
Every AI feature (live Chat, Compare Algorithms, AI-generated practice problems) now
reports the **real failure reason** if something's wrong (bad key, no quota, wrong model
name) instead of quietly falling back to a canned response — so if it still looks
"offline" once the key is set, the on-screen message will tell you exactly why (e.g. a
401 means the key itself is wrong, a 429 means you're out of quota).

## What changed in this update
1. **Config file instead of environment variables** — `AppConfig` reads
   `algomentor.properties` first, environment variables second. Much harder to
   misconfigure than an IDE run-config field.
2. **Real error surfacing everywhere AI/email is used** — a wrong/expired key now shows
   the actual reason instead of silently behaving like no key was set at all. This was a
   genuine bug in the live AI Chat specifically (a failed request just left it stuck on
   "thinking..." forever) — fixed.
3. **Source code panel** — every algorithm's Visualizer tab now has a split view: the
   animation on the left, the exact real Java logic that's running on the right (not
   pseudocode — the literal code from that algorithm's class). An "Explain with AI" button
   jumps to the AI Chat tab with a question about it pre-filled.
4. **Full colorful glassmorphism redesign** — replaced the flat black/white/blue theme
   with layered gradients (indigo/violet/pink/teal), translucent "glass" cards, glowing
   borders, and — importantly — **every control (buttons, dropdowns, tabs) now has its
   text and background color set explicitly in its default state**, not only on hover.
   That "white on white until you hover" bug is fixed everywhere, not just patched in one
   spot.
5. **Jakarta Mail** (the current maintained library) replaces the older `javax.mail`
   artifact; JavaFX/SQLite/Maven plugin versions bumped up; `maven.compiler.release` set
   to 27.

## Honest caveat on dependency versions
I bumped `sqlite-jdbc`, `javafx`, `jakarta.mail`, and the Maven plugins to the newest
version numbers I'm confident actually exist, and set the compiler to target JDK 27 as
you asked. I can't reach Maven Central from where I built this to double-check the exact
latest patch number of each artifact right now — if Maven reports a specific version
can't be resolved, it's almost always safe to just drop the last digit (e.g. `27.0.1` →
`27.0.0`, or `javafx.version` down to the `21.0.x` LTS line) since nothing in this
codebase depends on a version-specific feature.

## Run it
```
mvn clean javafx:run
```
Watch the console on launch — the configuration report prints there first, before the
window even opens.

---

## Round 2 fixes (real bugs, not config issues)

1. **Orphaned username/email lock — fixed for real.** Previously, a `users` row was
   created the moment someone clicked Register, before the code was verified. If they
   never finished verifying, that username/email was permanently stuck forever. Now
   registration only writes to a `pending_registrations` table; a real account is only
   ever created in `users` the instant the correct code is verified. An abandoned or
   wrong code no longer blocks anything — that username/email stays completely free to
   use again.
2. **Gmail auth failures now say exactly what's wrong**, and spaces pasted from Google's
   App Password display (shown as `abcd efgh ijkl mnop`) are now stripped automatically.
   A made-up password (or your real Gmail login password) will still be rejected by
   Google — that's Google's rule, not this app's — but the app now tells you that
   explicitly instead of a generic error.
3. **The Anthropic 401 error was a key mix-up, not a bug**: the `ANTHROPIC_API_KEY` value
   supplied (`sk-proj-...`) is an **OpenAI** key format, not Anthropic's (`sk-ant-...`).
   Anthropic correctly rejected it. The app now detects this exact situation at startup
   *and* before making any wasted request, and tells you directly: "this looks like an
   OpenAI key, move it to OPENAI_API_KEY." Move that real key over, and OpenAI features
   (Chat / Compare / Practice Problems) will work immediately with it — `OPENAI_API_KEY`
   was still the placeholder `sk-...` in what was pasted, so it wasn't actually configured.

---

## Round 3

**AI chat with no paid API key:** added Groq as the default AI provider. Groq's free tier
needs an account and a key, but genuinely **no credit card, ever** — console.groq.com, sign
up, "API Keys" → create, paste as `GROQ_API_KEY`. Live chat, Compare Algorithms, and AI
problem generation all use it automatically (`AI_PROVIDER=groq` is the default). If you get
an OpenAI key later, set `AI_PROVIDER=openai` and it switches over with zero code changes.
I can't generate or share a working key on your behalf — there's no way for me to hand you
one that would actually work under your name — but this is the closest real, free path.

**Why old accounts kept showing up after a fresh install:** the database file lives at
`~/.algomentor/algomentor.db` (in your **home folder**, not the project folder), so
replacing the project's zip never touched it — every earlier test account (including ones
created by older, buggier versions of this project, before last round's registration fix)
was still sitting in that file. Two ways to clear it:
- **Easiest:** set `RESET_DB_ON_START=true` in `algomentor.properties`, run the app once
  (check the console — it'll confirm the old file was deleted), then set it back to
  `false` so you don't wipe real data on later runs.
- **Manual:** just delete the file yourself — the console prints its exact path every time
  the app starts.

With that file cleared and last round's registration fix in place, a username/email is
never reserved until its code is actually verified, so this specific problem shouldn't
recur going forward.

---

## Update: AI Mentor Chat, Raw Algorithm panel, better Complexity Analysis, new colours

- **AI Tips box removed.** The right sidebar is now the **AI Mentor Chat** (live chat with speech
  bubbles, quick buttons: Explain it / Example / Why this speed?, right-click a bubble to copy).
  The chat always knows which algorithm is open in the visualizer.
- **Source Code panel** now has a **Raw Algorithm & Tips** panel under it: plain-English steps,
  a "Picture it" imagination tip and a "Watch for" hint for all 10 algorithms (works offline).
- **Complexity Analysis tab** (was "Compare Algorithms"): the AI now writes WHAT each algorithm does,
  WHY its speed is what it is, a HEAD-TO-HEAD comparison, a VERDICT, and where your current algorithm
  ranks. A **summary table** (best / average / worst / space / key trait) appears under the analysis.
  The Big-O numbers come from verified data in `AlgorithmGuide.java`, not from the AI's memory.
  Binary Search is compared with Linear Search as a baseline.
- **Model:** `GROQ_MODEL=openai/gpt-oss-20b` (Groq moved the Llama models behind Enterprise access).
  If a model ever returns "model_not_found" again, open https://console.groq.com/docs/models and put
  any currently-listed model id into `GROQ_MODEL` in `algomentor.properties`.
- **Colours:** panels were flat because a solid colour was painted on top of their gradient; fixed in
  `style.css` so the login screen's blue/violet/pink glow now runs through the whole app.
