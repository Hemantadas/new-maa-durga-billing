package com.newmadurga.billing;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import java.util.concurrent.TimeUnit;

public class OtpLoginActivity extends Activity {
    private FirebaseAuth auth;
    private EditText phoneInput, codeInput;
    private Button sendButton, verifyButton;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private boolean codeSent = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        auth = FirebaseAuth.getInstance();
        showLogin();
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private TextView label(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextColor(Color.WHITE); t.setTextSize(size);
        t.setGravity(Gravity.CENTER); t.setPadding(dp(8), dp(8), dp(8), dp(8));
        return t;
    }

    private void showLogin() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(18,18,18));

        TextView title = label("নিউ মা দূর্গা বিলিং", 25);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));
        TextView subtitle = label("মোবাইল OTP দিয়ে লগইন করুন", 16);
        root.addView(subtitle);

        phoneInput = new EditText(this);
        phoneInput.setSingleLine(true);
        phoneInput.setText("+919932886333");
        phoneInput.setHint("+91 মোবাইল নম্বর");
        phoneInput.setTextColor(Color.WHITE);
        phoneInput.setHintTextColor(Color.LTGRAY);
        phoneInput.setTextSize(18);
        phoneInput.setInputType(3);
        phoneInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        phoneInput.setBackgroundColor(Color.rgb(48,48,48));
        root.addView(phoneInput, fieldParams());

        sendButton = new Button(this);
        sendButton.setText("OTP পাঠান");
        root.addView(sendButton, fieldParams());

        codeInput = new EditText(this);
        codeInput.setSingleLine(true);
        codeInput.setHint("৬ সংখ্যার OTP লিখুন");
        codeInput.setTextColor(Color.WHITE);
        codeInput.setHintTextColor(Color.LTGRAY);
        codeInput.setTextSize(18);
        codeInput.setInputType(2);
        codeInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        codeInput.setBackgroundColor(Color.rgb(48,48,48));
        codeInput.setVisibility(View.GONE);
        root.addView(codeInput, fieldParams());

        verifyButton = new Button(this);
        verifyButton.setText("যাচাই করে লগইন");
        verifyButton.setVisibility(View.GONE);
        root.addView(verifyButton, fieldParams());

        sendButton.setOnClickListener(v -> sendOtp());
        verifyButton.setOnClickListener(v -> verifyOtp());

        setContentView(root);
    }

    private LinearLayout.LayoutParams fieldParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(12);
        return p;
    }

    private void sendOtp() {
        String phone = phoneInput.getText().toString().trim().replace(" ", "");
        if (!phone.matches("^\\+[1-9][0-9]{7,14}$")) {
            phoneInput.setError("দেশের কোডসহ নম্বর লিখুন, যেমন +919932886333");
            return;
        }
        sendButton.setEnabled(false);
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override public void onVerificationCompleted(PhoneAuthCredential credential) {
                    signIn(credential);
                }
                @Override public void onVerificationFailed(FirebaseException e) {
                    sendButton.setEnabled(true);
                    String detail = "OTP পাঠানো যায়নি | Type: " + e.getClass().getSimpleName() + " | Firebase code: " + (e instanceof FirebaseAuthException ? ((FirebaseAuthException)e).getErrorCode() : "unknown") + " | Message: " + e.getMessage();
                    Toast.makeText(OtpLoginActivity.this, detail, Toast.LENGTH_LONG).show();
                }
                @Override public void onCodeSent(String id, PhoneAuthProvider.ForceResendingToken token) {
                    verificationId = id; resendToken = token; codeSent = true;
                    codeInput.setVisibility(View.VISIBLE);
                    verifyButton.setVisibility(View.VISIBLE);
                    sendButton.setEnabled(true);
                    sendButton.setText("আবার OTP পাঠান");
                    Toast.makeText(OtpLoginActivity.this, "OTP পাঠানো হয়েছে", Toast.LENGTH_LONG).show();
                }
            }).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyOtp() {
        String code = codeInput.getText().toString().trim();
        if (verificationId == null || code.length() != 6) {
            codeInput.setError("৬ সংখ্যার OTP লিখুন");
            return;
        }
        signIn(PhoneAuthProvider.getCredential(verificationId, code));
    }

    private void signIn(PhoneAuthCredential credential) {
        verifyButton.setEnabled(false);
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "লগইন সফল", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                verifyButton.setEnabled(true);
                String message = task.getException() == null ? "OTP যাচাই হয়নি" : task.getException().getLocalizedMessage();
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
