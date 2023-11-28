package com.kittichanr.springgrpc.service;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DiskImageStore implements ImageStore {
    private String imageFolders;
    private ConcurrentMap<String, ImageMetaData> data;

    public DiskImageStore(String imageFolders) {
        this.imageFolders = imageFolders;
        this.data = new ConcurrentHashMap<>(0);
    }

    @Override
    public String Save(String laptopID, String imageType, ByteArrayOutputStream imageData) throws IOException {
        String imageID = UUID.randomUUID().toString();
        String imagePath = String.format("%s/%s%s", imageFolders, imageID, imageType);

        FileOutputStream fileOutputStream = new FileOutputStream(imagePath);
        imageData.writeTo(fileOutputStream);
        fileOutputStream.close();

        ImageMetaData metaData = new ImageMetaData(laptopID, imageType, imagePath);
        data.put(imageID, metaData);

        return imageID;
    }
}
