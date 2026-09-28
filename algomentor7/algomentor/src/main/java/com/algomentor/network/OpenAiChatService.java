package com.algomentor.network;

import com.algomentor.model.ChatMessage;
import com.algomentor.util.AlgorithmGuide;
import com.algomentor.util.AppConfig;
import com.algomentor.util.AppExecutors;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * All ChatGPT (OpenAI) integration lives here: a live free-form chat, a
 * "compare these algorithms" analysis, and topic-based practice-problem
 * generation. Every call reuses {@link ApiClient} (HTTP off the FX thread)
 * and {@link JsonValue}/{@link JsonParser} (the hand-written JSON layer
 * already used elsewhere in the app) - no new networking/JSON machinery.
 *
 * The system prompts below are deliberately never shown in the UI: only the
 * model's final answer is displayed, so the person using the app sees a
 * clean response, not the instructions steering it.
 *
 * Supports two providers, picked via AI_PROVIDER in algomentor.properties:
 *   - "groq" (default) - Groq's free tier needs an API key but genuinely NO
 *     credit card, no payment info at all (console.groq.com, sign up, create
 *     a key). This exists specifically for a project with no budget.
 *   - "openai" - if you do have an OpenAI key, set AI_PROVIDER=openai.
 * Groq's API is OpenAI-compatible (same request/response JSON), so both
 * providers share every line of code below except the URL/key/model.
 *
 * Every call surfaces the REAL failure reason (wrong key, no quota, bad
 * model name, network error) instead of quietly pretending everything is
 * fine - if this looks like "offline mode", the returned text says why.
 */
public class OpenAiChatService {

    private final ApiClient apiClient = new ApiClient(AppExecutors.get());

    private boolean usingGroq() { return !"openai".equalsIgnoreCase(AppConfig.get("AI_PROVIDER", "groq")); }

    private String apiUrl() {
        return usingGroq() ? "https://api.groq.com/openai/v1/chat/completions"
                            : "https://api.openai.com/v1/chat/completions";
    }

    private String apiKey() {
        return usingGroq() ? AppConfig.get("GROQ_API_KEY") : AppConfig.get("OPENAI_API_KEY");
    }

    private String model() {
        return usingGroq() ? AppConfig.get("GROQ_MODEL", "openai/gpt-oss-20b")
                            : AppConfig.get("OPENAI_MODEL", "gpt-4o-mini");
    }

    private String providerLabel() { return usingGroq() ? "Groq" : "OpenAI"; }

    private String keyProblem() {
        String key = apiKey();
        String keyName = usingGroq() ? "GROQ_API_KEY" : "OPENAI_API_KEY";
        if (key == null || key.isBlank()) {
            return usingGroq()
                ? "GROQ_API_KEY is not set. Groq is free with no credit card required: create an "
                  + "account at console.groq.com, make an API key, paste it as GROQ_API_KEY in "
                  + "algomentor.properties, then restart the app."
                : "OPENAI_API_KEY is not set. Add it to algomentor.properties (project root) - "
                  + "get a key at platform.openai.com/api-keys - then restart the app.";
        }
        if (key.startsWith("sk-ant-")) {
            return keyName + " currently holds an Anthropic key (starts with \"sk-ant-\"). "
                    + "Move it to ANTHROPIC_API_KEY, and put a real " + providerLabel() + " key here instead.";
        }
        return null;
    }

    /** Live back-and-forth chat (no algorithm context). */
    public CompletableFuture<String> chat(List<ChatMessage> history) {
        return chat(history, null, null);
    }

