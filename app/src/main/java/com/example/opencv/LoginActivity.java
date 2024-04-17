package com.example.opencv;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import cn.leancloud.LCObject;
import cn.leancloud.LCUser;
import cn.leancloud.LeanCloud;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class LoginActivity extends AppCompatActivity {

    EditText userNameInput, passwordInput;
    Button loginButton, signupButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        LeanCloud.initialize(this, "nMKlsJWMeXPRTzUXRAtfRA24-gzGzoHsz", "jHVnjnAuhQ52D1BD6QLbKmaH", "https://nmklsjwm.lc-cn-n1-shared.com");

        userNameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.loginButton);
        signupButton = findViewById(R.id.signupButton);

        loginButton.setOnClickListener(v -> {

            String userName = userNameInput.getText().toString();
            String password = passwordInput.getText().toString();

            if (userName.equals("") || password.equals("")) {
                Toast.makeText(LoginActivity.this, "Username and password cannot be null", Toast.LENGTH_SHORT).show();
            } else{
                LCUser.logIn(userName, password).subscribe(new Observer<LCUser>() {
                    public void onSubscribe(Disposable disposable) {
                    }

                    public void onNext(LCUser user) {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                    }

                    public void onError(Throwable throwable) {
                        Toast.makeText(LoginActivity.this, "Username or password wrong", Toast.LENGTH_SHORT).show();
                    }

                    public void onComplete() {
                    }
                });
            }
        });

        signupButton.setOnClickListener(v -> {

            String userName = userNameInput.getText().toString();
            String password = passwordInput.getText().toString();

            if(userName.equals("") || password.equals(""))
            {
                Toast.makeText(LoginActivity.this, "Username and password cannot be null", Toast.LENGTH_SHORT).show();
            }else {
                LCUser user = new LCUser();
                user.setUsername(userName);
                user.setPassword(password);
                user.signUpInBackground().subscribe(new Observer<LCUser>() {
                    public void onSubscribe(Disposable disposable) {}
                    public void onNext(LCUser user) {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                    public void onError(Throwable throwable) {
                        Toast.makeText(LoginActivity.this, "Unable to register", Toast.LENGTH_SHORT).show();
                    }
                    public void onComplete() {}
                });
            }
        });
    }
}