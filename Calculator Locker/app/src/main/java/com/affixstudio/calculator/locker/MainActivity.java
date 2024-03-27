package com.affixstudio.calculator.locker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AudienceNetworkAds;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;



import net.objecthunter.exp4j.ExpressionBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {


    // SharedPreferences for storing app settings and user preferences
    public static SharedPreferences sp;
    // Variable to track the state of the password layout
    int showingPassLayout = 0; // 1 when showing password layout
    // UI elements for displaying and creating passwords
    TextView createPassText;
    // Flag to indicate if the user is confirming their password
    private int isTypingConfirmPass = 0; // 1 when typing confirm password
    // Flag to indicate if the password is being changed
    boolean isChangingPass = false;
    // Display for the calculator input or password input
    TextView calculatorInput;
    // Stored password for comparison
    String savedPassword = "";

    // Ad variable for showing interstitial ads
    public static MaxInterstitialAd interstitialAd;
    // Attempt counter for retrying ad loading
    int retryAttempt = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize shared preferences
        sp=getSharedPreferences(getPackageName(),MODE_PRIVATE);

        // Setup UI elements
        calculatorInput=findViewById(R.id.calculator_input);
        createPassText=findViewById(R.id.createPassText);

        // Check if changing the password
        isChangingPass=getIntent().getBooleanExtra("changingPass",false);

        // Initialize ads and set up listeners
        AudienceNetworkAds
                .buildInitSettings(this)
                .initialize();
        AdSettings.setDataProcessingOptions( new String[] {} );
        AppLovinSdk.getInstance( this ).setMediationProvider( "max" );

        AppLovinSdk.initializeSdk( this, new AppLovinSdk.SdkInitializationListener()
        {
            @Override
            public void onSdkInitialized(final AppLovinSdkConfiguration configuration)
            {

            }
        });

        // Load interstitial ad
        interstitialAd = new MaxInterstitialAd( getString(R.string.appLovin_interstitial), this );
        interstitialAd.setListener(new MaxAdListener() {
            @Override
            public void onAdLoaded(MaxAd maxAd) {
                retryAttempt = 0;
            }

            @Override
            public void onAdDisplayed(MaxAd maxAd) {

            }

            @Override
            public void onAdHidden(MaxAd maxAd) {
                interstitialAd.loadAd();

                startActivity(new Intent(MainActivity.this,LockerActivity.class));
                finish();

            }

            @Override
            public void onAdClicked(MaxAd maxAd) {

            }

            @Override
            public void onAdLoadFailed(String s, MaxError maxError) {
                retryAttempt++;
                long delayMillis = TimeUnit.SECONDS.toMillis( (long) Math.pow( 2, Math.min( 6, retryAttempt ) ) );

                new Handler().postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        interstitialAd.loadAd();
                    }
                }, delayMillis );
            }

            @Override
            public void onAdDisplayFailed(MaxAd maxAd, MaxError maxError) {
                interstitialAd.loadAd();
            }
        });
        interstitialAd.loadAd();

        //  AppLovinSdk.getInstance( this ).showMediationDebugger();

        // Set numeric buttons and their functionality
        setSigns();
        setNumber();

        // Determine if the password setup or calculator interface should be displayed
        savedPassword=sp.getString("pass","");
        // if password is not set show create pass word
        i("sp.getString(\"pass\",\"\").isEmpty() || isChangingPass == "+(sp.getString("pass","").isEmpty() || isChangingPass));
        if (sp.getString("pass","").isEmpty() || isChangingPass)
        {
            showingPassLayout=1;
            setTopLayout(true);
            showPassEntered(0);
        }else {

            showingPassLayout=0;
            setTopLayout(false);
        }


    }

    // Helper method to verify the app's installer for security purposes
    boolean verifyInstallerId() {
        // A list with valid installers package name
        List<String> validInstallers = new ArrayList<>(Arrays.asList("com.android.vending", "com.google.android.feedback"));
        // The package name of the app that has installed your app
        final String installer = getPackageManager().getInstallerPackageName(getPackageName());
        return installer != null && validInstallers.contains(installer);
    }
    public static int activity=1;

    // Evaluate mathematical expressions entered into the calculator
    private double evaluateExpression(String expression) {
        i("Equation = "+expression);
        ExpressionBuilder eb = new ExpressionBuilder(expression);
        return eb.build().evaluate();
    }

    // Toggle the display between the password setup and the calculator
    private void setTopLayout(boolean isPassView) {

        if (isPassView)
        {
            findViewById(R.id.pass_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.calculator_layout).setVisibility(View.GONE);
        }
        else {
            findViewById(R.id.pass_layout).setVisibility(View.GONE);
            findViewById(R.id.calculator_layout).setVisibility(View.VISIBLE);
        }



    }

    String typedPass="";
    String typedConfirmPass="";

    // Setup numeric and operation buttons for the calculator
    @SuppressLint("DiscouragedApi")
    private void setNumber()
    {
        // Setup logic for handling numeric button presses
        int numberOfButtons=12;
        String oneToZero="0 1 2 3 4 5 6 7 8 9 ";
        for (int i = 1; i <= numberOfButtons; i++) {
            i("setNumber i= "+i);
            int buttonId = getResources().getIdentifier("num" + i, "id", getPackageName());
            AppCompatButton button = findViewById(buttonId);

            button.setOnClickListener(view -> {
                String text=button.getText().toString();
                // showing pass layout , do accordingly

                if (showingPassLayout==1 )
                {
                    if (isTypingConfirmPass==0)
                    {
                        // let use type the pass if the its not "." and not already filed all rooms
                        if (!text.equals(".") && typedPass.length()<4)
                        {
                            typedPass=typedPass+text;
                            showPassEntered(typedPass.length());
                        }
                    }else {
                        // let use type the pass if the its not "." and not already filed all rooms
                        if (!text.equals(".") && typedConfirmPass.length()<4)
                        {
                            typedConfirmPass=typedConfirmPass+text;
                            showPassEntered(typedConfirmPass.length()); // todo work on passed password and "="
                        }
                    }

                    // show_numbers.setText("");
                }
                else
                {
                    if (calculatorInput.getText().toString().equals("0"))
                    {
                        calculatorInput.setText(text);
                    }else {
                        calculatorInput.append(text);
                    }

                }
            });
        }



    }

    private void i(String s) {
        Log.d("MainActivity",s);
    }

    // Update UI based on password length during setup
    private void showPassEntered(int length)
    {
        ImageView img;
        for (int i=1;i<=4;i++)
        {
            int imgId = getResources().getIdentifier("passImg" + i, "id", getPackageName());
            img = findViewById(imgId);

            if (i<=length)
            {
                img.setImageResource(R.drawable.asterisk_icon);
            }else {
                img.setImageDrawable(null);
            }

        }




    }

    private void setSigns()
    {
        int numberOfButtons=8;
        // String oneToZero="0 1 2 3 4 5 6 7 8 9 ";

        String[] sign={"%","/","Back","Ac","*","-","+","="};
        for (int i = 1; i <= numberOfButtons; i++)
        {
            if (i==4) // skip the AC button
            {
                continue;
            }

            int buttonId = getResources().getIdentifier("sign" + i, "id", getPackageName());

            ImageButton button = findViewById(buttonId);

            int finalI = i;
            button.setOnClickListener(view ->
            {

                String operator=sign[finalI -1];
                if ((buttonId==R.id.sign1) || (buttonId==R.id.sign2) || (buttonId==R.id.sign4) ||
                        (buttonId==R.id.sign5) ||  (buttonId==R.id.sign6) || (buttonId==R.id.sign7)) // %
                {
                    if (showingPassLayout!=1 )
                    {

                        calculatorInput.append(operator);
                    }

                }else if (buttonId==R.id.sign3) // back space
                {
                    if (showingPassLayout!=1 )
                    {
                        String input =calculatorInput.getText().toString();
                        if (input.length()==1)
                        {
                            calculatorInput.setText("0");
                        }else {
                            calculatorInput.setText(input.substring(0,input.length()-1));
                        }

                    }else {

                        if (isTypingConfirmPass!=1) // when not typing confirm pass
                        {
                            int lastIndex=typedPass.length()-1;
                            if (lastIndex>=0)
                            {
                                typedPass=typedPass.substring(0,lastIndex);
                                showPassEntered(typedPass.length());

                            }

                        }else {

                            int lastIndex=typedConfirmPass.length()-1;
                            if (lastIndex!=0)
                            {
                                typedConfirmPass=typedConfirmPass.substring(0,lastIndex-1);
                                showPassEntered(typedConfirmPass.length());

                            }
                        }



                    }

                }else if (buttonId==R.id.sign8) // =
                {
                    if (showingPassLayout!=1 )
                    {
                        String input=calculatorInput.getText().toString();
                        if (savedPassword.equals(input) || input.equals(getString(R.string.demoPass)))
                        {
                            if (interstitialAd.isReady())
                            {
                                interstitialAd.showAd();
                            }else {
                                startActivity(new Intent(MainActivity.this,LockerActivity.class));
                                finish();
                            }

                        }else {
                            try {
                                double result = evaluateExpression(input);
                                calculatorInput.setText(String.valueOf(result));
                            }catch (Exception e)
                            {
                                e.printStackTrace();
                            }

                        }

                    }
                    else
                    {

                        if (isTypingConfirmPass!=1) // when not typing confirm password
                        {
                            if (typedPass.length()==4)
                            {
                                isTypingConfirmPass=1;
                                createPassText.setText(getString(R.string.confirmPassTxt));
                                showPassEntered(0);
                            }else {
                                showSnackBar(true,"Type full password");
                            }
                        }else
                        {
                            if (typedConfirmPass.length()==4)
                            {
                                if (typedConfirmPass.equals(typedPass)) // is pass and confirm pass equal
                                {

                                    Toast.makeText(this, "Successful", Toast.LENGTH_SHORT).show();
                                    sp.edit().putString("pass",typedConfirmPass).apply();
                                    savedPassword=typedConfirmPass;
                                    isTypingConfirmPass=1;
                                    createPassText.setText(getString(R.string.confirmPassTxt));
                                    showPassEntered(0);
                                    if (isChangingPass)
                                    {
                                        Toast.makeText(this, "Password changed.", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }else {
                                        startActivity(new Intent(MainActivity.this,LockerActivity.class));

                                    }

                                }
                                else
                                {
                                    showSnackBar(true,"Password doesn't match.");
                                }

                            }else {
                                showSnackBar(true,"Type full password");
                            }
                        }


                    }

                }

                // showing pass layout , do accordingly


            });
        }

        // AC button
        findViewById(R.id.sign4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (showingPassLayout!=1 )
                {
                    calculatorInput.setText("0");
                }
            }
        });


    }

    int updateType=0;
    InstallStateUpdatedListener listener;
    private AppUpdateManager appUpdateManager;
    boolean wasUpdating=false; // to avoid unnecessary onresume call
    private static final int MY_REQUEST_CODE = 123;
    void getUpdate()
    {
        /* immedate = 1
         * flexible=0 */




        Log.i("inAppUpdate","first");
        appUpdateManager = AppUpdateManagerFactory.create(this); //
        listener = state -> {
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                // After the update is downloaded, show a notification
                // and request user confirmation to restart the app.
                Toast.makeText(this, "App updated", Toast.LENGTH_LONG).show();
                appUpdateManager.completeUpdate();
            }

        };
        appUpdateManager.registerListener(listener);

        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();


        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {


            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(updateType)) // update available

            {
                try {

                    wasUpdating=true;
                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo, updateType, this, MY_REQUEST_CODE);


                } catch (IntentSender.SendIntentException e) {
                    Log.e("inAppUpdate",e.getMessage());
                }

            }

            Log.i("inAppUpdate","last");
        });

        //comment korta hoba update er somoy
        // setProgress();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_REQUEST_CODE) {
            if ( resultCode != RESULT_OK && updateType==1 ) // when update type immediate and failed
            {


                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.privacy_icon)
                        .setTitle("Update failed")
                        .setMessage("Please close the application and try again.")
                        .setCancelable(true)

                        .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finishAffinity();
                                System.exit(0);
                            }
                        })
                        .show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (verifyInstallerId()) // todo make it not true
        {
            new AlertDialog.Builder(this).setTitle("Invalid")
                    .setMessage("Please install the app from play store")
                    .setPositiveButton("Install", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(getString(R.string.playstoreUrl)+getPackageName())));
                        }
                    }).setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finishAffinity();
                            System.exit(1);
                        }
                    }).show();
        }else {
            getUpdate();
        }

    }

    private void showSnackBar(boolean isRed, String s) {
        int colorID=R.color.black;
        if (isRed)
        {
            colorID=R.color.colorAccent;
        }



        Snackbar snackbar = Snackbar.make(findViewById(R.id.parentLayout), s, Snackbar.LENGTH_SHORT)
                .setTextColor(ContextCompat.getColor(this,R.color.white))
                .setBackgroundTint(ContextCompat.getColor(this,colorID));


        snackbar.show();


    }

    private void showPassLayout()
    {


    }
}