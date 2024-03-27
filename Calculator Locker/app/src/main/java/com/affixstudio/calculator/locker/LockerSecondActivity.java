package com.affixstudio.calculator.locker;

import static android.app.ProgressDialog.show;
import static com.affixstudio.calculator.locker.LockerActivity.getSecretKey;
import static com.affixstudio.calculator.locker.MainActivity.interstitialAd;
import static com.affixstudio.calculator.locker.PickFileActivity.getFileNameFromUri;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.affixstudio.calculator.Model.FileInfo;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAdView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.Target;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.stfalcon.imageviewer.listeners.OnDismissListener;
import com.stfalcon.imageviewer.loader.ImageLoader;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class LockerSecondActivity extends AppCompatActivity
{

    // Define UI elements and variables for handling media display and interaction
    RecyclerView galleryRecyclerView;
    LinearLayout emptyView, itemRecycleView, tools;
    private GalleryAdapter adapter; // Adapter for handling the display of media files
    private ArrayList<FileInfo> fileList; // List of FileInfo objects representing media files
    private SQLiteDatabase db; // Database for storing encrypted file info
    Context c;
    TextView cancelSelection, unhide, delete;
    CheckBox selectAll;
    LinearLayout selectionLayout;
    boolean wentToPickMedia = false; // Flag to check if user is coming back from picking media

    // Static block for loading SQLCipher library for encrypted database
    static {
        System.loadLibrary("sqlcipher");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locker_second);

        // Initialize variables and set up UI elements
        c=this;
        Intent in=getIntent();
        isVideo=in.getBooleanExtra("isPickingVideo",false);


        TextView title=findViewById(R.id.title);
        galleryRecyclerView=findViewById(R.id.galleryRecyclerView);
        emptyView=findViewById(R.id.emptyView);
        cancelSelection=findViewById(R.id.cancel);
        itemRecycleView=findViewById(R.id.itemRecycleView);
        selectAll=findViewById(R.id.selectAll);
        selectionLayout=findViewById(R.id.selectionLayout);
        tools=findViewById(R.id.tools);
        delete=findViewById(R.id.delete);
        unhide=findViewById(R.id.unhide);


        // Set onClickListener for adding new media, setting up RecyclerView, and handling media type
        findViewById(R.id.addNewMedia).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {

                wentToPickMedia=true;
                startActivity(new Intent(LockerSecondActivity.this,PickFileActivity.class).putExtra("isPickingVideo",isVideo));

            }
        });

        adapter = new GalleryAdapter();
        galleryRecyclerView.setAdapter(adapter);

        if(isVideo)
        {
            title.setText("Videos");
        }
        String query = "CREATE TABLE IF NOT EXISTS " + getString(R.string.fileInfoDBName) + " " + getString(R.string.tableCreationSql);



        // Initialize and configure the database for storing and fetching encrypted file information
        db = SQLiteDatabase.openOrCreateDatabase(getDBDirectory(this,getString(R.string.fileInfoDBName)),getString(R.string.databasePassWord), null);
        db.execSQL(query);
        galleryRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        fileList = new ArrayList<>();

        // Fetch files from the database and update UI accordingly
        fetchFilesFromDB(); // todo save video thumbnail before encrypting it

    }


    // Utility method to retrieve a bitmap image for display, handling decryption as needed
    private Bitmap getImage(String filePath) {
        // Decrypt file and return the bitmap
        try {
            File encryptedFile = new File(filePath);
            FileInputStream fis = new FileInputStream(encryptedFile);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKey secretKey = getSecretKey();
            byte[] iv = new byte[cipher.getBlockSize()];
            fis.read(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            CipherInputStream cis = new CipherInputStream(fis, cipher);
            Bitmap bitmap = BitmapFactory.decodeStream(cis);

            return bitmap;
            // Or if it's a video:
            // videoView.setVideoURI(Uri.fromFile(new File(filePath)));
            // videoView.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Utility method to get the directory for storing the database
    public static String getDBDirectory(Context c,String dbName) {
        // Return the path to the specified database within the application's storage directory
        // Create parent directory if needed
        File dir = new File(Environment.getExternalStorageDirectory(), c.getString(R.string.encryptedFolderName));
        if(!dir.exists()) {
            dir.mkdir();
        }

        // Return path to database file
        return dir.getAbsolutePath() + "/"+dbName+".db";

    }

    boolean isVideo=false;

    // Method to fetch files from the database and update the fileList and UI
    private void fetchFilesFromDB() {
        // Query the database for encrypted files, update fileList, and refresh RecyclerView

        fileList.clear();
        int mediaType=0;
        if (isVideo) {
            mediaType=1;
        }
        Cursor cursor = db.rawQuery("SELECT * FROM " + getString(R.string.fileInfoDBName)+" WHERE mediaType='"+mediaType+"'",null);
        while (cursor.moveToNext()) {

            if (new File(cursor.getString(4)).exists())
            {
                FileInfo fileInfo=new FileInfo(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getInt(5),cursor.getString(6));

                i("fileInfo in fetchFilesFromDB = = = =  "+fileInfo.getFile_Thumbnail_Path());
                fileList.add(fileInfo);
            }

        }
        Log.i("LockerSecond","fileList.size() "+fileList.size());

        if (fileList.size()>0)
        {
            Collections.reverse(fileList);
            emptyView.setVisibility(View.GONE);
            itemRecycleView.setVisibility(View.VISIBLE);
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
            adapter.notifyDataSetChanged();
        }


    }





    // GalleryAdapter class for binding media files to RecyclerView items
    public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder>
    {
        // Adapter methods for handling media file display in RecyclerView
        private boolean isLongPressed = false;

        ArrayList<FileInfo> selectedMedia=new ArrayList<>();
        private int clickCount=0;

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gallery_recycle_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int i) {
            int position =holder.getAdapterPosition();
            FileInfo file = fileList.get(position);

            Log.i("Locker","file.getFile_Thumbnail_Path() "+file.getFile_Thumbnail_Path());
            if (isVideo)
            {
                Glide.with(c)
                        .asBitmap()
                        .load(file.getFile_Path_o())
                        .override(500, 500) // Downsample the image
                        .format(DecodeFormat.PREFER_RGB_565) // Prefer a lower color format

                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache the video thumbnail
                        .into(holder.thumbnail);
            }else {
                Glide.with(c).load(file.getFile_Path_o()).override(500, 550).into(holder.thumbnail);

            }


            holder.check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    fileList.get(position).setSelected(b);
                    if (b)
                    {
                        selectedMedia.add(file);
                    }else {
                        selectedMedia.remove(file);
                    }
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                holder.check.setVisibility(View.VISIBLE);
                return true;
            });
            holder.itemView.setOnClickListener(new View.OnClickListener()   {
                @Override
                public void onClick(View view) {
                    if (isLongPressed)
                    {
                        holder.check.setChecked(!holder.check.isChecked());
                    }else
                    {
                        if (isVideo) {
                            startActivity(new Intent(c, ViewGalleryItemActivity.class)
                                    .putExtra("file_path", file.getFile_Path_o())
                                    .putExtra("o_path", file.getFile_path_d())
                            );
                        } else {

                            if (clickCount>=5) // ad on fifth photo
                            {
                                if (interstitialAd.isReady())
                                {
                                    interstitialAd.setListener(new MaxAdListener() {
                                        @Override
                                        public void onAdLoaded(MaxAd maxAd) {

                                        }

                                        @Override
                                        public void onAdDisplayed(MaxAd maxAd) {

                                        }

                                        @Override
                                        public void onAdHidden(MaxAd maxAd) {
                                            clickCount=0;
                                            interstitialAd.loadAd();
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
                                    interstitialAd.showAd();

                                }else {
                                    interstitialAd.loadAd();
                                }
                            }else {
                                clickCount++;
                            }


                            List<String> image = new ArrayList<>();
                            image.add(file.getFile_Path_o());
                            Window window = getWindow();


                            new StfalconImageViewer.Builder<>(c, image, new ImageLoader<String>() {
                                @Override
                                public void loadImage(ImageView imageView, String image) {
                                    window.setStatusBarColor(ContextCompat.getColor(c, android.R.color.black));
                                    Glide.with(c).asBitmap().load(image).into(imageView);
                                }
                            }).
                                    withBackgroundColor(ContextCompat.getColor(c, R.color.black))
                                    .withImageMarginPixels(20)
                                    .withTransitionFrom(holder.thumbnail).withHiddenStatusBar(true)
                                    .withDismissListener(new OnDismissListener() {
                                        @Override
                                        public void onDismiss() {
                                            window.setStatusBarColor(ContextCompat.getColor(c, R.color.white));
                                        }
                                    })
                                    .show();
                        }
                    }

                }
            });


            holder.itemView.setOnLongClickListener(v -> {

                isLongPressed=!isLongPressed;
                setCheckVisibleOrGone();


                return true;
            });

            if (isLongPressed)
            {
                holder.check.setVisibility(View.VISIBLE);
            }else {
                holder.check.setVisibility(View.GONE);
            }

            holder.check.setChecked(file.isSelected());





        }

        private void setCheckVisibleOrGone()
        {

            selectedMedia.clear();
            if (isLongPressed)
            {
                selectionLayout.setVisibility(View.VISIBLE);
                tools.setVisibility(View.VISIBLE);
            }else {
                selectionLayout.setVisibility(View.GONE);
                tools.setVisibility(View.GONE);
            }

            for (int j = 0; j < fileList.size(); j++) {
                if (!Objects.isNull(galleryRecyclerView.findViewHolderForAdapterPosition(j)))
                {
                    int shouldVisible=View.VISIBLE;

                    if (!isLongPressed)
                    {
                        shouldVisible=View.GONE;
                        fileList.get(j).setSelected(false);
                    }

                  //  ((ViewHolder) galleryRecyclerView.findViewHolderForAdapterPosition(j)).check.setVisibility(shouldVisible);
                }
            }
             notifyDataSetChanged(); // This will cause all views to refresh

        }

        private void createThumbnail(FileInfo file, ImageView thumbnail) throws IOException, GeneralSecurityException {

            i("Recreating the thumbnail");

            ExecutorService executor = Executors.newSingleThreadExecutor();


            CompletableFuture.runAsync(() -> {

                        try {


                            // Your background task here
                            File encryptedFile = new File(file.getFile_Path_o());

                            FileInputStream fis = new FileInputStream(encryptedFile);

                            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                            SecretKey secretKey = getSecretKey();

                            byte[] iv = new byte[cipher.getBlockSize()];
                            fis.read(iv);

                            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

                            CipherInputStream cis = new CipherInputStream(fis, cipher);

                            Bitmap bitmap;

                            if (isVideo)
                            {


                                FileDescriptor fd = fis.getFD();
                                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                                retriever.setDataSource(fd);
                                byte[] data = retriever.getEmbeddedPicture();

                                if(data != null) {
                                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                    // Use the bitmap as needed
                                }else {
                                    bitmap = retriever.getFrameAtTime();
                                }

                            }
                            else
                            {
                                // For image, decode bitmap directly
                                bitmap = BitmapFactory.decodeStream(cis);

                            }



                            Bitmap thumbnailBitmap = Bitmap.createScaledBitmap(Objects.requireNonNull(bitmap), 500, 500, false);


                            FileOutputStream thumbnailFos = new FileOutputStream(new File(getFilesDir(),getThumbnailName(file.getFile_Thumbnail_Path()) ));

                            thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, thumbnailFos);

                            thumbnailFos.close();

                            cis.close();
                            fis.close();


                        }catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }, executor)
                    .thenRun(() -> {

                        Glide.with(c).load(file.getFile_Thumbnail_Path()).into(thumbnail);
                    })
                    .whenComplete((result, error) -> {
                        executor.shutdown();  // Always shutdown executor when done
                    });


        }

        private String getThumbnailName(String fileThumbnailPath) {


            return fileThumbnailPath.substring(fileThumbnailPath.lastIndexOf("/")+1);
        }


        @Override
        public int getItemCount() {

            i("fileList.size() in getItemCount = "+fileList.size());
            return fileList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            ImageView thumbnail;
            CheckBox check;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                thumbnail = itemView.findViewById(R.id.thumbnail);
                check = itemView.findViewById(R.id.check);
                cancelSelection.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        isLongPressed=false;
                        setCheckVisibleOrGone();
                    }
                });
                selectAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        setSelection(b);
                    }
                });

                unhide.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        unhideFiles();
                    }
                });
                delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        deleteFiles();
                    }
                });
            }


        }

        private void deleteFiles() {
            if (selectedMedia.size()>0)
            {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                CompletableFuture.runAsync(() -> {
                            List<FileInfo> toRemove = new ArrayList<>();
                            // background task here
                            for (FileInfo f:selectedMedia)
                            {

                                // Define 'where' part of query.
                                String selection = "_id = ?";
                                // Specify arguments in placeholder order.
                                String[] selectionArgs = { String.valueOf(f.getFileID()) };
                                // Issue SQL statement.
                                db.delete(getString(R.string.fileInfoDBName), selection, selectionArgs);


                                File file=new File(f.getFile_Path_o());
                                if (file.exists())
                                {
                                    file.delete();



                                }
                                Log.i("LockerSecond","File Deleted");
                                toRemove.add(f);

                                // notifySystemFileModified(Uri.parse(f.getFile_path_d()));

                            }
                            fileList.removeAll(toRemove);

                        }, executor)
                        .thenRun(() -> {
                            // This will run once the background task is done
                            // If this is a UI application, make sure this code is thread-safe!
                            Log.i("lS","layout refreshed after unhide process");

                            setRecycleViewAfterFileModification();



                        })
                        .whenComplete((result, error) -> {
                            executor.shutdown();  // Always shutdown executor when done
                        });
            }else {
                Toast.makeText(c, "Please select a media.", Toast.LENGTH_SHORT).show();
            }


        }

        private void setRecycleViewAfterFileModification() {

            // setCheckVisibleOrGone();
            if (fileList.size()>0)
            {
                selectedMedia.clear();
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }else {
                galleryRecyclerView.removeAllViews();
                emptyView.setVisibility(View.VISIBLE);
                itemRecycleView.setVisibility(View.GONE);
                selectionLayout.setVisibility(View.GONE);
                tools.setVisibility(View.GONE);

            }
        }

        private void unhideFiles()
        {
            if (selectedMedia.size()>0)
            {

                ExecutorService executor = Executors.newSingleThreadExecutor();
                CompletableFuture.runAsync(() -> {
                            List<FileInfo> toRemove = new ArrayList<>();
                            // background task here
                            for (FileInfo f:selectedMedia)
                            {
                                // Define 'where' part of query.
                                String selection = "_id = ?";
                                // Specify arguments in placeholder order.
                                String[] selectionArgs = { String.valueOf(f.getFileID()) };
                                // Issue SQL statement.
                                db.delete(getString(R.string.fileInfoDBName), selection, selectionArgs);


                                File file=new File(f.getFile_Path_o());
                                if (file.exists())
                                {

                                    InputStream in = null;
                                    OutputStream out = null;


                                    try {

                                        String fileName=getFileNameFromUri(Uri.parse(f.getFile_Path_o()));

                                        //create output directory if it doesn't exist
                                        File dir = new File (f.getFile_path_d().substring(0,f.getFile_Path_o().lastIndexOf("/")-1));
                                        if (!dir.exists())
                                        {
                                            dir.mkdirs();
                                        }


                                        in = new FileInputStream( f.getFile_Path_o());

                                        String outPath=dir.getAbsoluteFile()+"/" + fileName;

                                        i("out path = "+outPath);
                                        out = new FileOutputStream(outPath);

                                        // Transfer bytes from in to out
                                        byte[] buf = new byte[1024];
                                        int len;
                                        while ((len = in.read(buf)) > 0) {
                                            out.write(buf, 0, len);
                                        }
                                        in.close();
                                        out.close();


                                        file.delete();

                                        Log.i("LockerSecond","File Unhide");
                                        notifySystemFileModified(Uri.parse(f.getFile_path_d()));




                                    }
                                    catch (IOException e) {
                                        Log.e("tag", e.getMessage());
                                    }


                                }
                                toRemove.add(f);

                                // notifySystemFileModified(Uri.parse(f.getFile_path_d()));

                            }
                            fileList.removeAll(toRemove);

                        }, executor)
                        .thenRun(() -> {
                            // This will run once the background task is done
                            // If this is a UI application, make sure this code is thread-safe!
                            Log.i("lS","layout refreshed after unhide process");

                            setRecycleViewAfterFileModification();


                        })
                        .whenComplete((result, error) -> {
                            executor.shutdown();  // Always shutdown executor when done
                        });


            }else {
                Toast.makeText(c, "Please select a media.", Toast.LENGTH_SHORT).show();
            }


        }

        private void notifySystemFileModified(Uri contentUri) {
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);

            scanIntent.setData(contentUri);
            sendBroadcast(scanIntent);
        }

        private void setSelection(boolean b) {
            for (int j = 0; j < fileList.size(); j++) {

                    fileList.get(j).setSelected(b);
                 //   ((ViewHolder) galleryRecyclerView.findViewHolderForAdapterPosition(j)).check.setChecked(b);

            }
            adapter.notifyDataSetChanged();
        }
    }

    public class FilePath
    {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if returning from media picking activity and refresh file list if true
        if (wentToPickMedia)
        {
            fetchFilesFromDB();
            wentToPickMedia=false;
        }
    }


    public void onBackPressed(View v) {
        super.onBackPressed();
    }

    // Utility method for logging
    void i(String s)
    {
        Log.i("LockerSecond",s);
    }
}