    /**
     * Live back-and-forth chat. `history` is the full conversation so far.
     * `algorithm` / `complexity` (may be null) tell the mentor what the student is
     * currently looking at, so "explain this" style questions get a relevant answer.
     */
    public CompletableFuture<String> chat(List<ChatMessage> history, String algorithm, String complexity) {
        String problem = keyProblem();
        if (problem != null) {
            return CompletableFuture.completedFuture(problem);
        }
        StringBuilder sp = new StringBuilder();
        sp.append("You are AlgoMentor, a warm, clear AI tutor inside a desktop app that visualizes ")
          .append("data structures and algorithms. You are talking to a student. ")
          .append("Explain simply first, then add depth if useful. Use short everyday analogies and, when it helps, ")
          .append("a tiny example with real numbers. Keep answers under 180 words. ")
          .append("Use plain text only: NO markdown, no asterisks, no # headings, no backticks. ")
          .append("Use short paragraphs, or lines starting with '- ' for lists. ")
          .append("If the student asks something unrelated to computer science, gently steer back.");
        if (algorithm != null && !algorithm.isBlank()) {
            sp.append(" The student currently has this algorithm open in the visualizer: ").append(algorithm);
            if (complexity != null && !complexity.isBlank()) sp.append(" (").append(complexity).append(")");
            sp.append(". If they say 'this' or 'it', they mean that algorithm.");
        }
        JsonValue messages = JsonValue.newArray();
        messages.asArray().add(messageObject("system", sp.toString()));
        for (ChatMessage m : history) {
            messages.asArray().add(messageObject(m.role(), m.content()));
        }
        return complete(messages, 1500)
                .exceptionally(ex -> "Could not reach " + providerLabel() + ": " + rootMessage(ex));
    }

    /**
     * The "Complexity Analysis" feature. The verified Big-O facts are passed IN as ground truth,
     * so the model explains WHAT / WHY / HOW instead of guessing numbers, and ends with a verdict.
     */
    public CompletableFuture<String> analyzeComplexity(String category, String currentAlgorithm,
                                                       List<AlgorithmGuide.Facts> algorithms) {
        String problem = keyProblem();
        if (problem != null) {
            return CompletableFuture.completedFuture(problem);
        }
        StringBuilder facts = new StringBuilder();
        for (AlgorithmGuide.Facts a : algorithms) {
            facts.append("- ").append(a.name())
                    .append(" | best: ").append(a.best())
                    .append(" | average: ").append(a.average())
                    .append(" | worst: ").append(a.worst())
                    .append(" | extra space: ").append(a.space())
                    .append(" | traits: ").append(a.trait())
                    .append(" | best used when: ").append(a.useWhen())
                    .append("\n");
        }
        String systemPrompt = "You are an excellent algorithms professor writing a study note for a student who "
                + "is seeing these algorithms for the first time. Your goal: after reading, the student can explain "
                + "WHAT each algorithm does, WHY it has its complexity, and HOW to choose between them.\n\n"
                + "GROUND RULES\n"
                + "- The complexity table in the user message is verified ground truth. Never contradict it and never invent other numbers for it.\n"
                + "- Plain text only. No markdown: no asterisks, no # symbols, no backticks, no tables.\n"
                + "- Write for a beginner: short sentences, everyday words, one small analogy per algorithm.\n"
                + "- Be concrete. Wherever you can, use real numbers (for example: 'for 1,000,000 items, O(n^2) is about a trillion steps, O(n log n) is about 20 million').\n"
                + "- Total length about 450 to 650 words.\n\n"
                + "USE EXACTLY THESE SECTION HEADINGS, in capital letters, each on its own line, with a blank line after:\n\n"
                + "WHAT EACH ONE DOES\n"
                + "For every algorithm, one line starting with its name: what it does in plain words plus a tiny real-world analogy.\n\n"
                + "WHY THE SPEEDS ARE WHAT THEY ARE\n"
                + "For every algorithm, explain the reason behind its Big-O in one or two sentences (for example: two nested loops means n times n comparisons; halving the problem each step means log n levels). "
                + "Say what input causes its best case and what input causes its worst case, and why it uses the extra memory it does.\n\n"
                + "HEAD-TO-HEAD\n"
                + "Compare them on: how speed grows as data grows, memory use, predictability (does worst case differ from average?), and simplicity to write. Use a few short '- ' lines.\n\n"
                + "VERDICT\n"
                + "- Best all-round choice: name it and give the main reason.\n"
                + "- Weakest choice for big data: name it and give its concrete drawback.\n"
                + "- Surprise: one situation where a 'weaker' algorithm is actually the right pick, and why.\n"
                + "- Rule of thumb: one sentence of the form 'If ..., use ...; if ..., use ...'.\n\n"
                + "YOUR CURRENT ALGORITHM\n"
                + "Two sentences about where the algorithm the student has open ranks and one thing to try in the visualizer to see that behaviour.";
        String userPrompt = "Category: " + category + "\n"
                + "Algorithm the student currently has open: " + currentAlgorithm + "\n\n"
                + "Verified complexity table (ground truth):\n" + facts;
        JsonValue messages = JsonValue.newArray();
        messages.asArray().add(messageObject("system", systemPrompt));
        messages.asArray().add(messageObject("user", userPrompt));
        return complete(messages, 3000)
                .exceptionally(ex -> "Could not reach " + providerLabel() + " for the analysis: " + rootMessage(ex));
    }

