package com.algomentor.network;

import com.algomentor.util.AppConfig;
import com.algomentor.util.AppExecutors;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Sends real emails over SMTP (Gmail by default) using Jakarta Mail - this
 * is genuine, real email delivery, not a simulation.
 *
 * Configuration (set in algomentor.properties, or as environment variables):
 *   MAIL_USERNAME - the Gmail address AlgoMentor sends FROM
 *   MAIL_PASSWORD - a Gmail "App Password". This is NOT your normal Gmail
 *                   password - Google's SMTP servers refuse plain account
 *                   passwords from third-party apps as a security measure,
 *                   full stop, no way around it. An App Password is a free,
 *                   one-time-generated 16-character code (Google Account ->
 *                   Security -> 2-Step Verification -> App passwords) that
 *                   you then use exactly like a normal password - it is
 *                   still simple username+password authentication, nothing
 *                   more complex, no OAuth screens, no per-request approval.
 *
 * If MAIL_USERNAME / MAIL_PASSWORD are not set, this class does NOT crash
 * or silently pretend to work - it prints the email content to the console
 * AND returns a result string that says exactly that, so it's obvious (not
 * hidden) when the app is running without real email configured.
 */
public class EmailService {

    private final String fromAddress = AppConfig.get("MAIL_USERNAME");
    private final String password = stripSpaces(AppConfig.get("MAIL_PASSWORD"));
    private final String smtpHost = AppConfig.get("MAIL_SMTP_HOST", "smtp.gmail.com");
    private final String smtpPort = AppConfig.get("MAIL_SMTP_PORT", "587");

    private boolean isConfigured() {
        return fromAddress != null && !fromAddress.isBlank()
                && password != null && !password.isBlank();
    }

    /** Google displays App Passwords grouped in 4s ("abcd efgh ijkl mnop") - strip any pasted-in spaces. */
    private static String stripSpaces(String value) {
        return value == null ? null : value.replace(" ", "").trim();
    }

    public CompletableFuture<String> sendVerificationEmail(String toEmail, String username, String code) {
        String subject = "Verify your AlgoMentor account";
        String body = "Hi " + username + ",\n\n"
                + "Welcome to AlgoMentor! Use this verification code to activate your account:\n\n"
                + "    " + code + "\n\n"
                + "This code expires in 10 minutes. If you didn't request this, you can ignore this email.\n\n"
                + "- AlgoMentor";
        return sendAsync(toEmail, subject, body);
    }

    public CompletableFuture<String> sendWelcomeEmail(String toEmail, String username) {
        String subject = "Welcome to AlgoMentor, " + username + "!";
        String body = "Hi " + username + ",\n\n"
                + "Your email is verified and your AlgoMentor account is ready to go.\n\n"
                + "Inside the app you can:\n"
                + "  - Visualize sorting, searching, and graph algorithms step by step, with the\n"
                + "    real Java source for each one shown right alongside the animation\n"
                + "  - Chat live with the AI mentor, and get an AI comparison of which algorithm\n"
                + "    is genuinely best (and which is weak) for a given problem type\n"
                + "  - Practice with curated real problems and AI-generated originals\n\n"
                + "Happy learning!\n"
                + "- AlgoMentor";
        return sendAsync(toEmail, subject, body);
    }

    private CompletableFuture<String> sendAsync(String toEmail, String subject, String body) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConfigured()) {
                System.out.println("[EmailService] MAIL_USERNAME/MAIL_PASSWORD not configured - "
                        + "printing email instead of sending it. Set them in algomentor.properties "
                        + "to send this for real.");
                System.out.println("[EmailService] To: " + toEmail + " | Subject: " + subject);
                System.out.println(body);
                return "(Not configured) Email content was printed to the console instead of sent. "
                        + "Set MAIL_USERNAME and MAIL_PASSWORD in algomentor.properties to send real emails.";
            }
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", smtpHost);
                props.put("mail.smtp.port", smtpPort);

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(fromAddress, password);
                    }
                });

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(fromAddress));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject(subject);
                message.setText(body);

                Transport.send(message);
                return "Email sent to " + toEmail;
            } catch (AuthenticationFailedException e) {
                return "GMAIL REJECTED THIS LOGIN. This means MAIL_PASSWORD is not a real App Password - "
                        + "Gmail refuses any made-up or normal account password here, with no exception. "
                        + "Fix: go to myaccount.google.com/apppasswords (2-Step Verification must be ON "
                        + "first), generate one, and paste exactly that 16-character value as MAIL_PASSWORD "
                        + "in algomentor.properties, then restart the app.";
            } catch (Exception e) {
                return "Failed to send email to " + toEmail + ": " + e.getMessage()
                        + " (check MAIL_USERNAME/MAIL_PASSWORD in algomentor.properties)";
            }
        }, AppExecutors.get());
    }
}
