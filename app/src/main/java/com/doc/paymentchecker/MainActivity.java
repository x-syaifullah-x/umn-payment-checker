package com.doc.paymentchecker;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.applovin.mediation.ads.MaxAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.doc.paymentchecker.databinding.ActivityMainBinding;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends FragmentActivity {

    private static final String PREF_NAME = "my_prefs";
    private static final String PREF_KEY_USER_NAME = "user_name";
    private static final String PREF_KEY_PASSWORD = "password";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private ActivityMainBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        loadAd(binding.adView);

        showMessageBoard(binding.messageBoard);

        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String userName = sharedPreferences.getString(PREF_KEY_USER_NAME, "");
        String password = sharedPreferences.getString(PREF_KEY_PASSWORD, "");
        if (!userName.isEmpty() && !password.isEmpty()) {
            checkExpiry(userName, password);
        } else {
            binding.loginLayout.setVisibility(View.VISIBLE);
        }
        binding.loginButton.setOnClickListener(v -> {
            String _username = String.valueOf(binding.usernameTextbox.getText());
            String _password = String.valueOf(binding.passwordTextbox.getText());
            if (!(_username.isEmpty() || _password.isEmpty())) {
                checkExpiry(_username, _password);
            }
        });
    }

    private void showMessageBoard(TextView view) {
        StringBuilder sb = new StringBuilder();
        Resources r = getResources();
        DisplayMetrics displayMetrics = r.getDisplayMetrics();
        float applyDimension = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400, displayMetrics);
        float screenWidth = Math.round(applyDimension);
        String text = "Welcome to payment checker";
        sb.replace(0, sb.toString().length(), "");
        sb.append(text);
        Paint paint = new Paint();
        paint.setTextSize(view.getTextSize());
        float textWidth = paint.measureText(sb.toString());
        if (textWidth <= screenWidth) {
            int padding = (int) (screenWidth - textWidth) / 2;
            view.setPadding(padding, 0, padding, 0);
            sb.insert(0, "     ").append("     ");
        }
        view.setText(sb.toString());
        view.setSelected(true);
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

    private void checkExpiry(String user, String pass) {
        Uri uri = new Uri.Builder()
                .scheme("http")
                .authority("line.my-tv.cc")
                .path("player_api.php")
                .appendQueryParameter("username", user.trim())
                .appendQueryParameter("password", pass.trim())
                .build();
        new Thread(() -> {
            HttpURLConnection httpURLConnection = null;
            try {
                URL URL = new URL(uri.toString());
                httpURLConnection = (HttpURLConnection) URL.openConnection();
                int responseCode = httpURLConnection.getResponseCode();

                int buffersSize = 1024 * 1024;
                byte[] buffers = new byte[buffersSize];
                StringBuilder result = new StringBuilder();
                if (responseCode >= 200 && responseCode < 300) {
                    InputStream is = httpURLConnection.getInputStream();
                    while (true) {
                        int readCount = is.read(buffers, 0, buffersSize);
                        if (readCount != -1) {
                            if (readCount == buffersSize) {
                                result.append(new String(buffers));
                            } else {
                                result.append(new String(Arrays.copyOf(buffers, readCount)));
                            }
                        } else {
                            is.close();
                            break;
                        }
                    }
                    JSONObject response = new JSONObject(result.toString());
                    JSONObject userInfo = response.getJSONObject("user_info");
//                    JSONObject serverInfo = response.getJSONObject("server_info");
                    String expDate = userInfo.getString("exp_date");
                    handler.post(() -> {
                        long expDateLong = Long.parseLong(expDate);
                        long a = expDateLong * 1000L;
                        boolean isExp = a <= System.currentTimeMillis();
                        if (isExp) {
                            String text = "YOUR ACCOUNT IS EXPIRED";
                            binding.expiryText.setText(text);
                        } else {
                            Date date = new Date(a);
                            //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault());
                            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                            String expDateFormatted = sdf.format(date);
                            String text = "EXPIRATION: " + expDateFormatted;
                            binding.expiryText.setText(text);
                        }
                        binding.loginLayout.setVisibility(View.GONE);
                        binding.qrCodeLayout.setVisibility(View.VISIBLE);
                        SharedPreferences.Editor editor =
                                getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
                        editor.putString(PREF_KEY_USER_NAME, user);
                        editor.putString(PREF_KEY_PASSWORD, pass);
                        editor.apply();
                    });
                } else {
                    InputStream is = httpURLConnection.getErrorStream();
                    while (true) {
                        int readCount = is.read(buffers, 0, buffersSize);
                        if (readCount != -1) {
                            if (readCount == buffersSize) {
                                result.append(new String(buffers));
                            } else {
                                result.append(new String(Arrays.copyOf(buffers, readCount)));
                            }
                        } else {
                            is.close();
                            break;
                        }
                    }
                    String resultAsString = result.toString();
                    handler.post(() -> {
                        if (resultAsString.isBlank()) {
                            Toast.makeText(this, "Make sure the User & Password is correct", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, resultAsString, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (Throwable e) {
                e.printStackTrace();
                String message = e.getLocalizedMessage();
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            } finally {
                if (httpURLConnection != null)
                    httpURLConnection.disconnect();
            }
        }).start();
    }
}
