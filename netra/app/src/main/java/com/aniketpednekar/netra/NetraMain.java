package com.aniketpednekar.netra;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.FileOutputStream;
import java.util.Locale;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class NetraMain extends AppCompatActivity implements SurfaceHolder.Callback, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener{

    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;

    Camera.PictureCallback rawCallback;
    Camera.PictureCallback jpegCallback;
    Camera.ShutterCallback shutterCallback;

    ToggleButton modeToggle, volumeToggle, textToggle;
    TextToSpeech t1;
    AudioManager audioManager;
    TextView captionTextView;
    boolean isSpeaking = false;
    boolean algomusMode = false;
    boolean showText = true;

    int mute = 0;
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;

    //private View mContentView;
    //private View mControlsView;
    private boolean mVisible;
    
    private static final String DEBUG_TAG = "Gestures";
    private GestureDetectorCompat mDetector;

    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    public void setOutput(String caption){
        TextView captionText = (TextView) findViewById(R.id.captiontext);
        captionText.setText(caption);
        captionText.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestInternetPermissions();
        //requestWritePermissions();
        requestCameraPermissions();

        setContentView(R.layout.activity_netra_main);

        captionTextView = (TextView) findViewById(R.id.captiontext);
        captionTextView.setVisibility(View.INVISIBLE);




        surfaceView = (SurfaceView) findViewById(R.id.surfaceView );
        surfaceHolder = surfaceView.getHolder();

        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        jpegCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                FileOutputStream outputStream = null;
                try {
                    if(algomusMode)
                        getAlgomus(data);
                    else
                        getClarifai(data);
                    //Toast.makeText(getApplicationContext(),getImgurUrl(data),Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }

//                try {
//                    File path = new File(Environment.getExternalStorageDirectory().getPath() + "/netra/");
//                    if (!path.exists()) path.mkdirs();
//                    File cameraFile = new File(path, "netra_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
//                    /*
//                    outputStream = new FileOutputStream(cameraFile);
//                    outputStream.write(data);
//                    outputStream.close();
//                    */
//                    outputStream = new FileOutputStream(cameraFile);
//
//                    //bitmap = rotateImage(bitmap, 90);
//
//                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//                    byte[] rotatedData = stream.toByteArray();
//                    Toast.makeText(getApplicationContext(),getAlgomusDescription(rotatedData),Toast.LENGTH_SHORT).show();
//                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
//                    outputStream.flush();
//                    outputStream.close();
//
//                    Toast.makeText(getApplicationContext(),"Pic saved",Toast.LENGTH_SHORT).show();
//                } catch (Exception e){
//                    Toast.makeText(getApplicationContext(),"Pic not saved " + e.getMessage(),Toast.LENGTH_SHORT).show();
//                }/* catch (IOException e){
//                    Toast.makeText(getApplicationContext(),"Pic not saved " + e.getMessage(),Toast.LENGTH_SHORT).show();
//                }*/
//                //Toast.makeText(getApplicationContext(),"Pic saved",Toast.LENGTH_SHORT).show();
                refreshCamera();

            }
        };

        mVisible = true;
        //mControlsView = findViewById(R.id.fullscreen_content_controls);
        //mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        /*mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });*/

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
        
        mDetector = new GestureDetectorCompat(this,this);
        mDetector.setOnDoubleTapListener(this);

        //AudioManager
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        if(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)==0){
            mute = 1;
        }else{
            mute = 0;
        }

        t1=new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = t1.setLanguage(Locale.US);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "This Language is not supported");
                        Intent installIntent = new Intent();
                        installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                        startActivity(installIntent);
                    } else {
                        t1.speak("Welcome to Netra", TextToSpeech.QUEUE_FLUSH, null);
                    }
                        t1.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onDone(String utteranceId) {
                                Log.d("TAG", "utterance complete");
                                isSpeaking = false;
                                //captionTextView.setVisibility(View.INVISIBLE);
                            }

                            @Override
                            public void onError(String utteranceId) {
                            }

                            @Override
                            public void onStart(String utteranceId) {
                                Log.d("Utterance started", "Utterance started");
                            }
                        });
                    } else {
                        Log.e("MainActivity", "Initilization Failed!");
                    }




            }
        });

        //t1.setOnUtteranceProgressListener(mProgressListener);



        //Handling Toggle Buttons
        modeToggle = (ToggleButton)findViewById(R.id.modetoggle);
        volumeToggle = (ToggleButton)findViewById(R.id.volumetoggle);
        textToggle = (ToggleButton)findViewById(R.id.texttoggle);

        modeToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (!isChecked) {
                    algomusMode = true;
                    Log.d("ALGOMUS MODE", String.valueOf(algomusMode));
                } else {
                    algomusMode = false;
                    Log.d("ALGOMUS MODE", String.valueOf(algomusMode));
                }
            }
        });

        textToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (!isChecked) {
                    showText = true;
                    //captionTextView.setVisibility(View.VISIBLE);
                } else {
                    //showText = false;
                    showText = true;
                    //captionTextView.setVisibility(View.INVISIBLE);
                }
            }
        });

        volumeToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                    } else {
                        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                    }

                }else{

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 1);
                    } else {
                        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                    }

                }
            }

        });

    }

    public void requestWritePermissions(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(getApplicationContext(),"Requesting Write Permissions", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
            }
        }
    }

    public void requestCameraPermissions(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                Toast.makeText(getApplicationContext(),"Requesting Write Permissions", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        0);
            }
        }
    }

    public void requestInternetPermissions(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.INTERNET)) {
                Toast.makeText(getApplicationContext(),"Requesting Write Permissions", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.INTERNET},
                        2);
            }
        }
    }


    public void refreshCamera(){
        if (surfaceHolder.getSurface()==null){
            return;
        }

        try{
            camera.stopPreview();
        }
        catch (Exception e){
        }

        try{
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        }
        catch (Exception e){
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    /*
    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }*/

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        //mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            /*mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);*/
        }
    };

    /*
    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }*/

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            //mControlsView.setVisibility(View.VISIBLE);
        }
    };

    private final Handler mHideHandler = new Handler();
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Log.d("SURFACE CREATED", "SURFACE CREATED");
        try{

            if(camera!=null) {
                Log.d("CAMERA NULL","NULL");
                camera.release();
                Log.d("CAMERA RELEASED","CAMERA RELEASED");
            }
            camera = Camera.open();

            //camera.setDisplayOrientation(90);

            //camera.setDisplayOrientation(getRotationAngle(0, camera));
        }
        catch (Exception e){
            Log.d("CAMERA NOT CREATED",e.getMessage());
            return;
        }

        Camera.Parameters params = camera.getParameters();
        camera.setParameters(params);

        try{
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        }
        catch (Exception e){
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        camera.stopPreview();
        camera.release();
        camera = null;

    }

    public int getRotationAngle(
            int cameraId, android.hardware.Camera camera){
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = this.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        camera.setDisplayOrientation(90);
        //camera.setDisplayOrientation(getRotationAngle(0, camera));
    }
    
     @Override
    public boolean onTouchEvent(MotionEvent event){
        this.mDetector.onTouchEvent(event);
        // Be sure to call the superclass implementation
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDown: " + event.toString());
        return true;
    }


    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        boolean result = false;
        try {
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        Log.d(DEBUG_TAG, "Left to Right swipe performed");
                        onSwipeRight();
                    } else {
                        Log.d(DEBUG_TAG, "Right to Left swipe performed");
                        onSwipeLeft();
                    }
                }
                result = true;
            }
            else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) {
                    Log.d(DEBUG_TAG, "Top to bottom swipe performed");
                    onSwipeBottom();
                } else {
                    Log.d(DEBUG_TAG, "Bottom to Top swipe performed");
                    onSwipeTop();
                }
            }
            result = true;

        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return result;
    }


    public static Bitmap rotateImage(Bitmap source, float angle) {
        Bitmap retVal;

        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        retVal = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);

        return retVal;
    }

    public void getAlgomus(byte[] image)
            throws Exception {
        new RetrieveCaption(getApplicationContext(), captionTextView, t1, showText).execute(image);

        //return "temp";
    }

    public void getClarifai(byte[] image)
            throws Exception {
        new RetrieveTags(getApplicationContext(), captionTextView, t1, isSpeaking, showText).execute(image);

    }
    @Override
    public void onLongPress(MotionEvent event) {
        Log.d(DEBUG_TAG, "onLongPress: " + event.toString());
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        Log.d(DEBUG_TAG, "onScroll: " + e1.toString() + e2.toString());
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        Log.d(DEBUG_TAG, "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString());
        camera.takePicture(null, null, jpegCallback);
        Log.d("Algomus", String.valueOf(algomusMode));

        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDoubleTapEvent: " + event.toString());
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());
        return true;
    }

    public void onPause(){
        if(t1 !=null){
            t1.stop();
            t1.shutdown();
        }
        super.onPause();
    }

    public void onSwipeRight() {
    }

    public void onSwipeLeft() {
    }

    public void onSwipeTop() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }

    public void onSwipeBottom() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);

    }

    public void changeMode(){
        //TODO: use this to changeMode - call appropriately from both swipeLeft/swipeRight or ToggleButton onCheckChanged
    }

    private UtteranceProgressListener mProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
            Log.d("TAG", "utterance started");
        } // Do nothing

        @Override
        public void onError(String utteranceId) {
            Log.d("TAG", "utterance error");
        } // Do nothing.

        @Override
        public void onDone(String utteranceId) {
            Log.d("TAG", "utterance complete");
            isSpeaking = false;
        }
    };
}
