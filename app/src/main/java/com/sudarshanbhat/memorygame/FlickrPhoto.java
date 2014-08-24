package com.sudarshanbhat.memorygame;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

/**
 * Created by sudarshan on 8/21/14.
 * Class that holds each Flickr Photo object details
 * after parsing from the API's json response
 */
public class FlickrPhoto implements Parcelable {

    public final String imageTitle;
    public final String imageLink;
    public final String imageResourceLink;
    public boolean isHidden = false;

    public FlickrPhoto(JSONObject json) {
        imageTitle = json.optString("title");
        imageLink = json.optString("link");

        imageResourceLink = json.optJSONObject("media").optString("m");
    }


    public FlickrPhoto(Parcel parcel) {
        imageTitle = parcel.readString();
        imageLink = parcel.readString();
        imageResourceLink = parcel.readString();
        isHidden = parcel.readInt() == 1;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(imageTitle);
        parcel.writeString(imageLink);
        parcel.writeString(imageResourceLink);
        parcel.writeInt(isHidden ? 1 : 0);
    }


    @Override
    public int describeContents() {
        return 0;
    }


    public static final Creator<FlickrPhoto> CREATOR = new Creator<FlickrPhoto>() {
        @Override
        public FlickrPhoto[] newArray(int i) {
            return new FlickrPhoto[0];
        }


        @Override
        public FlickrPhoto createFromParcel(Parcel parcel) {
            return new FlickrPhoto(parcel);
        }
    };
}
