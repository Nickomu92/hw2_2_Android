package com.nikomu.hw2_3_android;

import android.graphics.Bitmap;

public class GridViewItem {

    /**
     * Зображення елемента.
     */
    private Bitmap image;

    /**
     * Підпис елемента.
     */
    private String title;

    /**
     * Тип елемента (каталог, текстовий файл, інший файл)
     */
    private int fileType;

    public GridViewItem(Bitmap image, String title, int fileType) {
        this.image = image;
        this.title = title;
        this.fileType = fileType;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    @Override
    public String toString() {
        return "GridViewItem{" +
                "image=" + image +
                ", title='" + title + '\'' +
                '}';
    }
}
