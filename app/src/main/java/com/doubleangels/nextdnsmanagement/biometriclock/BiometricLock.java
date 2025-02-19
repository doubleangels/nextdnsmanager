package com.doubleangels.nextdnsmanagement.biometriclock;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

/**
 * A helper class that wraps Biometric + Device Credential authentication logic.
 * Usage:
 *   1. Create an instance in your Activity:
 *        BiometricLock biometricLock = new BiometricLock(this);
 *   2. Check if you can authenticate:
 *        if (biometricLock.canAuthenticate()) {
 *            // 3. Show the prompt:
 *            biometricLock.showPrompt(
 *               "Secure Login",
 *               "Use biometric or device lock",
 *               "Authenticate with fingerprint, face, or device PIN/pattern",
 *               new BiometricLockCallback() {
 *                   @Override
 *                   public void onAuthenticationSucceeded() {
 *                       // Access granted!
 *                   }
 *
 *                   @Override
 *                   public void onAuthenticationError(String error) {
 *                       // Error or user canceled
 *                   }
 *
 *                   @Override
 *                   public void onAuthenticationFailed() {
 *                       // Biometric not recognized
 *                   }
 *               }
 *            );
 *        } else {
 *            // Handle fallback or show a message
 *        }
 */
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
        BiometricManager biometricManager = BiometricManager.from(activity);

        int canAuth = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );
        // canAuth will be BIOMETRIC_SUCCESS if the user can authenticate with either
        // a biometric or the device credentials (PIN, pattern, password).

        return (canAuth == BiometricManager.BIOMETRIC_SUCCESS);
    }

    /**
     * Shows a prompt that allows either biometrics OR device credentials as a fallback.
     * No negative button is required because the system handles fallback to device PIN/pattern/password.
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

        // 1. Executor that will handle callback events on the main thread.
        Executor executor = ContextCompat.getMainExecutor(activity);

        // 2. Create the BiometricPrompt with a callback.
        // This covers both a fatal error AND user cancellation.
        // The user tried a biometric that didn’t match.
        BiometricPrompt biometricPrompt = new BiometricPrompt(activity,
                executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // This covers both a fatal error AND user cancellation.
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
                        // The user tried a biometric that didn’t match.
                        callback.onAuthenticationFailed();
                    }
                });

        // 3. Build the PromptInfo object.
        //    By using setAllowedAuthenticators(BIOMETRIC_STRONG | DEVICE_CREDENTIAL),
        //    the system will let users either scan a fingerprint/face or enter their device PIN/password.
        // Notice we do NOT call setNegativeButtonText(...)
        // because device credentials fallback is used instead of a "Cancel" button.
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                // Notice we do NOT call setNegativeButtonText(...)
                // because device credentials fallback is used instead of a "Cancel" button.
                .build();

        // 4. Show the prompt
        biometricPrompt.authenticate(promptInfo);
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
