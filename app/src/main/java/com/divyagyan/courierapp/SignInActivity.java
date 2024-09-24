package com.divyagyan.courierapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.regex.Pattern;


public class SignInActivity extends AppCompatActivity {
    private EditText emailSignInEditText,passwordSignInEditText;
    private Button userSignInButton;
    private TextView forgotPasswordTextView,signUpTextView;
    private ProgressBar loginProgressBar;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();

        emailSignInEditText = findViewById(R.id.emailSignInEditText);
        passwordSignInEditText = findViewById(R.id.passwordSignInEditText);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        signUpTextView = findViewById(R.id.signUpTextView);
        userSignInButton = findViewById(R.id.userSignInButton);
        loginProgressBar = findViewById(R.id.loginProgressBar);


        userSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailSignInEditText.getText().toString().trim();
                String password = passwordSignInEditText.getText().toString().trim();

                if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    emailSignInEditText.setError("Please enter a valid Email");
                    emailSignInEditText.requestFocus();
                }
                if(passwordSignInEditText.length() < 6){
                    passwordSignInEditText.setError("Please enter a valid password");
                    passwordSignInEditText.requestFocus();
                }
                loginProgressBar.setVisibility(View.VISIBLE);

                mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            loginProgressBar.setVisibility(View.GONE);
                            Toast.makeText(SignInActivity.this, "User Successfully Signed In", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignInActivity.this, DashboardActivity.class));
                        }else{
                            loginProgressBar.setVisibility(View.GONE);
                            Toast.makeText(SignInActivity.this, "Failed to Signed In", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });


        forgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignInActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

        signUpTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });

    }
}