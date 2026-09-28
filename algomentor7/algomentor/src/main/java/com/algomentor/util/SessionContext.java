package com.algomentor.util;

import com.algomentor.db.DatabaseManager;
import com.algomentor.model.User;
import com.algomentor.network.AiAnalysisService;
import com.algomentor.network.EmailService;
import com.algomentor.network.OpenAiChatService;

/**
 * Small app-wide singleton holding things every controller needs: the DB
 * connection, the currently logged-in user, and the AI/email services.
 * Simpler than threading these through FXMLLoader controller factories for
 * a project this size, while keeping each controller's constructor free of
 * boilerplate.
 */
public final class SessionContext {
    private static DatabaseManager databaseManager;
    private static AiAnalysisService aiAnalysisService;
    private static OpenAiChatService openAiChatService;
    private static EmailService emailService;
    private static User currentUser;

    private SessionContext() {}

    public static void init(DatabaseManager db) {
        databaseManager = db;
        aiAnalysisService = new AiAnalysisService();
        openAiChatService = new OpenAiChatService();
        emailService = new EmailService();
    }

    public static DatabaseManager db() { return databaseManager; }
    public static AiAnalysisService ai() { return aiAnalysisService; }
    public static OpenAiChatService openAi() { return openAiChatService; }
    public static EmailService email() { return emailService; }

    public static User getCurrentUser() { return currentUser; }
    public static void setCurrentUser(User user) { currentUser = user; }
}
