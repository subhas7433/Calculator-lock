package com.affixstudio.calculator.Model;

public class FileInfo {

    int fileID;
    String file_original_Name;
    String file_path_d;
    String file_name_d;
    String file_Path_o;
    String file_Thumbnail_Path;
    int mediaType;

    boolean isSelected=false; // use to check if selected on recycleview

    public String getFile_Thumbnail_Path() {
        return file_Thumbnail_Path;
    }

    public void setFile_Thumbnail_Path(String file_Thumbnail_Path) {
        this.file_Thumbnail_Path = file_Thumbnail_Path;
    }

    public FileInfo(int fileID, String file_original_Name, String file_path_d, String file_name_d, String file_Path_o, int mediaType,String file_Thumbnail_Path) {
        this.fileID = fileID;
        this.file_original_Name = file_original_Name;
        this.file_path_d = file_path_d;
        this.file_name_d = file_name_d;
        this.file_Path_o = file_Path_o;
        this.mediaType = mediaType;
        this.file_Thumbnail_Path = file_Thumbnail_Path;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public int getFileID() {
        return fileID;
    }

    public String getFile_original_Name() {
        return file_original_Name;
    }

    public String getFile_path_d() {
        return file_path_d;
    }

    public String getFile_name_d() {
        return file_name_d;
    }

    public String getFile_Path_o() {
        return file_Path_o;
    }

    public int getMediaType() {
        return mediaType;
    }

    public void setFileID(int fileID) {
        this.fileID = fileID;
    }

    public void setFile_original_Name(String file_original_Name) {
        this.file_original_Name = file_original_Name;
    }

    public void setFile_path_d(String file_path_d) {
        this.file_path_d = file_path_d;
    }

    public void setFile_name_d(String file_name_d) {
        this.file_name_d = file_name_d;
    }

    public void setFile_Path_o(String file_Path_o) {
        this.file_Path_o = file_Path_o;
    }

    public void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }
}
