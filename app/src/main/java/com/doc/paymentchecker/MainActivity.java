package com.doc.paymentchecker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ShareCompat;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class MainActivity extends FragmentActivity {

    private static final String SHARED_PREFERENCES_NAME = "myPrefs";

    private static final String SHARED_PREFERENCES_KEY_USERNAME = "usernam";

    private static final String SHARED_PREFERENCES_KEY_PASSWORD = "passwor";

    private EditText etUserName;

    private EditText etPassword;

    private TextView tvExpiry;

    private TextView tvUserName;

    private android.widget.FrameLayout vgLogin;

    private android.widget.FrameLayout vgQR;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadAd(findViewById(R.id.adView));

        tvUserName = findViewById(R.id.username);
        etUserName = findViewById(R.id.username_textbox);
        etPassword = findViewById(R.id.password_textbox);
        tvExpiry = findViewById(R.id.expiry_text);
        vgQR = findViewById(R.id.qr_code_layout);
        vgLogin = findViewById(R.id.login_layout);

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        String username = sharedPreferences.getString(SHARED_PREFERENCES_KEY_USERNAME, "");
        String password = sharedPreferences.getString(SHARED_PREFERENCES_KEY_PASSWORD, "");
        if (!username.isEmpty() && !password.isEmpty()) {
            chkexpiry(username, password);
        } else {
            vgLogin.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.login_button).setOnClickListener(view -> {
            String usernameInput = etUserName.getText().toString();
            String passwordInput = etPassword.getText().toString();
            if (usernameInput.isEmpty()) return;
            if (passwordInput.isEmpty()) return;
            chkexpiry(usernameInput, passwordInput);
        });

//        findViewById(R.id.btn_send).setOnClickListener(v -> {
//            EditText etSubject = findViewById(R.id.et_subject);
//            String inputSubject = etSubject.getText().toString();
////            if (inputSubject.isEmpty())
////                return;
//            EditText etMessage = findViewById(R.id.et_message);
//            String inputMessage = etMessage.getText().toString();
////            if (inputMessage.isEmpty())
////                return;
//
//            ShareCompat.IntentBuilder.from(MainActivity.this)
//                    .setType("message/rfc822")
//                    .addEmailTo("support@umntv.net")
//                    .setSubject(inputSubject)
//                    .setText("username: " + username + "\n\n\n" + inputMessage)
//                    .setChooserTitle("Choose an email client :")
//                    .startChooser();
//        });
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
        Uri uri = new Uri.Builder()
                .scheme("http")
                .authority("line.my-tv.cc")
                .path("player_api.php")
                .appendQueryParameter("username", user.trim())
                .appendQueryParameter("password", pass.trim())
                .build();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, uri.toString(), response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONObject userInfo = jsonObject.getJSONObject("user_info");
                String expDate = userInfo.getString("exp_date");
                long expDateLong = Long.parseLong(expDate);
                Date date = new Date(expDateLong * 1000L);
                //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String expDateFormatted = sdf.format(date);
                System.out.println("Expiration date: " + expDateFormatted);
                String userName = userInfo.getString("username");
                tvUserName.setText("USERNAME: " + userName);
                tvExpiry.setText("EXPIRATION: " + expDateFormatted);
                vgLogin.setVisibility(View.GONE);
                vgQR.setVisibility(View.VISIBLE);
                SharedPreferences.Editor edit = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                edit.putString(SHARED_PREFERENCES_KEY_USERNAME, user);
                edit.putString(SHARED_PREFERENCES_KEY_PASSWORD, pass);
                edit.apply();

                StringBuilder sb = new StringBuilder();
                Resources r = getResources();
                float screenWidth = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400, r.getDisplayMetrics()));
                String text = "Your UMN Q96 Mate has been updated. Thanks for your support";
                sb.replace(0, sb.toString().length(), "");
                sb.append(text);
                Paint paint = new Paint();
                TextView view = findViewById(R.id.message_board);
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
            } catch (JSONException e) {
                Toast.makeText(MainActivity.this, "Make sure the User & Password is correct", Toast.LENGTH_LONG).show();
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
