package com.doubleangels.nextdnsmanagement.biometriclock;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.util.concurrent.Executor;

public class BiometricLock {

    private final AppCompatActivity activity;

    public BiometricLock(AppCompatActivity activity) {
        this.activity = activity;
    }

    /**
     * Checks if the device supports biometrics (or device credentials)
     * and has at least one biometric enrolled or a lock screen set up.
     */
    public boolean canAuthenticate() {
        SentryManager sentryManager = new SentryManager(activity);
        try {
            BiometricManager biometricManager = BiometricManager.from(activity);
            if (biometricManager == null) {
                sentryManager.captureMessage("BiometricManager is null - biometric authentication not available");
                return false;
            }
            int canAuth = biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                            | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            boolean result = (canAuth == BiometricManager.BIOMETRIC_SUCCESS);
            if (!result) {
                sentryManager.captureMessage("Biometric authentication not available. Status: " + canAuth);
            }
            return result;
        } catch (Exception e) {
            sentryManager.captureException(e);
            // Don't allow exceptions to bypass authentication - return false for security
            return false;
        }
    }

    /**
     * Shows a prompt that allows either biometrics OR device credentials as a
     * fallback.
     * No negative button is required because the system handles fallback to device
     * PIN/pattern/password.
     *
     * @param title       Main title text
     * @param subtitle    Smaller text under the title
     * @param description Additional description text
     * @param callback    Callback to handle results
     */
    public void showPrompt(String title,
            String subtitle,
            String description,
            BiometricLockCallback callback) {

        SentryManager sentryManager = new SentryManager(activity);

        // 1. Executor that will handle callback events on the main thread.
        Executor executor = ContextCompat.getMainExecutor(activity);

        // 2. Create the BiometricPrompt with a callback.
        BiometricPrompt biometricPrompt = new BiometricPrompt(activity,
                executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode,
                            @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        String errorMessage = "Biometric authentication error (" + errorCode + "): " + errString;
                        sentryManager.captureMessage(errorMessage);
                        callback.onAuthenticationError(errString.toString());
                    }

                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        callback.onAuthenticationSucceeded();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        String failMessage = "Biometric authentication failed: unrecognized biometric input.";
                        sentryManager.captureMessage(failMessage);
                        callback.onAuthenticationFailed();
                    }
                });

        // 3. Build the PromptInfo object.
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        // 4. Show the prompt with error handling.
        try {
            biometricPrompt.authenticate(promptInfo);
        } catch (Exception e) {
            sentryManager.captureException(e);
            callback.onAuthenticationError(e.getMessage());
        }
    }

    /**
     * Callback interface for the result of authentication.
     */
    public interface BiometricLockCallback {
        /** Called when the user is successfully authenticated. */
        void onAuthenticationSucceeded();

        /** Called when an unrecoverable error or user-cancellation occurs. */
        void onAuthenticationError(String error);

        /** Called when a biometric was presented but not recognized. */
        void onAuthenticationFailed();
    }
}
