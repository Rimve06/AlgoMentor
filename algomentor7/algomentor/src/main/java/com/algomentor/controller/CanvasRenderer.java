package com.algomentor.controller;

import com.algomentor.model.DemoGraph;
import com.algomentor.model.GraphNode;
import com.algomentor.model.Step;
import com.algomentor.model.StepType;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.HashSet;
import java.util.Set;

/**
 * Draws a single Step onto a Canvas. Only knows about StepType, never about
 * which concrete algorithm produced the step - this is what lets one
 * renderer serve bubble sort, merge sort, binary search, BFS and DFS alike.
 */
public class CanvasRenderer {
    private final Canvas canvas;

    // Cumulative graph traversal state (visited/frontier persist across steps
    // within one run, since graph steps don't carry a full snapshot).
    private final Set<Integer> visitedNodes = new HashSet<>();
    private final Set<Integer> frontierNodes = new HashSet<>();

    public CanvasRenderer(Canvas canvas) {
        this.canvas = canvas;
    }

    public void resetGraphState() {
        visitedNodes.clear();
        frontierNodes.clear();
    }

    public void clear() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        // Deep-space gradient instead of flat near-black, matching the app theme.
        gc.setFill(new javafx.scene.paint.LinearGradient(0, 0, 1, 1, true,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#0d1030")),
                new javafx.scene.paint.Stop(0.55, Color.web("#1a1240")),
                new javafx.scene.paint.Stop(1, Color.web("#2a1140"))));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public void drawArrayStep(Step step) {
        clear();
        int[] data = step.getArraySnapshot();
        if (data == null || data.length == 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        double padding = 20;
        double barGap = 4;
        double barWidth = (w - 2 * padding - barGap * (data.length - 1)) / data.length;
        int max = 1;
        for (int v : data) max = Math.max(max, v);

        Set<Integer> highlighted = new HashSet<>(step.getIndices());
        boolean isFound = step.getType() == StepType.MARK_FOUND;

        for (int i = 0; i < data.length; i++) {
            double barHeight = (data[i] / (double) max) * (h - 2 * padding - 24);
            double x = padding + i * (barWidth + barGap);
            double y = h - padding - barHeight;

            Color color;
            if (isFound && highlighted.contains(i)) {
                color = Color.web("#4ade80"); // green - found
            } else if (highlighted.contains(i) && step.getType() == StepType.SWAP) {
                color = Color.web("#f87171"); // red - swapping
            } else if (highlighted.contains(i)) {
                color = Color.web("#fbbf24"); // amber - comparing/overwriting
            } else {
                color = Color.web("#60a5fa"); // blue - default
            }

            gc.setFill(color);
            gc.fillRoundRect(x, y, barWidth, barHeight, 4, 4);

            gc.setFill(Color.web("#e5e7eb"));
            gc.setFont(Font.font(11));
            String label = String.valueOf(data[i]);
            gc.fillText(label, x + barWidth / 2 - label.length() * 3, h - padding + 14);
        }
    }

    public void drawGraphStep(DemoGraph graph, Step step) {
        switch (step.getType()) {
            case ENQUEUE -> frontierNodes.addAll(step.getIndices());
            case DEQUEUE -> frontierNodes.removeAll(step.getIndices());
            case VISIT -> visitedNodes.addAll(step.getIndices());
            default -> {}
        }
        drawGraph(graph);
    }

    public void drawGraph(DemoGraph graph) {
        clear();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double sx = canvas.getWidth() / 700.0;
        double sy = canvas.getHeight() / 460.0;
        double scale = Math.min(sx, sy);
        double offsetX = (canvas.getWidth() - 700 * scale) / 2;
        double offsetY = (canvas.getHeight() - 460 * scale) / 2;

        gc.setStroke(Color.web("#6d72b8"));
        gc.setLineWidth(2);
        for (GraphNode node : graph.getNodes().values()) {
            for (int neighborId : node.getNeighbors()) {
                if (neighborId < node.getId()) continue; // draw each edge once
                GraphNode other = graph.get(neighborId);
                gc.strokeLine(offsetX + node.getX() * scale, offsetY + node.getY() * scale,
                        offsetX + other.getX() * scale, offsetY + other.getY() * scale);
            }
        }

        double r = 22 * scale;
        for (GraphNode node : graph.getNodes().values()) {
            double cx = offsetX + node.getX() * scale;
            double cy = offsetY + node.getY() * scale;

            Color fill;
            if (visitedNodes.contains(node.getId())) fill = Color.web("#4ade80");
            else if (frontierNodes.contains(node.getId())) fill = Color.web("#fbbf24");
            else fill = Color.web("#60a5fa");

            gc.setFill(fill);
            gc.fillOval(cx - r, cy - r, r * 2, r * 2);
            gc.setStroke(Color.web("#12131a"));
            gc.setLineWidth(2);
            gc.strokeOval(cx - r, cy - r, r * 2, r * 2);

            gc.setFill(Color.web("#12131a"));
            gc.setFont(Font.font(13));
            String label = String.valueOf(node.getId());
            gc.fillText(label, cx - label.length() * 4, cy + 5);
        }
    }
}