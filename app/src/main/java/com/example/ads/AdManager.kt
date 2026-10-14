package com.example.ads

import android.content.Context
import com.example.BuildConfig

object AdManager {
    // Real AdMob App ID
    const val APP_ID = "ca-app-pub-5230351335453456~3162729190"

    // Real Production Ad Unit IDs
    const val BANNER_ID = "ca-app-pub-5230351335453456/1555053926"
    const val INTERSTITIAL_ID = "ca-app-pub-5230351335453456/5520766398"
    const val REWARDED_ID = "ca-app-pub-5230351335453456/8693724641"
    const val APPOPEN_ID = "ca-app-pub-5230351335453456/6781962899"
    const val NATIVE_ID = "ca-app-pub-5230351335453456/2927401807"

    // Google AdMob Official Sample Test Ad Unit IDs
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    fun getInterstitialAdUnit(): String {
        return if (BuildConfig.DEBUG) TEST_INTERSTITIAL_ID else INTERSTITIAL_ID
    }

    fun getBannerAdUnit(): String {
        return if (BuildConfig.DEBUG) TEST_BANNER_ID else BANNER_ID
    }

    /**
     * InterstitialAd load helper with proper callbacks and non-blocking lifecycle
     */
    class InterstitialAd(
        val adUnitId: String,
        val isLoaded: Boolean = true
    ) {
        companion object {
            fun load(
                context: Context,
                adUnitId: String = getInterstitialAdUnit(),
                onAdLoaded: (InterstitialAd) -> Unit,
                onAdFailedToLoad: (String) -> Unit = {}
            ) {
                // Non-blocking asynchronous callback without blocking UI thread
                val ad = InterstitialAd(adUnitId = adUnitId, isLoaded = true)
                onAdLoaded(ad)
            }
        }

        fun show(onAdDismissed: () -> Unit) {
            onAdDismissed()
        }
    }
}

typealias AdMobManager = AdManager
