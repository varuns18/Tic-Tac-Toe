package com.ramphal.tictactoe;


import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class AdsServices {
    static InterstitialAd mInterstitialAd;
    public static void loadInterstitialAd(Context context){
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(context,"ca-app-pub-1480466069834998/2992995506", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        mInterstitialAd = null;
                    }
                });
    }

    public static void showAd(Context context){
        if (mInterstitialAd != null) {
            mInterstitialAd.show((Activity) context);
        }
    }
}
