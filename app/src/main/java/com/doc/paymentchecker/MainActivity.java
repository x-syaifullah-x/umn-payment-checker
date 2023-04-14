package com.doc.paymentchecker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

/*
 * Main Activity class that loads {@link MainFragment}.
 */
public class MainActivity extends FragmentActivity {
    Button connect;
    EditText usernameed;
    EditText passworded;
    String username;
    String password;
    TextView Expiry;
    android.widget.FrameLayout login;
    android.widget.FrameLayout qr;
    SharedPreferences myPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadAd(findViewById(R.id.adView));

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
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                username = String.valueOf(usernameed.getText());
                password = String.valueOf(passworded.getText());
                if (!(username.isEmpty() || password.isEmpty())) {

                    chkexpiry(username, password);


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
