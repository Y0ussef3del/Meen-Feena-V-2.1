package com.example.game.viewmodel

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.*

fun GameViewModel.isNetworkAvailable(context: Context): Boolean {
    return try {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    } catch (e: Exception) {
        false
    }
}

internal fun GameViewModel.getLocalHeartsCount(): Int {
    return sharedPreferences.getInt("hearts_count", GameViewModel.MAX_HEARTS)
}

fun GameViewModel.saveLocalHeartsCount(count: Int) {
    val safeCount = count.coerceIn(0, GameViewModel.MAX_HEARTS)
    sharedPreferences.edit().putInt("hearts_count", safeCount).apply()
    _roomState.update { it.copy(heartsCount = safeCount) }
}

internal fun GameViewModel.checkAndResetDailyHearts() {
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val lastPlayDate = sharedPreferences.getString("last_play_date", "")

    if (lastPlayDate.isNullOrEmpty() || todayStr != lastPlayDate) {
        sharedPreferences.edit()
            .putString("last_play_date", todayStr)
            .putInt("hearts_count", GameViewModel.MAX_HEARTS)
            .apply()
    }
    _roomState.update { it.copy(heartsCount = getLocalHeartsCount()) }
}

fun GameViewModel.hasHeartsToPlay(): Boolean {
    checkAndResetDailyHearts()
    return getLocalHeartsCount() > 0
}

fun GameViewModel.consumeHeart(): Boolean {
    val currentHearts = getLocalHeartsCount()
    return if (currentHearts > 0) {
        saveLocalHeartsCount(currentHearts - 1)
        true
    } else {
        false
    }
}

fun GameViewModel.loadRewardedAdInternal() {
    if (rewardedAd != null || isAdLoading || !isNetworkAvailable(getApplication())) {
        return
    }
    isAdLoading = true
    val adRequest = AdRequest.Builder().build()
    RewardedAd.load(getApplication(), "ca-app-pub-6722529223110069/2125092694", adRequest,
        object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                isAdLoading = false
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedAd = null
                isAdLoading = false
            }
        })
}

fun GameViewModel.showAdToEarnHeart(activity: Activity, onFinished: (Boolean) -> Unit) {
    if (activity.isFinishing || activity.isDestroyed || !isNetworkAvailable(activity)) {
        onFinished(false)
        return
    }

    rewardedAd?.let { ad ->
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewardedAdInternal()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                loadRewardedAdInternal()
                onFinished(false)
            }
        }
        ad.show(activity) { _ ->
            val currentHearts = getLocalHeartsCount()
            saveLocalHeartsCount(currentHearts + 1)
            onFinished(true)
        }
    } ?: run {
        val currentHearts = getLocalHeartsCount()
        saveLocalHeartsCount(currentHearts + 1)
        loadRewardedAdInternal()
        onFinished(true)
    }
}

fun GameViewModel.loadInterstitialAdInternal() {
    if (interstitialAd != null || isInterstitialLoading || !isNetworkAvailable(getApplication())) {
        return
    }
    isInterstitialLoading = true
    val adRequest = AdRequest.Builder().build()
    InterstitialAd.load(
        getApplication(),
        "ca-app-pub-6722529223110069/3139787314",
        adRequest,
        object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                isInterstitialLoading = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialAd = null
                isInterstitialLoading = false
            }
        }
    )
}

fun GameViewModel.showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit) {
    if (activity.isFinishing || activity.isDestroyed || !isNetworkAvailable(activity)) {
        onAdDismissed()
        return
    }

    interstitialAd?.let { ad ->
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitialAdInternal()
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                loadInterstitialAdInternal()
                onAdDismissed()
            }
        }
        ad.show(activity)
    } ?: run {
        loadInterstitialAdInternal()
        onAdDismissed()
    }
}

fun GameViewModel.checkHeartsAndProceed(activity: Activity, onNoInternet: () -> Unit, onAccessGranted: () -> Unit) {
    if (hasHeartsToPlay()) {
        onAccessGranted()
    } else {
        showAdToEarnHeart(activity) { success ->
            if (success) {
                onAccessGranted()
            } else {
                onNoInternet()
            }
        }
    }
}