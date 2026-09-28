package com.algomentor.controller;

import com.algomentor.algorithm.AlgorithmFactory;
import com.algomentor.algorithm.Traceable;
import com.algomentor.model.ChatMessage;
import com.algomentor.model.DemoGraph;
import com.algomentor.model.Step;
import com.algomentor.model.StepType;
import com.algomentor.util.AlgorithmCodeSnippets;
import com.algomentor.util.AlgorithmGuide;
import com.algomentor.util.AppExecutors;
import com.algomentor.util.ProblemBank;
import com.algomentor.util.SessionContext;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * The main workspace controller. Owns:
 *  - the playback engine (a JavaFX Timeline stepping through a pre-computed
 *    List&lt;Step&gt;, independent of whichever algorithm produced it),
 *  - the algorithm-agnostic canvas rendering delegate (CanvasRenderer),
 *  - background computation of algorithm traces and AI tips via the shared
 *    thread pool, always marshalled back with Platform.runLater,
 *  - the Practice Problems / Complexity Analysis tabs and the always-visible
 *    AI Mentor Chat sidebar, which reuse the same shared thread pool.
 */
public class MainController {

    @FXML private BorderPane rootPane;
    @FXML private Label userLabel;
    @FXML private ListView<String> algorithmList;
    @FXML private TextField customInputField;
    @FXML private Spinner<Integer> sizeSpinner;
    @FXML private HBox targetBox;
    @FXML private TextField targetField;
    @FXML private Button generateButton;
    @FXML private Button startButton;
    @FXML private Label complexityLabel;
    @FXML private Label descriptionLabel;
    @FXML private StackPane canvasHolder;
    @FXML private Canvas canvas;
    @FXML private Label stepDescriptionLabel;
    @FXML private Button stepBackButton;
    @FXML private Button playPauseButton;
    @FXML private Button stepForwardButton;
    @FXML private Slider progressSlider;
    @FXML private Slider speedSlider;
    @FXML private VBox playbackBar;
    @FXML private TabPane centerTabs;
    @FXML private TextArea codeArea;
    @FXML private Button explainButton;
    @FXML private TextArea rawAlgoArea;
    @FXML private SplitPane visualizerSplit;
    @FXML private VBox codePanel;
    @FXML private VBox guidePanel;
    @FXML private Button codeFoldButton;
    @FXML private Button guideFoldButton;

    // --- Practice Problems tab ---
    @FXML private ComboBox<String> topicCombo;
    @FXML private ScrollPane problemsScroll;
    @FXML private VBox problemsContainer;
    @FXML private Button aiProblemsButton;
    @FXML private TextArea aiProblemsArea;

    // --- AI Mentor Chat sidebar ---
    @FXML private ScrollPane chatScroll;
    @FXML private VBox chatBox;
    @FXML private Label chatContextLabel;
    @FXML private TextField chatInputField;
    @FXML private Button chatSendButton;

    // --- Complexity Analysis tab ---
    @FXML private Label compareCategoryLabel;
    @FXML private Button compareButton;
    @FXML private TextArea compareResultArea;
    @FXML private Label compareTableTitle;
    @FXML private TableView<AlgorithmGuide.Facts> compareTable;

    private final Random random = new Random();
    private CanvasRenderer renderer;

    private int[] currentArray;
    private DemoGraph currentGraph;
    private List<Step> currentSteps;
    private int currentStepIndex = -1;
    private Timeline playbackTimeline;
    private boolean suppressSliderEvents = false;
    private long runStartMillis;

    private final List<ChatMessage> chatHistory = new ArrayList<>();
    private boolean chatBusy = false;
    private String lastCategory = null;
    private String highlightedAlgorithm = null;

    private static final String WELCOME = "Hi! I'm your AlgoMentor. Ask me anything about the algorithm you're "
            + "watching - I can explain it, give a real-life example, or tell you why it is fast or slow. "
            + "Tap a button below or type your own question.";