    /** Generates a few original practice problems for a topic (text only, no fabricated links). */
    public CompletableFuture<String> generatePracticeProblems(String topic) {
        String problem = keyProblem();
        if (problem != null) {
            return CompletableFuture.completedFuture(problem);
        }
        String systemPrompt = "You are an algorithms teaching assistant. Write exactly 3 short original " +
                "practice problem statements (numbered 1-3) about " + topic + ", each 2-3 sentences, " +
                "increasing in difficulty. Plain text only, no markdown, no external links (these are " +
                "original problems, not copies of real ones).";
        JsonValue messages = JsonValue.newArray();
        messages.asArray().add(messageObject("system", systemPrompt));
        messages.asArray().add(messageObject("user", "Generate the 3 problems now."));
        return complete(messages, 1500)
                .exceptionally(ex -> "Could not reach " + providerLabel() + " for problem generation: " + rootMessage(ex));
    }

    private String rootMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        String msg = cause.getMessage();
        if (msg == null || msg.isBlank()) return cause.getClass().getSimpleName();
        if (cause instanceof java.io.IOException) {
            return msg + " (the connection dropped even after 3 automatic retries - check your internet/VPN and try again)";
        }
        return msg;
    }

    private JsonValue messageObject(String role, String content) {
        JsonValue m = JsonValue.newObject();
        m.put("role", JsonValue.ofString(role));
        m.put("content", JsonValue.ofString(content));
        return m;
    }

    private CompletableFuture<String> complete(JsonValue messages, int maxTokens) {
        JsonValue body = JsonValue.newObject();
        body.put("model", JsonValue.ofString(model()));
        body.put("messages", messages);
        body.put("max_tokens", JsonValue.ofNumber(maxTokens));
        // gpt-oss models are "reasoning" models: hidden thinking tokens count against the
        // token budget. Low effort keeps replies fast and stops the thinking from eating the answer.
        if (usingGroq() && model().startsWith("openai/gpt-oss")) {
            body.put("reasoning_effort", JsonValue.ofString("low"));
        }

        return apiClient.postJson(apiUrl(), body.toJson(), "Authorization", "Bearer " + apiKey())
                .thenApply(this::extractText);
    }

    private String extractText(JsonValue response) {
        JsonValue choices = response.get("choices");
        if (choices != null && choices.isArray() && !choices.asArray().isEmpty()) {
            JsonValue first = choices.asArray().get(0);
            JsonValue message = first.get("message");
            if (message != null) {
                JsonValue content = message.get("content");
                if (content != null && !content.isNull()) {
                    String text = cleanReply(content.asString());
                    if (!text.isBlank()) return text;
                    throw new RuntimeException(providerLabel() + " returned an empty answer - please try again.");
                }
            }
        }
        JsonValue error = response.get("error");
        if (error != null) {
            JsonValue msg = error.get("message");
            if (msg != null) throw new RuntimeException(providerLabel() + " error: " + msg.asString());
        }
        throw new RuntimeException("Unexpected " + providerLabel() + " response shape: " + response.toJson());
    }

    /** The UI shows plain text, so strip the markdown characters models like to sneak in anyway. */
    private String cleanReply(String text) {
        if (text == null) return "";
        String t = text.replace("**", "").replace("__", "").replace("`", "");
        t = t.replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "");
        t = t.replaceAll("(?m)^\\s*[*]\\s+", "- ");
        return t.trim();
    }
}
