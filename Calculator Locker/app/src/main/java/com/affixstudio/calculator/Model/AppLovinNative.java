package com.affixstudio.calculator.Model;




import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;


import com.affixstudio.calculator.locker.R;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.nativeAds.MaxNativeAdListener;
import com.applovin.mediation.nativeAds.MaxNativeAdLoader;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder;

public class AppLovinNative {

    int layout;
    Activity a;
    private MaxAd loadedNativeAdSmall;
    private MaxAd loadedNativeAdMid;
    private MaxAd loadedNativeAdBig;

    public AppLovinNative(int layout, Activity a) {
        this.layout = layout;
        this.a = a;
    }



    public void mid(FrameLayout frameLayout){


        MaxNativeAdViewBinder binder = new MaxNativeAdViewBinder.Builder( layout)
                .setTitleTextViewId( R.id.title_text_view )
                .setBodyTextViewId( R.id.body_text_view )
                //  .setStarRatingContentViewGroupId( R.id.star_rating_view )
                .setAdvertiserTextViewId( R.id.advertiser_textView )
                .setIconImageViewId( R.id.icon_image_view )
                .setMediaContentViewGroupId( R.id.media_view_container )
                // .setOptionsContentViewGroupId( R.id.options_view )
                .setCallToActionButtonId( R.id.cta_button )
                .build();

        MaxNativeAdLoader nativeAdLoader = new MaxNativeAdLoader( a.getString(R.string.applovinNativeADID), a ); // change it with the original id
        nativeAdLoader.setNativeAdListener(new MaxNativeAdListener()
        {
            @Override
            public void onNativeAdLoaded(@Nullable MaxNativeAdView maxNativeAdView, MaxAd maxAd) {
                Log.i("home","native loaded ");



                if ( loadedNativeAdMid != null )
                {
                    nativeAdLoader.destroy( loadedNativeAdMid );
                }

                // Save ad for cleanup.
                loadedNativeAdMid = maxAd;

                frameLayout.removeAllViews();
                frameLayout.addView( maxNativeAdView );


            }

            @Override
            public void onNativeAdLoadFailed(String s, MaxError maxError) {
                super.onNativeAdLoadFailed(s, maxError);

                Log.i("home","Native load failed "+maxError.getMessage());
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        nativeAdLoader.loadAd( new MaxNativeAdView( binder, a ) );
                    }
                },5000);



            }

            @Override
            public void onNativeAdClicked(MaxAd maxAd) {
                super.onNativeAdClicked(maxAd);
                nativeAdLoader.loadAd( new MaxNativeAdView( binder, a ) );
            }

            @Override
            public void onNativeAdExpired(MaxAd maxAd) {
                super.onNativeAdExpired(maxAd);
                nativeAdLoader.loadAd( new MaxNativeAdView( binder, a ) );
            }
        });

        nativeAdLoader.loadAd( new MaxNativeAdView( binder, a ) );
    }
    public void big(String adUnit,FrameLayout frameLayout)
    {




        MaxNativeAdViewBinder binder = new MaxNativeAdViewBinder.Builder(layout)
                .setTitleTextViewId( R.id.title_text_view )
                .setBodyTextViewId( R.id.body_text_view )
                //  .setStarRatingContentViewGroupId( R.id.star_rating_view )
                .setAdvertiserTextViewId( R.id.advertiser_textView )
                .setIconImageViewId( R.id.icon_image_view )
                .setMediaContentViewGroupId( R.id.media_view_container )
                // .setOptionsContentViewGroupId( R.id.options_view )
                .setCallToActionButtonId( R.id.cta_button )
                .build();

        MaxNativeAdLoader nativeAdLoader = new MaxNativeAdLoader( adUnit, a );
        nativeAdLoader.setNativeAdListener(new MaxNativeAdListener()
        {
            @Override
            public void onNativeAdLoaded(@Nullable MaxNativeAdView maxNativeAdView, MaxAd maxAd) {
                Log.i("home","native loaded ");


               
                if ( loadedNativeAdBig != null )
                {
                    nativeAdLoader.destroy( loadedNativeAdBig );
                }

                // Save ad for cleanup.
                loadedNativeAdBig = maxAd;

                frameLayout.removeAllViews();
                frameLayout.addView( maxNativeAdView );


            }

            @Override
            public void onNativeAdLoadFailed(String s, MaxError maxError) {
                super.onNativeAdLoadFailed(s, maxError);

                Log.i("home","Native load failed "+maxError.getMessage());
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        nativeAdLoader.loadAd( new MaxNativeAdView( binder, a ) );
                    }
                },5000);



            }

            @Override
            public void onNativeAdClicked(MaxAd maxAd) {
                super.onNativeAdClicked(maxAd);
                nativeAdLoader.loadAd( new MaxNativeAdView( binder, a ) );
            }

            @Override
            public void onNativeAdExpired(MaxAd maxAd) {
                super.onNativeAdExpired(maxAd);
                nativeAdLoader.loadAd( new MaxNativeAdView( binder, a ) );
            }
        });

        nativeAdLoader.loadAd( new MaxNativeAdView( binder, a ) );

    }




}
