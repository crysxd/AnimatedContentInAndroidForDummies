package crysxd.de.animatedcontentinandroidfordummies;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;

/**
 * A video view for passive background videos
 */
public class PassiveVideoView extends FrameLayout implements MediaController.MediaPlayerControl, TextureView.SurfaceTextureListener {

    /**
     * The tag for logs
     */
    private static final String TAG = "PassiveVideoView";

    /**
     * The {@link MediaPlayer} playing the video
     */
    private MediaPlayer mMediaPlayer;

    /**
     * The {@link Uri} to the video
     */
    private Uri mVideoUri;

    /**
     * The {@link MediaPlayer.OnPreparedListener} starting the video when it is prepared
     */
    private MediaPlayer.OnPreparedListener mOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            start();

        }
    };

    /**
     * The {@link MediaPlayer.OnCompletionListener} which releases the {@link MediaPlayer} when payback is done
     */
    private MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.release();

        }
    };

    /**
     * The {@link ImageView} showing a fallback image
     */
    private ImageView mFallbackView;

    /**
     * The {@link ImageView} showing a loading image
     */
    private ImageView mLoadingView;

    /**
     * The {@link TextureView} displaying the video
     */
    private TextureView mTextureView;

    /**
     * The {@link MediaPlayer.OnErrorListener}. This listener will hide the video if an error occurs
     * to unveil the fallback image
     */
    private MediaPlayer.OnErrorListener mOnErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
            // Hide the texture view to unveil the fallback image
            mTextureView.setVisibility(GONE);
            mFallbackView.animate().alpha(1f).start();

            // Return true, we handles the error properly
            return true;

        }
    };

    /**
     * Creates a new instance
     *
     * @param context a {@link Context}
     */
    public PassiveVideoView(Context context) {
        super(context);
        init(null, 0);
    }

    /**
     * Creates a new instance
     *
     * @param context a {@link Context}
     * @param attrs   The attributes from the XML layout
     */
    public PassiveVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    /**
     * Creates a new instance
     *
     * @param context      a {@link Context}
     * @param attrs        The attributes from the XML layout
     * @param defStyleAttr The resource for the default sytle
     */
    public PassiveVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    /**
     * Initialises the view
     * @param attrs        The attributes from the XML layout
     * @param defStyleAttr The resource for the default sytle
     */
    private void init(@Nullable AttributeSet attrs, int defStyleAttr) {
        // Create views
        mLoadingView = new ImageView(getContext());
        mTextureView = new TextureView(getContext());
        mTextureView.setOpaque(false);
        mTextureView.setAlpha(0f);
        mFallbackView = new ImageView(getContext());
        mFallbackView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mFallbackView.setAlpha(0f);

        // Add them. Fallback will be beneath the texture view
        addView(mLoadingView);
        addView(mFallbackView);
        addView(mTextureView);

        // Read attributes and set them
        if (attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.PassiveVideoView, defStyleAttr, 0);
            int video = a.getResourceId(R.styleable.PassiveVideoView_video, 0);
            int fallback = a.getResourceId(R.styleable.PassiveVideoView_fallbackImage, 0);
            int loading = a.getResourceId(R.styleable.PassiveVideoView_loadingImage, 0);

            if (video != 0 && fallback != 0 && loading != 0) {
                setVideoResource(video, fallback, loading);

            } else if (video != 0 || fallback != 0 || loading != 0) {
                Log.e(TAG, "Video resource, loading or fallback not set but all required!");

            }
        }
    }

    /**
     * Prepares the view
     *
     * @param a the hosting {@link Activity}
     */
    public void onCreate(Activity a) {
        // Enable hardware acceleration for window
        a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        // Add it and set this as listener
        mTextureView.setSurfaceTextureListener(this);

        // If the surface texture is already available, directly call handler method
        // Listener won't work...
        if (mTextureView.isAvailable()) {
            onSurfaceTextureAvailable(mTextureView.getSurfaceTexture(), 0, 0);

        }
    }

    /**
     * Releases the media player
     */
    public void onDestroy() {
        try {
            mMediaPlayer.release();
        } catch (Exception e) {
            Log.e(TAG, "Error while releasing MediaPlayer", e);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float aspectRatio;

        try {
            // Calc aspect ratio based on video
            aspectRatio = this.mMediaPlayer.getVideoHeight() / (float) this.mMediaPlayer.getVideoWidth();

        } catch (Exception e1) {
            Log.e(TAG, "Error while calculating aspect ratio based on video", e1);

            Drawable d = this.mFallbackView.getDrawable();
            aspectRatio = d != null ? d.getIntrinsicHeight() / (float) d.getIntrinsicWidth() : 9 / 16f;
        }

        // Get width, calc height
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (width * aspectRatio);

        // Call super with the exact measurement
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );
    }

    /**
     * Sets the {@link Uri} of the video to be played. This method must be called before starting the
     * first layout pass! Calls afterwards will have no effect.
     *
     * @param uri      the {@link Uri}
     * @param fallback the fallback shown if video playback fails
     * @param loading the image shown while the video is loaded
     */
    public void setVideoUri(@NonNull Uri uri, @DrawableRes int fallback, @DrawableRes int loading) {
        mVideoUri = uri;
        mFallbackView.setImageResource(fallback);
        mLoadingView.setImageResource(loading);

    }

    /**
     * Sets the {@link RawRes} of the video to be played. This method must be called before starting the
     * first layout pass! Calls afterwards will have no effect.
     *
     * @param resource the {@link RawRes}
     * @param fallback the fallback shown if video playback fails
     * @param loading the image shown while the video is loaded
     */
    public void setVideoResource(@RawRes int resource, @DrawableRes int fallback, @DrawableRes int loading) {
        setVideoUri(Uri.parse("android.resource://" + getContext().getPackageName() + "/" + resource), fallback, loading);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        try {
            // Creating MediaPlayer, the surface is ready!
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(getContext(), mVideoUri);
            mMediaPlayer.setSurface(new Surface(mTextureView.getSurfaceTexture()));
            mMediaPlayer.setLooping(true);
            mMediaPlayer.setOnErrorListener(mOnErrorListener);
            mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
            mMediaPlayer.setOnCompletionListener(mOnCompletionListener);

            // Load the video async. Listener will start playback as soon as the video is ready
            mMediaPlayer.prepareAsync();
        } catch (Exception e) {
            mOnErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        // Nothing to do
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        // Nothing to do
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        // Nothing to do
    }

    @Override
    public void start() {
        try {
            // We have to delay the animation for 200ms as the first couple of frames are all black
            mMediaPlayer.start();
            mTextureView.animate().alpha(1f).setStartDelay(200).start();
            requestLayout();
        } catch (Exception e) {
            Log.e(TAG, "Error while starting playback", e);
        }
    }

    @Override
    public void pause() {
        try {
            mMediaPlayer.pause();
        } catch (Exception e) {
            Log.e(TAG, "Error while pausing playback", e);
        }
    }

    @Override
    public int getDuration() {
        try {
            return mMediaPlayer.getDuration();
        } catch (Exception e) {
            Log.e(TAG, "Error while getting duration", e);
            return 0;
        }
    }

    @Override
    public int getCurrentPosition() {
        try {
            return mMediaPlayer.getCurrentPosition();
        } catch (Exception e) {
            Log.e(TAG, "Error while getting current position", e);
            return 0;
        }
    }

    @Override
    public void seekTo(int i) {
        try {
            mMediaPlayer.seekTo(i);
        } catch (Exception e) {
            Log.e(TAG, "Error while seeking to position", e);
        }
    }

    @Override
    public boolean isPlaying() {
        try {
            return mMediaPlayer.isPlaying();
        } catch (Exception e) {
            Log.e(TAG, "Error while determining play state", e);
            return false;
        }
    }

    @Override
    public int getBufferPercentage() {
        Log.w(TAG, "Access to stub method getBufferPercentage()");
        return 0;
    }

    @Override
    public boolean canPause() {
        Log.w(TAG, "Access to stub method canPause()");
        return false;
    }

    @Override
    public boolean canSeekBackward() {
        Log.w(TAG, "Access to stub method canSeekBackward()");
        return false;
    }

    @Override
    public boolean canSeekForward() {
        Log.w(TAG, "Access to stub method canSeekForward()");
        return false;
    }

    @Override
    public int getAudioSessionId() {
        try {
            return mMediaPlayer.getAudioSessionId();
        } catch (Exception e) {
            Log.e(TAG, "Error while getting udi session id", e);
            return 0;
        }
    }
}
