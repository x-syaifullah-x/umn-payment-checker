package com.doc.paymentchecker;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.Executors;

public class MainActivity extends FragmentActivity {
    private Button connect;
    private EditText usernameed;
    private EditText passworded;
    private String username;
    private String password;
    private TextView Expiry;
    private android.widget.FrameLayout login;
    private android.widget.FrameLayout qr;
    private SharedPreferences myPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadAd(findViewById(R.id.adView));

        showMessageBoard(findViewById(R.id.message_board));

        usernameed = findViewById(R.id.username_textbox);
        passworded = findViewById(R.id.password_textbox);
        connect = findViewById(R.id.login_button);
        Expiry = findViewById(R.id.expiry_text);
        login = findViewById(R.id.login_layout);
        qr = findViewById(R.id.qr_code_layout);
        myPrefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
        if (!(Objects.requireNonNull(myPrefs.getString("usernam", "")).isEmpty() && Objects.requireNonNull(myPrefs.getString("passwor", "")).isEmpty())) {

            chkexpiry(myPrefs.getString("usernam", ""), myPrefs.getString("passwor", ""));

        } else {
            login.setVisibility(View.VISIBLE);

        }
        connect.setOnClickListener(view -> {
            username = String.valueOf(usernameed.getText());
            password = String.valueOf(passworded.getText());
            if (!(username.isEmpty() || password.isEmpty())) {
                chkexpiry(username, password);
            }
        });
    }

    private void showMessageBoard(TextView view) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("message_board")
                .addSnapshotListener(this, (value, error) -> {
                    if (value == null) return;
                    for (DocumentChange documentChange : value.getDocumentChanges()) {
                        String id = documentChange.getDocument().getId();
                        if (id.equals("title")) {
                            StringBuilder sb = new StringBuilder();
                            Resources r = getResources();
                            float screenWidth = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400, r.getDisplayMetrics()));
                            String text = documentChange.getDocument().getString("text");
                            if (text != null && !text.isEmpty()) {
                                sb.replace(0, sb.toString().length(), "");
                                sb.append(text);
                                Paint paint = new Paint();
                                paint.setTextSize(view.getTextSize());
                                float textWidth = paint.measureText(sb.toString());
//                                float screenWidth = getResources().getDisplayMetrics().widthPixels;
                                if (textWidth <= screenWidth) {
                                    int padding = (int) (screenWidth - textWidth) / 2;
                                    view.setPadding(padding, 0, padding, 0);
                                    sb.insert(0, "     ").append("     ");
                                }
                                view.setText(sb.toString());
                                view.setSelected(true);
                            }
                        }
                    }
                });
    }

    private void loadAd(MaxAdView adView) {
        Context context = adView.getContext();
        AppLovinSdkSettings settings = new AppLovinSdkSettings(context);
        List<String> adUnitIds = new ArrayList<>();
        settings.setInitializationAdUnitIds(adUnitIds);
        AppLovinSdk appLovinSdk = AppLovinSdk.getInstance(settings, context);
        appLovinSdk.setMediationProvider("max");
        adView.bringToFront();
        adView.loadAd();
    }

    private void chkexpiry(String user, String pass) {
        RequestQueue MyRequestQueue = Volley.newRequestQueue(this);
        String url = "http://line.my-tv.cc/player_api.php?username=" + user.trim() + "&password=" + pass.trim();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.e("firees", response);
                        if (response.contains("exp_date")) {
                            ObjectMapper objectMapper = new ObjectMapper();
                            try {
                                Map<String, Object> jsonMap = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
                                });
                                String expDate = (String) ((Map<String, Object>) jsonMap.get("user_info")).get("exp_date");
                                long expDateLong = Long.parseLong(expDate);
                                Date date = new Date(expDateLong * 1000L);
                                //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                String expDateFormatted = sdf.format(date);
                                System.out.println("Expiration date: " + expDateFormatted);
                                Expiry.setText("EXPIRATION: " + expDateFormatted);
                                login.setVisibility(View.GONE);
                                qr.setVisibility(View.VISIBLE);
                                SharedPreferences.Editor e1 = myPrefs.edit();
                                e1.putString("usernam", user); // add or overwrite someValue
                                e1.putString("passwor", pass);
                                e1.apply();
                                //Toast.makeText(MainActivity.this,"Expiration date: " + expDateFormatted,Toast.LENGTH_LONG).show();

                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }

                        } else {

                            Toast.makeText(MainActivity.this, "Make sure the User & Password is correct", Toast.LENGTH_LONG).show();
                        }
                        // Handle response
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Error make sure values are correct", Toast.LENGTH_LONG).show();
            }
        });

        MyRequestQueue.add(stringRequest);
    }
}
