package com.sudarshanbhat.memorygame;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Fragment;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by sudarshan on 8/21/14.
 */
public class GameFragment extends Fragment implements GameHelperFragment.FlickrPhotoDownloadListener, AdapterView.OnItemClickListener {

    // Constants
    public static final int MAX_TILES = 9;
    public static final int COUNTDOWN_TIME = 15; // seconds

    // Views
    private View mRootView;
    private GridView mMemoryGridView;
    private TextView mGameHelpTextView;
    private ImageView mQuizImageView;
    private ProgressBar mGridProgressBar;

    // Data and grid adapter
    private ArrayList<FlickrPhoto> mFlickrPhotos;
    private FlickrPhotoAdapter mFlickrPhotoAdapter;

    // FlickrPhotos are cached.
    private FlickrPhotoCache mDownloader;

    // Position of the photo currently being quizzed
    private int mQuizPhotoPosition = -1;
    private int mCountDownValue = 0;

    // Total number of wrong moves made
    private int mWrongMoveCount = 0;
    private long mTotalTimeTaken = 0;
    private boolean mResultsShown = false;

    // System/s animation duration time
    private int mShortAnimationDuration;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mDownloader = FlickrPhotoCache.getInstance(getActivity().getApplicationContext());
        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.game_fragment_layout, container, false);
        mMemoryGridView = (GridView) mRootView.findViewById(R.id.memory_grid);
        mGameHelpTextView = (TextView) mRootView.findViewById(R.id.game_help_text);
        mQuizImageView = (ImageView) mRootView.findViewById(R.id.quiz_imageview);
        mGridProgressBar = (ProgressBar) mRootView.findViewById(R.id.grid_progressbar);

        // Set size of the grid by getting the screen width. Important specially in landscape.
        int gridSize = getGridSize();
        ViewGroup.LayoutParams params = mMemoryGridView.getLayoutParams();
        params.width = gridSize;
        params.height = gridSize;
        mMemoryGridView.setLayoutParams(params);
        mMemoryGridView.setOnItemClickListener(this);

        // Restore instance state
        if (savedInstanceState == null) {
            mFlickrPhotos = new ArrayList<FlickrPhoto>();
        }
        else {
            mFlickrPhotos = savedInstanceState.getParcelableArrayList("flickr_photos");
            mQuizPhotoPosition = savedInstanceState.getInt("selected_photo_position");
            mCountDownValue = savedInstanceState.getInt("countdown_value");
            mTotalTimeTaken = savedInstanceState.getLong("total_time_taken");
            mWrongMoveCount = savedInstanceState.getInt("wrong_move_count");
            mResultsShown = savedInstanceState.getBoolean("results_shown");

            if(mQuizPhotoPosition != -1) {
                mDownloader.applyPhoto(mQuizImageView, mFlickrPhotos.get(mQuizPhotoPosition));
                showHelpText(R.string.help_text_quiz);
            }
            else if (mResultsShown) {
                showResults();
            }
            else {
                mQuizImageView.setVisibility(View.GONE);
                showCountDownText();
            }

            showLoadingIndicator(savedInstanceState.getBoolean("progress_bar_visibility"));
        }

        mFlickrPhotoAdapter = new FlickrPhotoAdapter(getActivity(), mFlickrPhotos, getTileSize());
        mMemoryGridView.setAdapter(mFlickrPhotoAdapter);

        // Set download response listener with helper fragment
        GameHelperFragment helperFragment =
                (GameHelperFragment) getFragmentManager()
                        .findFragmentByTag(GameHelperFragment.TAG);
        helperFragment.setFlickrPhotoDownloadListener(this);
        return mRootView;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.game, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new_game:
                startNewGame();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            startNewGame();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("flickr_photos", mFlickrPhotos);
        outState.putInt("selected_photo_position", mQuizPhotoPosition);
        outState.putInt("countdown_value", mCountDownValue);
        outState.putBoolean("progress_bar_visibility", mGridProgressBar.getVisibility() == View.VISIBLE);
        outState.putLong("total_time_taken", mTotalTimeTaken);
        outState.putInt("wrong_move_count", mWrongMoveCount);
        outState.putBoolean("results_shown", mResultsShown);
    }


    private void startNewGame() {
        mResultsShown = false;
        cancelTimer();
        hideFlickrPhoto();
        getHelperFragment().loadPhotos(); // Response of this is returned in onResponse() method here
        showLoadingIndicator(true);
    }

    @Override
    public void onResponse(ArrayList<FlickrPhoto> photos) {
        if (getActivity() == null) {
            return;
        }

        showLoadingIndicator(false);
        mRootView.setAlpha(0);
        mRootView.setVisibility(View.VISIBLE);
        mFlickrPhotos.clear();
        mFlickrPhotos.addAll(photos);
        mFlickrPhotoAdapter.notifyDataSetChanged();

        mRootView.animate().alpha(1f).setDuration(mShortAnimationDuration).setListener(null);
        startTimer();
    }


    @Override
    public void onError() {
        showLoadingIndicator(false);
        Toast.makeText(getActivity().getApplicationContext(), R.string.error_msg, Toast.LENGTH_SHORT).show();
    }

    // Timer is started as soon as all the photos are downloaded
    // and shown in the grid.
    private void startTimer() {
        getHelperFragment().startTimer();
    }

    // Cancels the timer.
    private void cancelTimer() { getHelperFragment().cancelTimer(); }


    @Override
    public void onTimerTick(int timeLeftInSecs) {
        mCountDownValue = timeLeftInSecs;
        showCountDownText();
    }


    @Override
    public void onTimerFinish() {
        mCountDownValue = 0;
        showCountDownText();
        startQuiz();
    }

    // Quiz is where user is asked to show the position for the photo
    private void startQuiz() {
        mQuizPhotoPosition = 0;
        mTotalTimeTaken = System.currentTimeMillis();
        mWrongMoveCount = 0;
        mRootView.animate().alpha(0.0f)
                .setDuration(mShortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        showHelpText(R.string.help_text_quiz);
                        hideAllTiles();
                        showRandomFlickrPhoto();
                        mRootView.animate()
                                .alpha(1f)
                                .setDuration(mShortAnimationDuration)
                                .setListener(null);
                    }
                });
    }


    private void endQuiz() {
        mQuizImageView.setVisibility(View.GONE);
        mQuizImageView.setImageResource(0);
        mQuizPhotoPosition = -1;
        mTotalTimeTaken = (System.currentTimeMillis() - mTotalTimeTaken)/1000;

        // Show results
        showResults();
    }


    // Shows a random photo from to user asking user to point the location
    private void showRandomFlickrPhoto() {
        Random random = new Random(System.currentTimeMillis());

        int randomValue = 0;
        do {
            randomValue = random.nextInt(MAX_TILES);
        } while (!mFlickrPhotos.get(randomValue).isHidden);

        FlickrPhoto photo = mFlickrPhotos.get(randomValue);
        ViewGroup.LayoutParams params = mQuizImageView.getLayoutParams();
        params.width = getTileSize();
        params.height = getTileSize();
        mQuizImageView.setLayoutParams(params);

        mQuizPhotoPosition = randomValue;
        mQuizImageView.setVisibility(View.VISIBLE);
        mDownloader.applyPhoto(mQuizImageView, photo);
    }


    private void hideFlickrPhoto() {
        mQuizPhotoPosition = -1;
        mQuizImageView.setVisibility(View.GONE);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long value) {

        if (position == mQuizPhotoPosition) {
            showTile(position);
            for (int i = 0; i < Math.min(MAX_TILES, mFlickrPhotos.size()); i++) {
                if (mFlickrPhotos.get(i).isHidden) {
                    showRandomFlickrPhoto();
                    return;
                }
            }
            endQuiz();
        }
        else {
            mWrongMoveCount ++;
        }
    }


    private void showTile(int position) {
        mFlickrPhotos.get(position).isHidden = false;
        mFlickrPhotoAdapter.notifyDataSetChanged();
    }

    private void hideAllTiles() {
        for (int i = 0; i < mFlickrPhotos.size(); i++) {
            mFlickrPhotos.get(i).isHidden = true;
        }
        mFlickrPhotoAdapter.notifyDataSetChanged();
    }


    private int getGridSize() {
        return (int) ((3 * getTileSize()) + (2 * getResources().getDimension(R.dimen.grid_spacing)));
    }


    // Grid is square, tile is square. So it's calculated in runtime
    private int getTileSize() {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        int shortestAvailableScreenWidth = Math.min(width, height);
        int screenHeight = height;
        int actionBarHeight = 0;

        // Calculate ActionBar height
        TypedValue tv = new TypedValue();
        if (getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,
                    getResources().getDisplayMetrics());
        }

        // for landscape
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            shortestAvailableScreenWidth -= (actionBarHeight + 100); // Extra padding
        }
        else {
            screenHeight -= (actionBarHeight + 100); // Extra padding

            // Grid should occupy only 65% of the available screen.
            // Remaining 30% is for the help text and quiz imageview
            int gridSize =  (int) (6.5f * screenHeight) / 10;

            shortestAvailableScreenWidth = Math.min(gridSize, shortestAvailableScreenWidth);
        }

        int tileSize = (int) ((shortestAvailableScreenWidth - (getResources().getDimension(R.dimen.activity_horizontal_margin) * 2)) / 3);
        return tileSize;
    }


    // Shows count down when grid is loaded
    private void showCountDownText() {
        if (getActivity() == null) {
            return;
        }

        if (mCountDownValue == 0) {
            mGameHelpTextView.setText("");
            return;
        }

        mGameHelpTextView.setText(Html.fromHtml(getString(R.string.help_text_remember, mCountDownValue)));
    }


    // For various help text
    private void showHelpText(int resId) {
        if (resId == 0) {
            mGameHelpTextView.setText("");
            return;
        }
        mGameHelpTextView.setText(resId);
    }


    private void showResults() {
        mResultsShown = true;
        mGameHelpTextView.setText(getString(R.string.help_text_result, mTotalTimeTaken, mWrongMoveCount));
    }


    private void showLoadingIndicator(boolean flag) {

        if (flag) {
            mGridProgressBar.setVisibility(View.VISIBLE);
        }
        else {
            mGridProgressBar.setVisibility(View.GONE);
        }
    }

    private GameHelperFragment getHelperFragment() {
        return (GameHelperFragment) getFragmentManager()
                .findFragmentByTag(GameHelperFragment.TAG);
    }
}
