package com.sudarshanbhat.memorygame;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * Created by sudarshan on 8/21/14.
 * Grid's adapter that shows all the flickr photos
 * in 3x3 grid
 */
public class FlickrPhotoAdapter extends ArrayAdapter<FlickrPhoto> {

    private FlickrPhotoCache mDownloader;
    private int mTileSize;

    public FlickrPhotoAdapter(Context context, ArrayList<FlickrPhoto> photos, int tileSize) {
        super(context.getApplicationContext(), R.layout.grid_item_layout, photos);
        mDownloader = FlickrPhotoCache.getInstance(context.getApplicationContext());
        mTileSize = tileSize;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if (convertView == null) {
            vh = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.grid_item_layout, parent, false);
            vh.photoView = (ImageView) convertView.findViewById(R.id.photo_imageview);
            convertView.setTag(vh);
        }
        else {
            vh = (ViewHolder) convertView.getTag();
        }

        ViewGroup.LayoutParams params = vh.photoView.getLayoutParams();
        params.height = mTileSize;
        params.width = mTileSize;

        if (getItem(position).isHidden) {
            vh.photoView.setImageResource(R.drawable.ic_empty_grid_item);
        }
        else {
            mDownloader.applyPhoto(vh.photoView, getItem(position));
        }
        return convertView;
    }


    @Override
    public int getCount() {
        // Show only 9 tiles
        return Math.min(super.getCount(), GameFragment.MAX_TILES);
    }

    public static class ViewHolder {
        ImageView photoView;
    }
}
