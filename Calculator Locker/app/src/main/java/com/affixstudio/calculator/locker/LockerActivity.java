package com.affixstudio.calculator.locker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.affixstudio.calculator.Model.AppLovinNative;
import com.affixstudio.calculator.Model.Dialog.Dialog;
import com.affixstudio.calculator.Model.FileInfo;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAdView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class LockerActivity extends AppCompatActivity
{
    // Declaration of constants and variables for managing permissions and activities
    private  final int MANAGE_FULL_STORAGE = 1211;
    final int PERMISSION_REQUEST_CODE = 1212;
    Context c;
    Activity a;
    public static int File_info_db_version=1;

    private List<FileInfo> pickedFilesInfo=new ArrayList<>();


    Dialog dialogClass;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locker);


        // Context and activity assignments
        c=LockerActivity.this;
        a=LockerActivity.this;

        // Finding and setting up UI components (FloatingActionButton, ImageButtons)
        FloatingActionButton addMedia=findViewById(R.id.addNewMedia);
        ImageButton addVideo=findViewById(R.id.addVideo);
        ImageButton addImage=findViewById(R.id.addImage);

        // Dialog for close event preparation
        dialogClass=new Dialog(this);
        dialogClass.prepareCloseDialog();

        // Advertisement setup using AppLovin SDK
        AppLovinNative lovinNative=new AppLovinNative(R.layout.native_ad_big,this);
        lovinNative.mid(findViewById(R.id.ad_frame));

        // Ad view listener setup
        MaxAdView adView = findViewById( R.id.bn_appLovin );
        adView.setListener(new MaxAdViewAdListener() {
            @Override
            public void onAdExpanded(MaxAd maxAd) {

            }

            @Override
            public void onAdCollapsed(MaxAd maxAd) {

            }

            @Override
            public void onAdLoaded(MaxAd maxAd) {
                adView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdDisplayed(MaxAd maxAd) {

            }

            @Override
            public void onAdHidden(MaxAd maxAd) {

            }

            @Override
            public void onAdClicked(MaxAd maxAd) {

            }

            @Override
            public void onAdLoadFailed(String s, MaxError maxError) {

            }

            @Override
            public void onAdDisplayFailed(MaxAd maxAd, MaxError maxError) {

            }
        });

        adView.loadAd();



        // Setting onClick listeners for UI components to handle user interactions
        addMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

               // onClickVidOrImg(true);
                if (addVideo.getVisibility()==View.VISIBLE)
                {
                    addVideo.setVisibility(View.GONE);
                    addImage.setVisibility(View.GONE);
                }else {
                    addVideo.setVisibility(View.VISIBLE);
                    addImage.setVisibility(View.VISIBLE);
                }

            }
        });

        findViewById(R.id.setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LockerActivity.this,SettingActivity.class));
            }
        });


        addVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickVidOrImg(true);
            }
        });
        addImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickVidOrImg(false);
            }
        });
        findViewById(R.id.openVideoScreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openShowScreen(true);
            }
        });
        findViewById(R.id.openPhotoScreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openShowScreen(false);
            }
        });


    }

    // onClickVidOrImg method: Starts an activity to pick a video or an image
    void onClickVidOrImg(boolean isVideo)
    {

        if (checkPermission()) {
            startActivity(new Intent(this,PickFileActivity.class).putExtra("isPickingVideo",isVideo));

        }else {
            requestPermission();
        }
    }

    // openShowScreen method: Starts an activity to show picked videos or images
    void openShowScreen(boolean isVideo)
    {

        if (checkPermission()) {
            startActivity(new Intent(this,LockerSecondActivity.class).putExtra("isPickingVideo",isVideo));

        }else {
            requestPermission();
        }




    }





    String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // Permission check and request methods
    private boolean checkPermission() {

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.R)
        {
            return Environment.isExternalStorageManager();
        }else {
            return (ContextCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, permissions[1]) == PackageManager.PERMISSION_GRANTED);
        }






    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.R)
        {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
          //  intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivityForResult(intent, MANAGE_FULL_STORAGE);
        }else {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }




    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED)
                {
                    // Permissions granted, do task
                //    filePickerDialog.show();
                } else {

                    new AlertDialog.Builder(c).setMessage("Storage read write permission required.")
                                    .setTitle("Action Required")
                                        .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                intent.setData(Uri.parse("package:" + getPackageName()));
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);
                                            }
                                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            }).setCancelable(true).show();

                }
                break;
            case MANAGE_FULL_STORAGE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        // Permission granted
                        i("Manage full storage granted");
                     //   filePickerDialog.show();
                    } else {
                        // Permission denied
                        i("Manage full storage not granted");
                        new AlertDialog.Builder(c).setMessage("Manage all files permission required.")
                                .setTitle("Action Required")
                                .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        requestPermission();
                                    }
                                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                                }).setCancelable(true).show();
                    }
                }

        }

    }



    // onActivityResult method: Handles the result of activities started for result
    @SuppressLint("WrongConstant")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);






    }

    public String getGeneralType(Uri uri) {
        String mimeType = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            ContentResolver cr = getApplicationContext().getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }

        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                return "image";
            } else if (mimeType.startsWith("video/")) {
                return "video";
            }
        }

        return null;
    }


    private void i(String s) {

        Log.d("LockerActivity",s);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // Custom behavior for the back button press
        dialogClass.closeDialog.show();

    }




    // Encryption and decryption methods
    @SuppressLint("SimpleDateFormat")
    private String createNewEncryptedFileName() {


        Date now = new Date();

        // Create a SimpleDateFormat with milliseconds
         SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

        // Format the current date and time with milliseconds
        String formattedDateTime = dateFormat.format(now);

        // Create a file name with the formatted date and time
        return  "dsa_" + formattedDateTime+".dat" ;

    }

    public static SecretKey getSecretKey() {

        // Start with incremental bytes
        byte[] keyBytes = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F};

        // Modify some bytes to add complexity
        keyBytes[0] = (byte)0xAB;
        keyBytes[5] = (byte)0x37;
        keyBytes[10] = (byte)0x1F;
        keyBytes[15] = (byte)0xEE;
       return new SecretKeySpec(keyBytes, "AES");
    }
    private void decryptFile(Uri encryptedUri, Uri outputUri) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        CompletableFuture.runAsync(() -> {
                    try {
                        // Open an input stream to read the encrypted file contents
                      //  InputStream in = getContentResolver().openInputStream(encryptedUri);
                        // Open encrypted file
                        FileInputStream in = new FileInputStream(encryptedUri.getPath());

                        // Create a cipher stream to decrypt the data
                        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

                        // Use the same secret key as before
                        SecretKey secretKey = getSecretKey();

                        byte[] iv = new byte[16]; // 16 bytes is the size of the IV for AES
                        in.read(iv);
                        // Initialize the cipher with the IV
                        IvParameterSpec ivSpec = new IvParameterSpec(iv);
                        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

                        CipherInputStream cin = new CipherInputStream(in, cipher);

                        // Open an output stream to write the decrypted file to the output URI
                        OutputStream out = getContentResolver().openOutputStream(outputUri);

                        // Write decrypted data...
                        byte[] buffer = new byte[1024];
                        int read;
                        while((read = cin.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        i("File restored");
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(c, "Something went wrong. Please try again.", Toast.LENGTH_LONG).show();
                    }
                }, executor)
                .thenRun(() -> {
                    // This will run once the background task is done
                    // If this is a UI application, make sure this code is thread-safe!
                })
                .whenComplete((result, error) -> {
                    executor.shutdown();  // Always shutdown executor when done
                });
    }

    private String getFileName(String filePath) {
        return filePath.substring(filePath.lastIndexOf("/")+1);
    }
    // Inner class for media items
    public class MediaItem {
        private boolean isPhoto;
        private boolean isLandscape;
        private int imageResource;
        private String videoUri;

        public MediaItem(boolean isPhoto, boolean isLandscape, int imageResource, String videoUri) {
            this.isPhoto = isPhoto;
            this.isLandscape = isLandscape;
            this.imageResource = imageResource;
            this.videoUri = videoUri;
        }

        public boolean isPhoto() {
            return isPhoto;
        }

        public boolean isLandscape() {
            return isLandscape;
        }

        public int getImageResource() {
            return imageResource;
        }

        public String getVideoUri() {
            return videoUri;
        }
    }

}