package com.sudarshanbhat.memorygame;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by sudarshan on 8/23/14.
 * Singleton class as a cache to all the downloaded Flickr Photos
 * Manager photo downloading, caching in memory and disk, and loading
 * photos into imageviews
 */
public class FlickrPhotoCache {

    private LruCache<String, Bitmap> mImageLruCache;

    private File mCacheDir;

    private static FlickrPhotoCache sInstance;

    public static FlickrPhotoCache getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new FlickrPhotoCache(context);
        }

        return sInstance;
    }

    // Singleton. Private constructor.
    private FlickrPhotoCache(Context context) {
        // Get memory class of this device, exceeding this amount will throw OOM
        final int memClass = ((ActivityManager) context.getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE))
                .getMemoryClass();
        int lruCacheSize = 0;

        lruCacheSize = 1024 * 1024 * memClass / 4;

        mImageLruCache = new LruCache<String, Bitmap>(lruCacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };

        createCacheDirectory(context);
    }


    private void createCacheDirectory(Context context){
        // Directory to save cache directory
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            mCacheDir = new File(android.os.Environment.getExternalStorageDirectory(), "MemoryGame/Cache");
        }
        else {
            mCacheDir = context.getCacheDir();
        }

        // If cache directory is not found, create one
        if(!mCacheDir.exists()) {
            mCacheDir.mkdirs();
        }
    }

    // Applies Flickr Photo to imageview. Lazy loads if required.
    public void applyPhoto(ImageView imageView, FlickrPhoto photo) {
        if (mImageLruCache.get(photo.imageResourceLink) != null) {
            imageView.setImageBitmap(mImageLruCache.get(photo.imageResourceLink));
        }

        else {
            lazyLoad(imageView, photo);
        }
    }

    // Downloads flickr photo and stores it in memory and file cache
    public int downloadPhotos(ArrayList<FlickrPhoto> photos) {

        for (int i = 0; i < photos.size(); i++) {
            int retValue = downloadFlickrPhoto(photos.get(i));
            if (retValue == 1) {
                return 1;
            }
        }
        return 0;
    }


    // Downloads asynchronously and applies image to imageview
    public void lazyLoad(ImageView imageView, FlickrPhoto photo) {
        new PhotoLazyLoaderTask(imageView, photo).execute();
    }


    // Downloads flickr photo and stores it in memory and file cache
    private int downloadFlickrPhoto(FlickrPhoto photo) {
        Bitmap bitmap = getBitmap(photo.imageResourceLink);
        if (bitmap != null) {
            mImageLruCache.put(photo.imageResourceLink, bitmap);
            return 0;
        }
        return 1;
    }


    // Gets Bitmap by downloading it using HttpUrlConnection
    // And stores them in the cache directory
    private Bitmap getBitmap(String url) {


        if (mCacheDir != null && !mCacheDir.exists()) {
            return null;
        }

        //Hashcode of the url is used as the name of the file on SD card
        String filename = String.valueOf(url.hashCode());
        File f = new File(mCacheDir, filename);

        //from SD cache
        Bitmap b = decodeFile(f);
        if (b != null)
            return b;
        //from web
        try {
            Bitmap bitmap = null;

            URL urlObj;
            HttpURLConnection httpUrlConnection;
            urlObj = new URL(url);
            httpUrlConnection = (HttpURLConnection) urlObj.openConnection();
            httpUrlConnection.connect();
            int httpResponseCode = httpUrlConnection.getResponseCode();

            if (httpResponseCode == 200) {
                InputStream is = httpUrlConnection.getInputStream();
                OutputStream os = new FileOutputStream(f);
                copyStream(is, os);
                os.close();
                bitmap = decodeFile(f);
            }
            return bitmap;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }


    private void copyStream(InputStream inputStream, OutputStream outputStream){
        final int buffer_size = 1024;
        try
        {
            byte[] bytes = new byte[buffer_size];
            for(;;)
            {
                int count = inputStream.read(bytes, 0, buffer_size);
                if(count == -1)
                    break;
                outputStream.write(bytes, 0, count);
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }


    // decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f){
        try {

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            //Find the correct scale value.
            final int REQUIRED_SIZE = 256;
            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;

            while(true){
                if(width_tmp/2<REQUIRED_SIZE || height_tmp/2<REQUIRED_SIZE)
                    break;
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }

            o.inSampleSize = scale;
            o.inJustDecodeBounds = false;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Lazy loading images in case they are not found in local cache
     */
    private class PhotoLazyLoaderTask extends AsyncTask<Void, Void, Bitmap> {

        private Activity activity;
        private ImageView imageView;
        private FlickrPhoto photo;

        public PhotoLazyLoaderTask(ImageView imageView, FlickrPhoto photo) {
            this.activity = activity;
            this.imageView = imageView;
            this.photo = photo;

        }
        @Override
        protected Bitmap doInBackground(Void... voids) {
            Bitmap bitmap = getBitmap(photo.imageResourceLink);
            if (bitmap != null) {
                mImageLruCache.put(photo.imageResourceLink, bitmap);
            }
            return bitmap;
        }


        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}
