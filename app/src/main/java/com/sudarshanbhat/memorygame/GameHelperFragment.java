package com.sudarshanbhat.memorygame;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by sudarshan on 8/21/14.
 */
public class GameHelperFragment extends Fragment {
    public static final String TAG = "com.sudarshanbhat.memorygame.GAME_HELPER_FRAGMENT";

    public static final String API_URL = "https://api.flickr.com/services/feeds/photos_public.gne?format=json&nojsoncallback=1";

    public interface FlickrPhotoDownloadListener {
        public void onResponse(ArrayList<FlickrPhoto> photos);
        public void onError();
        public void onTimerTick(int timeLeftInSecs);
        public void onTimerFinish();
    }

    private CountDownTimer mCountDownTimer;
    private FlickrFeedFetchTask mFeedFetchTask;
    private FlickrPhotoDownloadListener mListener;

    FlickrPhotoCache mDownloader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDownloader = FlickrPhotoCache.getInstance(getActivity().getApplicationContext());

        // Headless fragment. Do not want it to be recreated on orientation change.
        // This fragment only helps in downloading photos from Flickr.
        setRetainInstance(true);
    }


    public void setFlickrPhotoDownloadListener(FlickrPhotoDownloadListener listener) {
        mListener = listener;
    }


    public void loadPhotos() {
        if (mFeedFetchTask != null &&
                (mFeedFetchTask.getStatus() == AsyncTask.Status.PENDING
                        || mFeedFetchTask.getStatus() == AsyncTask.Status.RUNNING)) {
            mFeedFetchTask.cancel(true);
        }
        mFeedFetchTask = new FlickrFeedFetchTask();
        mFeedFetchTask.execute();
    }

    public void startTimer() {
        mCountDownTimer = new CountDownTimer(GameFragment.COUNTDOWN_TIME * 1000, 1000) {
            @Override
            public void onTick(long l) {
                mListener.onTimerTick((int) l / 1000);
            }

            @Override
            public void onFinish() {
                mListener.onTimerFinish();
            }
        };
        mCountDownTimer.start();
    }


    public void cancelTimer() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }


    private String getFlickrApiResponseInJsonFormat() throws IOException, JSONException {
        URL apiUrl = new URL(API_URL);
        HttpURLConnection apiConnection = (HttpURLConnection) apiUrl.openConnection();
        apiConnection.setRequestMethod("POST");

        apiConnection.connect();

        int responseCode = apiConnection.getResponseCode();

        BufferedInputStream bis = new BufferedInputStream(apiConnection.getInputStream());
        apiConnection.getInputStream();

        ByteArrayBuffer baf = new ByteArrayBuffer(50);
        int totalRead = 0;
        int read = 0;
        int bufSize = 1024;
        byte[] buffer = new byte[bufSize];

        while (true) {
            read = bis.read(buffer);

            if (read == -1)
                break;

            totalRead += read;
            baf.append(buffer, 0, read);
        }

        byte[] bytes = baf.toByteArray();
        baf = null;
        apiConnection.disconnect();

        String response = new String(bytes);
        return response;
    }


    private ArrayList<FlickrPhoto> parse(String jsonResponse) throws JSONException {
        ArrayList<FlickrPhoto> photos = new ArrayList<FlickrPhoto>();

        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONArray items = jsonObject.getJSONArray("items");
        for (int i = 0; i < Math.min(items.length(), 9); i++) {
            photos.add(new FlickrPhoto(items.optJSONObject(i)));
        }
        return photos;
    }


    private class FlickrFeedFetchTask extends AsyncTask<Void, Void, ArrayList<FlickrPhoto>> {

        @Override
        protected ArrayList<FlickrPhoto> doInBackground(Void... voids) {
            try {
                String jsonResponse = getFlickrApiResponseInJsonFormat();
                ArrayList<FlickrPhoto> photos = parse(jsonResponse);
                mDownloader.downloadPhotos(photos);
                return photos;
            }

            catch (JSONException jsonException) {
                jsonException.printStackTrace();
            }
            catch (IOException exception) {
                exception.printStackTrace();
            }

            return null;
        }


        @Override
        protected void onPostExecute(ArrayList<FlickrPhoto> photos) {
            super.onPostExecute(photos);

            if (mListener != null) {
                mListener.onResponse(photos);
            }
            else {
                mListener.onError();
            }
        }
    }
}
