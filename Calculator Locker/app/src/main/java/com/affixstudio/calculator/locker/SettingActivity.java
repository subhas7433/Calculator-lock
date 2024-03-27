package com.affixstudio.calculator.locker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class SettingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        findViewById(R.id.changePassword).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                startActivity(new Intent(SettingActivity.this,MainActivity.class).putExtra("changingPass",true));
            }
        });

        // rate us
        findViewById(R.id.rateUs).setOnClickListener(view -> {

            openUrl(getString(R.string.playstoreUrl)+getPackageName());


        });
        // more apps
        findViewById(R.id.moreApps).setOnClickListener(view -> {
            openUrl(getString(R.string.playstoreAffixUrl));
        });

        // privacy policy
        findViewById(R.id.privacyPolicy).setOnClickListener(view -> {
            openUrl(getString(R.string.privacyPolicyUrl));

        });

        // share
        findViewById(R.id.share).setOnClickListener(view -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_name)+"\n\n"+getString(R.string.playstoreUrl)+getPackageName());
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, null);
            startActivity(shareIntent);
        });





    }
    public void onBackPressed(View v)
    {
        super.onBackPressed();
    }
    void openUrl(String url) // open url in browser
    {
        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)));
    }
}