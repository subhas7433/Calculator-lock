package com.affixstudio.calculator.RecycleAdapters;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.affixstudio.calculator.Model.MediaFile;
import com.affixstudio.calculator.locker.LockerSecondActivity;
import com.affixstudio.calculator.locker.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PickFilesAdapter extends RecyclerView.Adapter<PickFilesAdapter.ImageViewHolder>
{

    private List<MediaFile> mediaFiles;

    public PickFilesAdapter(List<MediaFile> mediaFiles) {
        this.mediaFiles = mediaFiles;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pick_file_recycle_item, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position)
    {
        MediaFile mediaFile = mediaFiles.get(holder.getAbsoluteAdapterPosition());
        holder.bind(mediaFile);



        holder.check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                mediaFile.setSelected(b);
                Log.i("PickFilesAdapter","path of selected file = "+mediaFile.getFilePath());
            }
        });
       // allCheckBox.add(holder.check);

     //   holder.check.setChecked(shouldSelectAll);
        holder.check.setChecked(mediaFile.isSelected());
    }

    @Override
    public int getItemCount() {
        return mediaFiles.size();
    }

    public  class ImageViewHolder extends RecyclerView.ViewHolder {

        private ImageView thumbnail;
        private CheckBox check;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            check = itemView.findViewById(R.id.check);

        }

        public void bind(MediaFile mediaFile)
        {
            // Load the image using a library like Glide or Picasso
            if (mediaFile.getFilePath().toLowerCase().endsWith(".mp4")) {
                // If the file is a video, load the video thumbnail
                Glide.with(itemView.getContext())
                        .asBitmap()
                        .load(mediaFile.getFilePath())
                        .override(300, 300) // Downsample the image
                        .format(DecodeFormat.PREFER_RGB_565) // Prefer a lower color format

                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache the video thumbnail
                        .into(thumbnail);



            } else {
                // If the file is an image, load the image
                Glide.with(itemView.getContext())
                        .load(mediaFile.getFilePath())
                        .override(300, 300) // Downsample the image
                        .format(DecodeFormat.PREFER_RGB_565) // Prefer a lower color format
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(thumbnail);

            }

        }
    }



    @SuppressLint("NotifyDataSetChanged")
    public void setSelection(boolean b)
    {
        for (MediaFile mediaFile:mediaFiles)
        {
            mediaFile.setSelected(b);
        }

       notifyDataSetChanged();
    }
}