    @FXML
    public void initialize() {
        userLabel.setText(SessionContext.getCurrentUser() != null
                ? "Signed in as " + SessionContext.getCurrentUser().getUsername() : "");

        algorithmList.getItems().addAll(AlgorithmFactory.NAMES);
        algorithmList.getSelectionModel().selectFirst();
        algorithmList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null) onAlgorithmChanged(val);
        });

        sizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(4, 30, 10));

        renderer = new CanvasRenderer(canvas);
        bindCanvasSize();
        initChat();
        initFolding();
        setupCompareTable();

        progressSlider.setDisable(true);
        progressSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (suppressSliderEvents || currentSteps == null) return;
            int target = (int) Math.round(newV.doubleValue());
            jumpToStep(target);
        });

        // Only the Visualizer tab (index 0) needs the play/pause/step bar.
        centerTabs.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            boolean isVisualizer = newV.intValue() == 0;
            playbackBar.setVisible(isVisualizer);
            playbackBar.setManaged(isVisualizer);
        });

        topicCombo.setItems(FXCollections.observableArrayList(ProblemBank.topics()));
        topicCombo.getSelectionModel().selectFirst();

        onAlgorithmChanged(algorithmList.getSelectionModel().getSelectedItem());
        handleGenerate();
    }

    /** Binds the Canvas dimensions to its holder's dimensions so it resizes with the window. */
    private void bindCanvasSize() {
        canvas.widthProperty().bind(canvasHolder.widthProperty());
        canvas.heightProperty().bind(canvasHolder.heightProperty());
        ChangeListener<Number> redraw = (obs, oldV, newV) -> redrawCurrentStep();
        canvas.widthProperty().addListener(redraw);
        canvas.heightProperty().addListener(redraw);
    }

    private void onAlgorithmChanged(String name) {
        boolean isSearch = AlgorithmFactory.isSearchAlgorithm(name);
        targetBox.setVisible(isSearch);
        targetBox.setManaged(isSearch);

        Traceable sample = sampleFor(name);
        complexityLabel.setText(sample.getComplexity());
        descriptionLabel.setText(sample.getDescription());
        codeArea.setText(AlgorithmCodeSnippets.get(name));
        rawAlgoArea.setText(AlgorithmGuide.rawAlgorithm(name));
        rawAlgoArea.positionCaret(0);
        chatContextLabel.setText("Talking about: " + name + "  (" + sample.getComplexity() + ")");
        handleGenerate();

        String category = AlgorithmFactory.categoryOf(name);
        List<String> compared = new ArrayList<>();
        for (AlgorithmGuide.Facts f : AlgorithmGuide.comparisonSet(category)) compared.add(f.name());
        compareCategoryLabel.setText("Category: " + category + "  (comparing: " + String.join(", ", compared) + ")");
        if (lastCategory != null && !lastCategory.equals(category)) {
            compareResultArea.clear();       // old analysis belongs to another category
            hideCompareTable();
        }
        lastCategory = category;
        if (topicCombo.getItems().contains(category)) {
            topicCombo.getSelectionModel().select(category);
        }
    }

    /** Cheap throwaway instance just to read name/complexity/description without running it. */
    private Traceable sampleFor(String name) {
        if (AlgorithmFactory.isGraphAlgorithm(name)) {
            return AlgorithmFactory.createGraphTraversal(name, new DemoGraph(), 0);
        } else if (AlgorithmFactory.isSearchAlgorithm(name)) {
            return AlgorithmFactory.createSearch(new int[]{1, 2, 3}, 2);
        } else {
            return AlgorithmFactory.createArraySort(name, new int[]{3, 1, 2});
        }
    }

    // ---------------------------------------------------------------
    // Generate input
    // ---------------------------------------------------------------

    @FXML
    private void handleGenerate() {
        stopPlayback();
        String name = algorithmList.getSelectionModel().getSelectedItem();
        currentSteps = null;
        currentStepIndex = -1;
        renderer.resetGraphState();

        if (AlgorithmFactory.isGraphAlgorithm(name)) {
            currentGraph = new DemoGraph();
            currentArray = null;
            renderer.drawGraph(currentGraph);
        } else {
            currentArray = parseOrRandomArray();
            if (AlgorithmFactory.isSearchAlgorithm(name)) {
                java.util.Arrays.sort(currentArray); // binary search needs sorted input
            }
            currentGraph = null;
            drawStaticArray();
        }

        progressSlider.setDisable(true);
        progressSlider.setValue(0);
        stepDescriptionLabel.setText("");
        playPauseButton.setText("\u25B6");
    }

    private int[] parseOrRandomArray() {
        String text = customInputField.getText();
        if (text != null && !text.isBlank()) {
            try {
                String[] parts = text.split(",");
                int[] arr = new int[parts.length];
                for (int i = 0; i < parts.length; i++) arr[i] = Integer.parseInt(parts[i].trim());
                return arr;
            } catch (NumberFormatException e) {
                stepDescriptionLabel.setText("Couldn't parse custom input, using random data instead.");
            }
        }
        int size = sizeSpinner.getValue();
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) arr[i] = 5 + random.nextInt(95);
        return arr;
    }

    private void drawStaticArray() {
        Step fake = new Step(StepType.DONE, List.of(), currentArray, "Ready.");
        renderer.drawArrayStep(fake);
    }

    // ---------------------------------------------------------------
    // Run (computes the full trace on a background thread)
    // ---------------------------------------------------------------

    @FXML
    private void handleStart() {
        stopPlayback();
        String name = algorithmList.getSelectionModel().getSelectedItem();
        startButton.setDisable(true);
        generateButton.setDisable(true);
        stepDescriptionLabel.setText("Computing trace...");
        runStartMillis = System.currentTimeMillis();

        // Trace computation happens off the FX thread on the shared pool -
        // for large inputs this keeps the UI responsive while it runs.
        AppExecutors.get().submit(() -> {
            try {
                Traceable algorithm;
                int inputSize;
                if (AlgorithmFactory.isGraphAlgorithm(name)) {
                    algorithm = AlgorithmFactory.createGraphTraversal(name, currentGraph, 0);
                    inputSize = currentGraph.size();
                } else if (AlgorithmFactory.isSearchAlgorithm(name)) {
                    int target = parseTargetOrRandom();
                    algorithm = AlgorithmFactory.createSearch(currentArray, target);
                    inputSize = currentArray.length;
                } else {
                    algorithm = AlgorithmFactory.createArraySort(name, currentArray);
                    inputSize = currentArray.length;
                }
                List<Step> steps = algorithm.run();
                Platform.runLater(() -> onTraceReady(steps, name, inputSize));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    stepDescriptionLabel.setText("Error: " + e.getMessage());
                    startButton.setDisable(false);
                    generateButton.setDisable(false);
                });
            }
        });
    }

    private int parseTargetOrRandom() {
        String text = targetField.getText();
        if (text != null && !text.isBlank()) {
            try { return Integer.parseInt(text.trim()); } catch (NumberFormatException ignored) {}
        }
        return currentArray[random.nextInt(currentArray.length)];
    }

    private void onTraceReady(List<Step> steps, String algorithmName, int inputSize) {
        this.currentSteps = steps;
        this.currentStepIndex = -1;
        renderer.resetGraphState();
        progressSlider.setDisable(false);
        progressSlider.setMax(Math.max(0, steps.size() - 1));
        startButton.setDisable(false);
        generateButton.setDisable(false);
        jumpToStep(0);
        startPlayback();
        logAttempt(algorithmName, inputSize);
    }

    private void logAttempt(String algorithmName, int inputSize) {
        if (SessionContext.getCurrentUser() == null) return;
        long duration = System.currentTimeMillis() - runStartMillis;
        int userId = SessionContext.getCurrentUser().getId();
        AppExecutors.get().submit(() -> {
            try {
                SessionContext.db().createAttempt(userId, algorithmName, inputSize, duration, null);
            } catch (Exception ignored) {
                // Non-critical: a logging failure shouldn't interrupt the visualization.
            }
        });
    }

    // ---------------------------------------------------------------
    // Playback engine
    // ---------------------------------------------------------------

    @FXML
    private void handlePlayPause() {
        if (currentSteps == null || currentSteps.isEmpty()) return;
        if (playbackTimeline != null && playbackTimeline.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            stopPlayback();
        } else {
            startPlayback();
        }
    }

    private void startPlayback() {
        if (currentSteps == null || currentSteps.isEmpty()) return;
        stopPlayback();
        double intervalMillis = 500 / Math.max(0.1, speedSlider.getValue());
        playbackTimeline = new Timeline(new KeyFrame(Duration.millis(intervalMillis), e -> advanceStep()));
        playbackTimeline.setCycleCount(Timeline.INDEFINITE);
        playbackTimeline.play();
        playPauseButton.setText("\u23F8");
    }

    private void stopPlayback() {
        if (playbackTimeline != null) playbackTimeline.stop();
        playPauseButton.setText("\u25B6");
    }

    private void advanceStep() {
        if (currentStepIndex >= currentSteps.size() - 1) {
            stopPlayback();
            return;
        }
        jumpToStep(currentStepIndex + 1);
    }

    @FXML
    private void handleStepForward() {
        stopPlayback();
        if (currentSteps != null && currentStepIndex < currentSteps.size() - 1) jumpToStep(currentStepIndex + 1);
    }

    @FXML
    private void handleStepBack() {
        stopPlayback();
        if (currentSteps != null && currentStepIndex > 0) {
            // Re-render from the start up to index-1 for graph algorithms since
            // their visited/frontier state is cumulative, not per-step snapshotted.
            int target = currentStepIndex - 1;
            replayGraphUpTo(target);
            jumpToStep(target);
        }
    }

    private void jumpToStep(int index) {
        if (currentSteps == null || index < 0 || index >= currentSteps.size()) return;
        String name = algorithmList.getSelectionModel().getSelectedItem();
        if (AlgorithmFactory.isGraphAlgorithm(name) && index < currentStepIndex) {
            replayGraphUpTo(index);
        }
        currentStepIndex = index;
        Step step = currentSteps.get(index);

        if (AlgorithmFactory.isGraphAlgorithm(name)) {
            renderer.drawGraphStep(currentGraph, step);
        } else {
            renderer.drawArrayStep(step);
        }

        stepDescriptionLabel.setText(step.getDescription());
        suppressSliderEvents = true;
        progressSlider.setValue(index);
        suppressSliderEvents = false;
    }

    /** Rebuilds cumulative visited/frontier graph state by replaying steps 0..upTo. */
    private void replayGraphUpTo(int upTo) {
        renderer.resetGraphState();
        for (int i = 0; i <= upTo && i < currentSteps.size(); i++) {
            renderer.drawGraphStep(currentGraph, currentSteps.get(i));
        }
    }

    private void redrawCurrentStep() {
        if (renderer == null) return;
        if (currentSteps != null && currentStepIndex >= 0) {
            jumpToStep(currentStepIndex);
        } else if (currentGraph != null) {
            renderer.drawGraph(currentGraph);
        } else if (currentArray != null) {
            drawStaticArray();
        }
    }

    // ---------------------------------------------------------------
    // Practice Problems (curated real links + AI-generated originals)
    // ---------------------------------------------------------------

    @FXML
    private void handleFindProblems() {
        String topic = topicCombo.getSelectionModel().getSelectedItem();
        if (topic == null) return;
        problemsContainer.getChildren().clear();
        List<ProblemBank.ProblemLink> problems = ProblemBank.problemsFor(topic);
        if (problems.isEmpty()) {
            problemsContainer.getChildren().add(new Label("No curated problems for this topic yet."));
            return;
        }
        for (ProblemBank.ProblemLink p : problems) {
            Hyperlink link = new Hyperlink(p.title() + "  (" + p.difficulty() + ")");
            link.setOnAction(e -> openInBrowser(p.url()));
            problemsContainer.getChildren().add(link);
        }
    }

    private void openInBrowser(String url) {
        AppExecutors.get().submit(() -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                }
            } catch (Exception ignored) {
                // If the platform can't open a browser, the user can still copy the link from the console.
                System.out.println("Open manually: " + url);
            }
        });
    }

    @FXML
    private void handleGenerateAiProblems() {
        String topic = topicCombo.getSelectionModel().getSelectedItem();
        if (topic == null) return;
        aiProblemsButton.setDisable(true);
        aiProblemsArea.setText("Generating original practice problems...");
        SessionContext.openAi().generatePracticeProblems(topic)
                .thenAccept(text -> Platform.runLater(() -> {
                    aiProblemsArea.setText(text);
                    aiProblemsButton.setDisable(false);
                }));
    }

    // ---------------------------------------------------------------
    // AI Mentor Chat (always-visible right sidebar, live conversation)
    // ---------------------------------------------------------------

    // ---------------------------------------------------------------
    // Collapsible Source Code / Raw Algorithm panels (+ / - buttons)
    // ---------------------------------------------------------------

    /** Width (px) of the thin strip that stays visible when both panels are folded away. */
    private static final double COLLAPSED_STRIP_WIDTH = 260;

    private boolean codeOpen = true;
    private boolean guideOpen = true;
    private boolean sideCollapsed = false;
    private double savedDivider = 0.62;

    private void initFolding() {
        // Keep the strip a fixed width if the window is resized while both panels are folded.
        visualizerSplit.widthProperty().addListener((obs, oldW, newW) -> {
            if (sideCollapsed && newW.doubleValue() > 0 && !visualizerSplit.getDividers().isEmpty()) {
                visualizerSplit.setDividerPositions(stripDivider(newW.doubleValue()));
            }
        });
    }

    private double stripDivider(double width) {
        return Math.max(0.5, 1.0 - COLLAPSED_STRIP_WIDTH / width);
    }

    @FXML
    private void handleToggleCode() {
        codeOpen = !codeOpen;
        applyFold(codePanel, codeArea, codeFoldButton, codeOpen);
        explainButton.setVisible(codeOpen);
        explainButton.setManaged(codeOpen);
        updateSideColumn();
    }

    @FXML
    private void handleToggleGuide() {
        guideOpen = !guideOpen;
        applyFold(guidePanel, rawAlgoArea, guideFoldButton, guideOpen);
        updateSideColumn();
    }

    /** Shows/hides one panel's body. A folded panel shrinks to just its header row. */
    private void applyFold(VBox panel, Node body, Button foldButton, boolean open) {
        body.setVisible(open);
        body.setManaged(open);
        foldButton.setText(open ? "\u2212" : "+");
        VBox.setVgrow(panel, open ? Priority.ALWAYS : Priority.NEVER);
        panel.setPrefHeight(open ? 0 : Region.USE_COMPUTED_SIZE);
    }

    /** When both panels are folded, give the visualization the space; restore it when one reopens. */
    private void updateSideColumn() {
        if (visualizerSplit.getDividers().isEmpty()) return;
        boolean anyOpen = codeOpen || guideOpen;
        double width = visualizerSplit.getWidth();
        if (!anyOpen && !sideCollapsed) {
            savedDivider = visualizerSplit.getDividers().get(0).getPosition();
            sideCollapsed = true;
            visualizerSplit.setDividerPositions(width > 0 ? stripDivider(width) : 0.8);
        } else if (anyOpen && sideCollapsed) {
            sideCollapsed = false;
            visualizerSplit.setDividerPositions(savedDivider);
        }
    }

    private void initChat() {
        // Keep the newest message in view.
        chatBox.heightProperty().addListener((obs, oldV, newV) -> Platform.runLater(() -> chatScroll.setVvalue(1.0)));
        addBubble(WELCOME, false);
    }

    /** Adds one speech bubble (right-aligned purple for the student, left-aligned for the AI). */
    private Label addBubble(String text, boolean fromUser) {
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMinHeight(Region.USE_PREF_SIZE);
        bubble.getStyleClass().add(fromUser ? "bubble-user" : "bubble-ai");
        bubble.maxWidthProperty().bind(chatScroll.widthProperty().multiply(0.82));

        ContextMenu menu = new ContextMenu();
        MenuItem copy = new MenuItem("Copy text");
        copy.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(bubble.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });
        menu.getItems().add(copy);
        bubble.setContextMenu(menu);

        HBox row = new HBox(bubble);
        row.setAlignment(fromUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        chatBox.getChildren().add(row);
        return bubble;
    }

    @FXML
    private void handleSendChat() {
        sendChatMessage(chatInputField.getText());
    }

    private void sendChatMessage(String message) {
        if (message == null || message.isBlank() || chatBusy) return;
        message = message.trim();
        addBubble(message, true);
        chatHistory.add(new ChatMessage("user", message));
        chatInputField.clear();
        setChatBusy(true);
        Label thinking = addBubble("Thinking...", false);

        String algorithm = algorithmList.getSelectionModel().getSelectedItem();
        String complexity = complexityLabel.getText();
        // Only send the most recent turns: keeps requests small (free-tier token limits).
        int from = Math.max(0, chatHistory.size() - 12);
        List<ChatMessage> recent = List.copyOf(chatHistory.subList(from, chatHistory.size()));

        SessionContext.openAi().chat(recent, algorithm, complexity).whenComplete((reply, ex) ->
                Platform.runLater(() -> {
                    boolean failed = ex != null || reply == null
                            || reply.startsWith("Could not reach") || reply.contains("_API_KEY");
                    if (failed) {
                        thinking.setText(ex != null ? "Something went wrong: " + ex.getMessage() : reply);
                        // Drop the unanswered question so a retry starts clean.
                        chatHistory.remove(chatHistory.size() - 1);
                    } else {
                        thinking.setText(reply);
                        chatHistory.add(new ChatMessage("assistant", reply));
                    }
                    setChatBusy(false);
                }));
    }

    private void setChatBusy(boolean busy) {
        chatBusy = busy;
        chatSendButton.setDisable(busy);
        chatInputField.setDisable(busy);
        if (!busy) chatInputField.requestFocus();
    }

    @FXML
    private void handleClearChat() {
        if (chatBusy) return;
        chatBox.getChildren().clear();
        chatHistory.clear();
        addBubble(WELCOME, false);
    }

    @FXML
    private void handleQuickExplain() {
        sendChatMessage("Explain how " + currentAlgorithmName() + " works in simple words, step by step.");
    }

    @FXML
    private void handleQuickExample() {
        sendChatMessage("Give me a real-life example of " + currentAlgorithmName()
                + " and a tiny worked example with real numbers.");
    }

    @FXML
    private void handleQuickWhy() {
        sendChatMessage("Why is " + currentAlgorithmName() + " " + complexityLabel.getText()
                + "? Explain the reason behind that speed and when it is best or worst.");
    }

    /** "Ask AI Mentor" button beside the code: asks the sidebar chat to walk through the code. */
    @FXML
    private void handleExplainWithAi() {
        sendChatMessage("Walk me through the source code of " + currentAlgorithmName()
                + " in simple words, part by part.");
    }

    private String currentAlgorithmName() {
        String name = algorithmList.getSelectionModel().getSelectedItem();
        return name != null ? name : "this algorithm";
    }

    // ---------------------------------------------------------------
    // Complexity Analysis (what / why / how + verdict, then a summary table)
    // ---------------------------------------------------------------

    private void setupCompareTable() {
        compareTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        compareTable.setPlaceholder(new Label("Run the analysis to see the summary table."));
        compareTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(AlgorithmGuide.Facts item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null && item.name().equals(highlightedAlgorithm)) {
                    setStyle("-fx-background-color: rgba(99,102,241,0.35);");
                } else {
                    setStyle("");
                }
            }
        });
        addCompareColumn("Algorithm", 150, AlgorithmGuide.Facts::name);
        addCompareColumn("Best", 95, AlgorithmGuide.Facts::best);
        addCompareColumn("Average", 95, AlgorithmGuide.Facts::average);
        addCompareColumn("Worst", 95, AlgorithmGuide.Facts::worst);
        addCompareColumn("Space", 70, AlgorithmGuide.Facts::space);
        addCompareColumn("Key trait & best use", 240, AlgorithmGuide.Facts::traitAndUse);
    }

    private void addCompareColumn(String title, double width, Function<AlgorithmGuide.Facts, String> getter) {
        TableColumn<AlgorithmGuide.Facts, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setSortable(false);
        col.setCellValueFactory(cd -> new ReadOnlyStringWrapper(getter.apply(cd.getValue())));
        col.setCellFactory(c -> new WrapCell(col));
        compareTable.getColumns().add(col);
    }

    /** Table cell that wraps long text instead of cutting it off with "...". */
    private static final class WrapCell extends TableCell<AlgorithmGuide.Facts, String> {
        private final Text text = new Text();

        WrapCell(TableColumn<AlgorithmGuide.Facts, String> column) {
            text.setFill(Color.web("#e8e8ff"));
            text.setStyle("-fx-font-size: 12px;");
            text.wrappingWidthProperty().bind(column.widthProperty().subtract(18));
            setPrefHeight(Control.USE_COMPUTED_SIZE);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                text.setText(item);
                setGraphic(text);
            }
        }
    }

    private void hideCompareTable() {
        compareTableTitle.setVisible(false);
        compareTableTitle.setManaged(false);
        compareTable.setVisible(false);
        compareTable.setManaged(false);
    }

    private void showCompareTable(List<AlgorithmGuide.Facts> facts, String currentName) {
        highlightedAlgorithm = currentName;
        compareTable.setItems(FXCollections.observableArrayList(facts));
        compareTable.refresh();
        compareTableTitle.setText("Summary table  (your current algorithm is highlighted)");
        compareTableTitle.setVisible(true);
        compareTableTitle.setManaged(true);
        compareTable.setVisible(true);
        compareTable.setManaged(true);
    }

    @FXML
    private void handleCompareAlgorithms() {
        String currentName = algorithmList.getSelectionModel().getSelectedItem();
        String category = AlgorithmFactory.categoryOf(currentName);
        List<AlgorithmGuide.Facts> facts = AlgorithmGuide.comparisonSet(category);

        if (facts.size() < 2) {
            compareResultArea.setText("Nothing to compare yet for \"" + category
                    + "\". Try a Sorting or Graph algorithm.");
            return;
        }

        compareButton.setDisable(true);
        hideCompareTable();
        compareResultArea.setText("Analyzing " + facts.size() + " algorithms in \"" + category
                + "\" - explaining what they do, why their speeds differ, and which to pick...");
        SessionContext.openAi().analyzeComplexity(category, currentName, facts)
                .whenComplete((result, ex) -> Platform.runLater(() -> {
                    compareResultArea.setText(ex != null ? "Analysis failed: " + ex.getMessage() : result);
                    compareResultArea.positionCaret(0);
                    showCompareTable(facts, currentName);   // the table is the summary, shown after the analysis
                    compareButton.setDisable(false);
                }));
    }

    // ---------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------

    @FXML
    private void handleOpenHistory() {
        try {
            stopPlayback();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/history.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception ignored) {}
    }

    @FXML
    private void handleLogout() {
        stopPlayback();
        SessionContext.setCurrentUser(null);
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception ignored) {}
    }
}
