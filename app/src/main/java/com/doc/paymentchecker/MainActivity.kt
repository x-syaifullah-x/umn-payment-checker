package com.doc.paymentchecker

import android.graphics.Paint
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.applovin.mediation.ads.MaxAdView
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkSettings
import com.doc.paymentchecker.data.Repository
import com.doc.paymentchecker.data.model.Result
import com.doc.paymentchecker.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

class MainActivity : FragmentActivity() {

    companion object {

        private const val PREF_NAME = "my_prefs"
        private const val PREF_KEY_USER_NAME = "user_name"
        private const val PREF_KEY_PASSWORD = "password"
    }

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        loadAd(binding.adView)

        showMessageBoard(binding.messageBoard)

        val sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val userName = sharedPreferences.getString(PREF_KEY_USER_NAME, "")
        val password = sharedPreferences.getString(PREF_KEY_PASSWORD, "")
        if (!userName.isNullOrBlank() && !password.isNullOrBlank()) {
            checkExpiry(userName, password)
        } else {
            binding.loginLayout.visibility = View.VISIBLE
        }
        binding.loginButton.setOnClickListener { _ ->
            val inputUsername = "${binding.usernameTextbox.text}"
            val inputPassword = "${binding.passwordTextbox.text}"
            if (inputUsername.isNotBlank() && inputPassword.isNotBlank()) {
                checkExpiry(inputUsername, inputPassword)
            }
        }
    }

    private fun checkExpiry(userName: String, password: String) {
        lifecycleScope.launch {
            Repository.getInstance().playerApi(userName = userName, password = password)
                .collect {
                    when (it) {
                        is Result.Loading -> {}
                        is Result.Success -> {
                            val userInfo = it.value.userInfo
                            val isExp =
                                userInfo.expDateTimeMillis <= System.currentTimeMillis()
                            val text: String
                            if (isExp) {
                                text = "YOUR ACCOUNT IS EXPIRED"
                            } else {
                                val date = Date(userInfo.expDateTimeMillis)
                                val sdf = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())
                                sdf.timeZone = TimeZone.getTimeZone("UTC")
                                val expDateFormatted = sdf.format(date)
                                text = "EXPIRATION: $expDateFormatted"
                            }
                            binding.expiryText.text = text
                            binding.loginLayout.visibility = View.GONE
                            binding.qrCodeLayout.visibility = View.VISIBLE
                            val editor =
                                getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit()
                            editor.putString(PREF_KEY_USER_NAME, userName)
                            editor.putString(PREF_KEY_PASSWORD, password)
                            editor.apply()
                        }

                        is Result.Error -> {
                            Toast.makeText(
                                this@MainActivity,
                                it.value.localizedMessage,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
        }
    }

    private fun showMessageBoard(view: TextView) {
        val sb = StringBuilder()
        val r = resources
        val displayMetrics = r.displayMetrics
        val applyDimension =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400f, displayMetrics)
        val screenWidth = applyDimension.roundToInt().toFloat()
        val text = "Welcome to payment checker"
        sb.replace(0, sb.toString().length, "")
        sb.append(text)
        val paint = Paint()
        paint.textSize = view.textSize
        val textWidth = paint.measureText(sb.toString())
        if (textWidth <= screenWidth) {
            val padding = (screenWidth - textWidth).toInt() / 2
            view.setPadding(padding, 0, padding, 0)
            sb.insert(0, "     ").append("     ")
        }
        view.text = sb.toString()
        view.isSelected = true
    }

    private fun loadAd(adView: MaxAdView) {
        val context = adView.context
        val settings = AppLovinSdkSettings(context)
        val adUnitIds: List<String> = ArrayList()
        settings.initializationAdUnitIds = adUnitIds
        val appLovInSdk = AppLovinSdk.getInstance(settings, context)
        appLovInSdk.mediationProvider = "max"
        adView.bringToFront()
        adView.loadAd()
    }
}