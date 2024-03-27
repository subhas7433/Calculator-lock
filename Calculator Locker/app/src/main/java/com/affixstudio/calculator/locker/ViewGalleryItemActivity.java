package com.affixstudio.calculator.locker;

import static com.affixstudio.calculator.locker.LockerActivity.getSecretKey;
import static com.affixstudio.calculator.locker.MainActivity.interstitialAd;

import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxError;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.jarvanmo.exoplayerview.media.SimpleMediaSource;
import com.jarvanmo.exoplayerview.ui.ExoVideoView;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class ViewGalleryItemActivity extends AppCompatActivity {


    private MediaPlayer mediaPlayer;
    private SurfaceView surfaceView;
    SimpleExoPlayer player;
    PlayerView playerView;
    ExoVideoView videoView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_item);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ImageView imageView = findViewById(R.id.imageView);
        // VideoView videoView = findViewById(R.id.fullScreenVideoView);

        String filePath = getIntent().getStringExtra("file_path");
        String decryptPath = getIntent().getStringExtra("o_path");
        Log.i("file_path","file path "+filePath);


        player = new SimpleExoPlayer.Builder(this).build();
        playerView = findViewById(R.id.player_view);
        playerView.setPlayer(player);


        Uri uri = Uri.parse(filePath);
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(new DefaultDataSourceFactory(this, "app-name"))
                .createMediaSource(MediaItem.fromUri(uri));
        player.prepare(mediaSource, false, false);
        player.setPlayWhenReady(true);

//        videoView=findViewById(R.id.videoView);
//        SimpleMediaSource mediaSource = new SimpleMediaSource(Uri.parse(filePath));//uri also supported
//        videoView.play(mediaSource);
//        videoView.play(mediaSource,true);//play from a particular position


    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        playerView = findViewById(R.id.player_view);
        playerView.setPlayer(player);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            // In landscape mode, expand video to full screen
            ViewGroup.LayoutParams params = playerView.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            playerView.setLayoutParams(params);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            // In portrait mode, set video height to a specific dp value or MATCH_PARENT
            ViewGroup.LayoutParams params = playerView.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT; // or specific dp
            playerView.setLayoutParams(params);
        }
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

                        File file = new File(outputUri.getPath());
                        if (!file.exists()) {
                            file.createNewFile();
                        }

                        Uri contentUri = FileProvider.getUriForFile(this, getPackageName()+".provider", file);
                        // Open an output stream to write the decrypted file to the output URI
                        OutputStream out = getContentResolver().openOutputStream(contentUri);

                        // Write decrypted data...
                        byte[] buffer = new byte[1024];
                        int read;
                        while((read = cin.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                        Log.i("","File restored");
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Something went wrong. Please try again.", Toast.LENGTH_LONG).show();
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


    @Override
    public void onBackPressed() {
        player.release();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (interstitialAd.isReady())
        {
            interstitialAd.setListener(new MaxAdListener() {
                @Override
                public void onAdLoaded(MaxAd maxAd) {

                }

                @Override
                public void onAdDisplayed(MaxAd maxAd)
                {

                }

                @Override
                public void onAdHidden(MaxAd maxAd)
                {
                    interstitialAd.loadAd();
                    finish();
                }

                @Override
                public void onAdClicked(MaxAd maxAd)
                {

                }

                @Override
                public void onAdLoadFailed(String s, MaxError maxError) {

                }

                @Override
                public void onAdDisplayFailed(MaxAd maxAd, MaxError maxError) {

                }
            });
            interstitialAd.showAd();
        }else
        {
            interstitialAd.loadAd();
            super.onBackPressed();
        }





    }
}