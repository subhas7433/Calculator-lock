package com.affixstudio.calculator.locker;

import static com.affixstudio.calculator.locker.LockerActivity.File_info_db_version;
import static com.affixstudio.calculator.locker.LockerSecondActivity.getDBDirectory;
import static com.affixstudio.calculator.locker.MainActivity.interstitialAd;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.affixstudio.calculator.Model.Database;
import com.affixstudio.calculator.Model.FileInfo;
import com.affixstudio.calculator.Model.MediaFile;
import com.affixstudio.calculator.Model.SecureDatabase;
import com.affixstudio.calculator.RecycleAdapters.PickFilesAdapter;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxError;
import com.google.android.material.snackbar.Snackbar;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AlgorithmParameters;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class PickFileActivity extends AppCompatActivity
{

    // RecyclerView for displaying media files
    private RecyclerView recyclerView;
    // Adapter for the RecyclerView to display media files
    private PickFilesAdapter imageAdapter;
    // List to hold media files for selection
    private List<MediaFile> mediaFiles;
    // Request code for runtime permission
    final int REQUEST_PERMISSION_CODE = 1;

    // Paths of selected files
    List<String> pickedFilesPath = new ArrayList<>();
    // UI elements for loading and submission
    LinearLayout loadingView, pickLayout;

    // Activity and context references
    Activity a;
    Context c;

    // Progress dialog to indicate processing
    ProgressDialog pd;

    // Submit button for finalizing file selection
    Button submit;

    // Database for storing selected file information
    SQLiteDatabase db;

    // Load SQLCipher library for database encryption
    static {
        System.loadLibrary("sqlcipher");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_file);

        // Initialize activity and context references
        a=this;
        c=this;

        // Get intent to check if picking video
        Intent in=getIntent();
        isPickingVideo=in.getBooleanExtra("isPickingVideo",false);

        // Initialize UI components
        submit=findViewById(R.id.submit);
        loadingView = findViewById(R.id.loadingView);
        pickLayout = findViewById(R.id.pickLayout);

        String query="CREATE TABLE IF NOT EXISTS "+getString(R.string.fileInfoDBName)+ " "+getString(R.string.tableCreationSql);

        db=SQLiteDatabase.openOrCreateDatabase(getDBDirectory(this,getString(R.string.fileInfoDBName)),getString(R.string.databasePassWord), null);

        db.execSQL(query);
        // Initialize the mediaFiles list
        mediaFiles = new ArrayList<>();
        // Set up the RecyclerView
        recyclerView = findViewById(R.id.pickFileRecycle);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3)); // Change the number of columns as needed
        recyclerView.getRecycledViewPool().setMaxRecycledViews(0, 20);

        // Setup select all/unselect all functionality
        setupSelectAllButton();

        // Check and request necessary permissions
        checkAndRequestPermissions();

        // Initialize and show progress dialog
        setupProgressDialog();

        // Handle submission of selected files
        handleSubmitButtonClick();

    }
    // Method to check and request necessary permissions
    private void checkAndRequestPermissions() {
        // Implementation to check and request permissions for reading external storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                fetchMediaFiles();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, REQUEST_PERMISSION_CODE);
            }
        }
        else
        {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERMISSION_CODE);
            } else {
                fetchMediaFiles();
            }
        }
    }


    // Method to setup the Select All/Unselect All functionality
    private void setupSelectAllButton() {
        // Implementation to toggle selection of all media files displayed
        AppCompatButton selectALL= findViewById(R.id.selectALL);
        selectALL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectALL.getText().toString().equalsIgnoreCase(getString(R.string.selectAll)))
                {
                    selectALL.setText(getString(R.string.unselectALL));
                    imageAdapter.setSelection(true);
                }else {
                    selectALL.setText(getString(R.string.selectAll));
                    imageAdapter.setSelection(false);
                }
            }
        });
    }

    // Method to setup progress dialog
    private void setupProgressDialog() {
        pd=new ProgressDialog(this);
        pd.setMessage("Encrypting your data");
        pd.setCancelable(false);
    }

    // Method to handle submission of selected files
    private void handleSubmitButtonClick() {
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {

                pickedFilesPath=getSelectedImages();
                if (pickedFilesPath.size()>0)
                {
                    currentFileProccessIndex=0;

                    processTheMedia(Uri.parse(pickedFilesPath.get(0)));
                }else {
                    showSnackBar(true,"No file selected");
                }

            }
        });
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check if permission was granted and proceed with fetching media files
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchMediaFiles();
            }
            else
            {
                // Handle the case where permission is denied
                Toast.makeText(this, "Permission denied to read external storage", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check if permission was granted and proceed with fetching media files
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // Permission granted, proceed with fetching media files
                    fetchMediaFiles();
                } else {
                    // Permission denied
                    Toast.makeText(this, "Permission denied to manage external storage", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }




    private void fetchImagesInDirectory(File directory, List<MediaFile> mediaFiles) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    String dirName = file.getName().toLowerCase();
                    // Exclude more system directories
                    if ( !dirName.startsWith(".") && !dirName.startsWith("cache")
                            && !dirName.equals("data") && !dirName.equals("obb") && !dirName.equals("system")
                            && !dirName.equals("sys") && !dirName.equals("etc")) {
                        fetchImagesInDirectory(file, mediaFiles);
                    }
                } else {
                    String fileName = file.getName().toLowerCase();
                    if (isPickingVideo) {
                        if (fileName.endsWith(".mp4") || fileName.endsWith(".mkv") || fileName.endsWith(".webm")
                                || fileName.endsWith(".avi") || fileName.endsWith(".mov")) {
                            addMediaFile(file, mediaFiles);
                        }
                    } else {
                        if (fileName.endsWith(".jpg") || fileName.endsWith(".png") || fileName.endsWith(".jpeg")
                                || fileName.endsWith(".bmp") || fileName.endsWith(".gif") || fileName.endsWith(".webp")) {
                            addMediaFile(file, mediaFiles);
                        }
                    }


                }
            }
        }
    }

    // Method to add a media file to the list
    private void addMediaFile(File file, List<MediaFile> mediaFiles) {
        String filePath = file.getAbsolutePath();
        MediaFile mediaFile = new MediaFile();
        mediaFile.setDisplayName(file.getName());
        mediaFile.setFilePath(filePath);

        long dateCreated = file.lastModified(); // Fetch the date created
        mediaFile.setDateCreated(dateCreated);
        mediaFiles.add(mediaFile);
    }

    // Method to get selected media files
    private List<String> getSelectedImages() {
        List<String> selectedImages = new ArrayList<>();
        // Loop through each media file and add it to the list if it is selected
        for (MediaFile mediaFile : mediaFiles) {
            if (mediaFile.isSelected()) {
                selectedImages.add(mediaFile.getFilePath());
            }
        }
        return selectedImages;
    }


    private final Executor executor = Executors.newSingleThreadExecutor(); // Create a single-threaded executor
    // Method to fetch media files from external storage
    private void fetchMediaFiles() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                List<MediaFile> fetchedMediaFiles = new ArrayList<>();
                File externalStorageDirectory = Environment.getExternalStorageDirectory();
                fetchImagesInDirectory(externalStorageDirectory, fetchedMediaFiles);

                // Sort the list by date created
                fetchedMediaFiles.sort(new Comparator<MediaFile>() {
                    @Override
                    public int compare(MediaFile o1, MediaFile o2) {
                        return Long.compare(o2.getDateCreated(), o1.getDateCreated()); // Sort in descending order
                    }
                });
                // Update the RecyclerView on the main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingView.setVisibility(View.GONE);
                        pickLayout.setVisibility(View.VISIBLE);
                        submit.setVisibility(View.VISIBLE);

                        mediaFiles.clear();
                        mediaFiles.addAll(fetchedMediaFiles);
                        imageAdapter = new PickFilesAdapter(mediaFiles);

                         recyclerView.setAdapter(imageAdapter);
                         if(mediaFiles.isEmpty())
                         {
                             findViewById(R.id.selectLayout).setVisibility(View.GONE);
                             recyclerView.setVisibility(View.GONE);
                             findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
                         }
                        //imageAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    private void showSnackBar(boolean isRed,String s) {
        int colorID=R.color.black;
        if (isRed)
        {
            colorID=R.color.colorAccent;
        }



        Snackbar snackbar = Snackbar.make(findViewById(R.id.parentLayout), s, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ContextCompat.getColor(this,colorID));

        snackbar.show();


    }


    int currentFileProccessIndex=0; // index of the file to be processed
    private FileInfo pickedFilesInfo; // file info to be processed
    boolean isPickingVideo=false; // is picking video or other media
    private void processTheMedia(Uri path)
    {


        // show progress dialog
        if (!pd.isShowing())
        {
            pd.show();
        }



        String fileName=getFileNameFromUri(path);
        // create file info object
        pickedFilesInfo=new FileInfo(0,fileName,"","",path.toString(),0,"0");


        if (isPickingVideo) // if picking video
        {
            pickedFilesInfo=new FileInfo(0,fileName,"","",path.toString(),1,"0"); // if video then mediaType=1
        }

        transferVideoFile(path);// transfer video file to lockerSecond screen

//        if (isPickingVideo)
//        {
//
//        }else {
//            encryptFile(path); // encryption takes long time
//        }


    }

    // method to transfer video file to lockerSecond screen
    private void transferVideoFile(Uri path) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture.runAsync(() -> {

                    InputStream in = null;
                    OutputStream out = null;


                    try {

                        String fileName=getFileNameFromUri(path); // get file name from uri
                        File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES); // get pictures directory

                        //create output directory if it doesn't exist
                        File dir = new File (picturesDirectory,getString(R.string.encryptedFolderName));
                        if (!dir.exists())
                        {
                            dir.mkdirs();
                        }


                        in = new FileInputStream( path.getPath());

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



                        pickedFilesInfo.setFile_name_d(fileName);

                       // saveTheVideoThumbnail(path);
                        pickedFilesInfo.setFile_Thumbnail_Path(""); // thumbnail not required for now
                        pickedFilesInfo.setFile_path_d(outPath);



                    }
                    catch (IOException e) {
                        Log.e("tag", e.getMessage());
                    }
                    // Your background task here

                }, executor)
                .thenRun(() -> {
                    // This will run once the background task is done
                    deleteTheOriginalFile(path);

                })
                .whenComplete((result, error) -> {
                    executor.shutdown();  // shutdown executor when done
                });

    }


    public static String getFileNameFromUri( Uri uri)
    {

        return uri.getPath().substring(uri.getPath().lastIndexOf("/")+1);
    }


    private void deleteTheOriginalFile(Uri path)
    {

        i("deleteTheOriginalFile");

        File file=new File(String.valueOf(path));

        if (file.delete())
        {
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(file);
            scanIntent.setData(contentUri);
            sendBroadcast(scanIntent); // send broadcast to system to scan the files for changes

            // increase each time a process task is completed
            currentFileProccessIndex+=1; // increase index of the file to be processed
            i("Original File deleted");
            i("pickedFilesPath.size size = "+pickedFilesPath.size()+" | currentFileProccessIndex="+currentFileProccessIndex+" ");

            saveFileInfoIntoDB(); // save file info into database

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (pickedFilesPath.size()>currentFileProccessIndex)// if there is more files to process
                    {
                        processTheMedia(Uri.parse(pickedFilesPath.get(currentFileProccessIndex))); // process the next file

                    }else { // if there is no more files to process
                        pd.dismiss(); // dismiss progress dialog

                        // show lockerSecond screen after an ad if available
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
                                    interstitialAd.loadAd();
                                    Toast.makeText(c, "Hiding process Completed", Toast.LENGTH_LONG).show();
                                    finish();
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
                            Toast.makeText(c, "Hiding process Completed", Toast.LENGTH_LONG).show();
                            finish();
                        }




                    }
                }
            });


        }else {
            i("Original File not deleted");
        }


    }

    private void saveFileInfoIntoDB() {

        db.beginTransaction();

        try {
            // Insert the file info into the database
            String sql = "INSERT INTO " + getString(R.string.fileInfoDBName) + " (file_original_Name, file_Path_o, file_name_d, file_path_d, file_Thumbnail_Path, mediaType) VALUES (?, ?, ?, ?, ?, ?)";

            Object[] args = new Object[] {
                    pickedFilesInfo.getFile_original_Name(),
                    pickedFilesInfo.getFile_Path_o(),
                    pickedFilesInfo.getFile_name_d(),
                    pickedFilesInfo.getFile_path_d(),
                    pickedFilesInfo.getFile_Thumbnail_Path(),
                    isPickingVideo ? 1 : 0
            };

            db.execSQL(sql, args);

            Log.i("Database Operation", "File info successfully inserted into db");
            db.setTransactionSuccessful();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        finally {
            db.endTransaction();
        }



    }




    private void i(String s) {

        Log.d("LockerActivity",s);
    }

    private void encryptFile(Uri uri)
    {

        //uri= Uri.fromFile(new File(Environment.getExternalStorageDirectory().getAbsolutePath()+Objects.requireNonNull(uri.getPath()).replace("/sfb_files","")));
        ExecutorService executor = Executors.newSingleThreadExecutor();


        CompletableFuture.runAsync(() -> {
                    // Your background task here
                    try {
                        File file = new File(uri.getPath());
                        if (!file.exists()) {
                            Toast.makeText(c, "File does not exist.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        Uri contentUri = FileProvider.getUriForFile(c, getPackageName()+".provider", file);

                        //Open an input stream to read the file contents
                        InputStream in = getContentResolver().openInputStream(contentUri);
                        //Create a cipher stream to encrypt the data:

                        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");


                        // Use keyBytes as before
                        SecretKey secretKey = getSecretKey();


                        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                        AlgorithmParameters params = cipher.getParameters();// changed
                        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();// changed
                        CipherInputStream cin = new CipherInputStream(in, cipher);


                        // Get external storage directory
                        File extDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);



                        // Create your app's folder
                        File appFolder = new File(extDir, getString(R.string.encryptedFolderName));
                        if (!appFolder.exists())
                        {
                            appFolder.mkdir();
                        }


                        String duplicate_file_name=createNewEncryptedFileName();

                        pickedFilesInfo.setFile_name_d(duplicate_file_name);

                        // Create the encrypted file
                        File encryptedFile = new File(appFolder, createNewEncryptedFileName());

                        FileOutputStream out = new FileOutputStream(encryptedFile);
                        // Write encrypted data...
                        out.write(iv); // changed
                        byte[] buffer = new byte[1024];
                        int read;
                        while((read = cin.read(buffer)) != -1)
                        {
                            out.write(buffer, 0, read);
                        }

                        out.close();

                        if (isPickingVideo)
                        {
                            saveTheVideoThumbnail(uri);
                        }else {
                            pickedFilesInfo.setFile_Thumbnail_Path(""); // thumbnail not required for images
                        }

                        pickedFilesInfo.setFile_path_d(encryptedFile.getAbsolutePath());





                    }catch (Exception e)
                    {
                        e.printStackTrace();
                        Toast.makeText(c, "Something went wrong. Please try again.", Toast.LENGTH_LONG).show();
                    }
                }, executor)
                .thenRun(() -> {
                    // This will run once the background task is done
                    // If this is a UI application, make sure this code is thread-safe!
                    deleteTheOriginalFile(uri);

                })
                .whenComplete((result, error) -> {
                    executor.shutdown();  // Always shutdown executor when done
                });




    }

    @SuppressLint("SimpleDateFormat")
    private void saveTheVideoThumbnail(Uri uri)
    {

        Bitmap thumbnail=null;
        if (isPickingVideo)
        {
            thumbnail = ThumbnailUtils.createVideoThumbnail(uri.getPath(), MediaStore.Video.Thumbnails.MICRO_KIND);
        }else {
            Bitmap originalBitmap = BitmapFactory.decodeFile(uri.getPath());
            int thumbnailWidth = originalBitmap.getWidth() / 5;
            int thumbnailHeight = originalBitmap.getHeight() / 5; // 1200 / 10 = 120
            thumbnail = Bitmap.createScaledBitmap(originalBitmap, thumbnailWidth, thumbnailHeight, false);
        }

        FileOutputStream fos = null;
        String fileName=new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())+"_thumbnail.jpg";
        try {
            fos = new FileOutputStream(new File(getFilesDir(), fileName));
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 60, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        pickedFilesInfo.setFile_Thumbnail_Path(new File(getFilesDir(),fileName).getAbsolutePath());



    }

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

    private SecretKey getSecretKey() {

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

                        File file = new File(outputUri.getPath());
                        if (!file.exists()) {
                            file.createNewFile();
                        }

                        Uri contentUri = FileProvider.getUriForFile(c, getPackageName()+".provider", file);
                        // Open an output stream to write the decrypted file to the output URI
                        OutputStream out = getContentResolver().openOutputStream(contentUri);

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

}