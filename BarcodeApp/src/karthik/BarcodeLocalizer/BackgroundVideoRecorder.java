package karthik.BarcodeLocalizer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

public class BackgroundVideoRecorder extends Service implements SurfaceHolder.Callback {

	private WindowManager windowManager;
    private SurfaceView surfaceView;
    private Camera camera = null;
    private MediaRecorder mediaRecorder = null;
    private SurfaceHolder surfHolder = null;
    
    // flag to control if we use mobile data to upload
    private boolean WifiUploadOnly = true; 
    private static boolean RECORD_AUDIO = false;
    
    private int NOTIFICATION_ID = 111;
    private static final String TAG = "PicasaBackgroundVideoRecorder";
    
    public static final String REQUEST_TYPE = "REQUEST_TYPE";
    public static final String REQUEST_TYPE_STOP = "STOP_RECORDING";
    public static final String REQUEST_TYPE_START = "START_RECORDING";
    public static final String REQUEST_TYPE_PAUSE = "PAUSE_RECORDING";
    public static final String REQUEST_TYPE_PLAY = "RESTART_RECORDING";
    
    private String mEmail;
    private String token;
    private String outputFile;
  
    private Timer uploadTimer;
    private long UPLOAD_INTERVAL = 60000;
    
    @Override
    public void onCreate() {
    	
        
        // Create new SurfaceView, set its size to 1x1, move it to the top left corner and set this service as a callback
        windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        surfaceView = new SurfaceView(this);
        LayoutParams layoutParams = new WindowManager.LayoutParams(
            1, 1,
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        windowManager.addView(surfaceView, layoutParams);
        surfaceView.getHolder().addCallback(this);
    }

    public int onStartCommand(Intent intent, int flags, int startID){
    	// gets email and access token from calling activity
    	mEmail = intent.getStringExtra(LoginActivity.EMAIL_MSG);
    	token = intent.getStringExtra(LoginActivity.TOKEN_MSG);
    	String requestType = intent.getStringExtra(REQUEST_TYPE);
    	
    	Log.v(TAG, "onStartCommand received request " + requestType);

    	if(requestType.equals(REQUEST_TYPE_START)){
        	Log.i(TAG, "Started service for user " + mEmail);
        	createStopPauseNotification();
        	// this is called only on the first creation of this class
        	// so onSurfaceCreated starts the video recording
        	startUploadTimer(UPLOAD_INTERVAL);
    	}
    	
    	if (requestType.equals(REQUEST_TYPE_PAUSE)){
        	Log.i(TAG, "Paused recording for user " + mEmail);
        	mediaRecorder.stop();
        	releaseMediaRecorder();
        	createStopPlayNotification();
        	// upload video
        	uploadTimer.cancel();
        	uploadVideo();
    	}
    	    	
    	if (requestType.equals(REQUEST_TYPE_PLAY)){
        	Log.i(TAG, "Restarted recording for user " + mEmail);
        	createStopPauseNotification();
        	startRecordingVideo(surfHolder);
        	startUploadTimer(UPLOAD_INTERVAL);
    	}

    	if (requestType.equals(REQUEST_TYPE_STOP)){
        	Log.i(TAG, "Stopped service for user " + mEmail);
        	uploadTimer.cancel();
           	uploadVideo();
            stopSelf();
    	}
    	
    	return START_STICKY;
    }

	/**
	 * 
	 */
	private void uploadVideo() {
		new ImageUploader(this, mEmail, new File(outputFile), token, null, WifiUploadOnly).execute();
	}
  
    
    private void createStopPauseNotification() {
    	
    	PendingIntent stopIntent = PendingIntent.getService(this, 0, getIntent(REQUEST_TYPE_STOP), PendingIntent.FLAG_CANCEL_CURRENT);    	    
    	PendingIntent pauseIntent = PendingIntent.getService(this, 1, getIntent(REQUEST_TYPE_PAUSE),  PendingIntent.FLAG_CANCEL_CURRENT);

    	Log.v(TAG, "Created intents");
    	// Start foreground service to avoid unexpected kill
        Notification notification = new Notification.Builder(this)
            .setContentTitle("Helios Background Video Recorder")
            .setContentText("")
            .setSmallIcon(R.drawable.eye)
            .addAction(R.drawable.pause, "Pause", pauseIntent)
            .addAction(R.drawable.stop, "Stop", stopIntent)
            .build();
        Log.v(TAG, "Created notification");
        startForeground(NOTIFICATION_ID, notification);
    }

    private void createStopPlayNotification() {
    	
    	PendingIntent stopIntent = PendingIntent.getService(this, 0, getIntent(REQUEST_TYPE_STOP), PendingIntent.FLAG_CANCEL_CURRENT);    	    
    	PendingIntent playIntent = PendingIntent.getService(this, 2, getIntent(REQUEST_TYPE_PLAY),  PendingIntent.FLAG_CANCEL_CURRENT);

    	Log.v(TAG, "Created stop and play intents");
    	// Start foreground service to avoid unexpected kill
        Notification notification = new Notification.Builder(this)
            .setContentTitle("Helios Background Video Recorder")
            .setContentText("")
            .setSmallIcon(R.drawable.eye)
            .addAction(R.drawable.play, "Play", playIntent)
            .addAction(R.drawable.stop, "Stop", stopIntent)
            .build();
        Log.v(TAG, "Created stop and play notification");
        startForeground(NOTIFICATION_ID, notification);
    }

    private Intent getIntent(String requestType){
    	Intent intent = new Intent(this, BackgroundVideoRecorder.class);
    	intent.putExtra(REQUEST_TYPE, requestType);
		intent.putExtra(LoginActivity.TOKEN_MSG, token);
		intent.putExtra(LoginActivity.EMAIL_MSG, mEmail);

		Log.v(TAG, "Created " + requestType + " intent for " + mEmail);
    	return intent;
    	
    }

    // Method called right after Surface created (initializing and starting MediaRecorder)
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
    	surfHolder = surfaceHolder;
        startRecordingVideo(surfHolder);        
    }

	private void startRecordingVideo(SurfaceHolder surfaceHolder) {
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
        .format(new java.util.Date().getTime());

    	outputFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
    			+ File.separator + "Helios_" + timeStamp + ".mp4";

    	camera = getCameraInstance();
    	if(camera == null){
    		Toast.makeText(this, "Camera unavailable or in use", Toast.LENGTH_LONG).show();
    		createStopPlayNotification();
    		return;
    	}
        mediaRecorder = new MediaRecorder();
        camera.unlock();

        mediaRecorder.setCamera(camera);
        mediaRecorder.setOrientationHint(90);
  
        if(RECORD_AUDIO){
        	mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        	mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        
        	mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        }else{
	        int targetFrameRate = 15;
	        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
	        
        	mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
			mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
			mediaRecorder.setVideoFrameRate(targetFrameRate);
			mediaRecorder.setVideoSize(profile.videoFrameWidth,
					profile.videoFrameHeight);
			mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
			mediaRecorder.setVideoEncoder(profile.videoCodec);
	    }

        mediaRecorder.setOutputFile(outputFile);
        Log.d(TAG, "Saving file to " + outputFile);
        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
        mediaRecorder.setMaxDuration(-1);
        
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
        }
	}

    // Stop recording and remove SurfaceView
    @Override
    public void onDestroy() {
    	
    	Log.v(TAG, "BackgroundVideoRecorder Service is being destroyed");
        mediaRecorder.stop();
        releaseMediaRecorder();
        
        windowManager.removeView(surfaceView);
    }

    private void releaseMediaRecorder(){
    	// release MediaRecorder object when not needed
        mediaRecorder.release();

        camera.lock();
        camera.release();    	
    }
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {}

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        	Log.d(TAG, "Error opening camera");
        }
        return c; // returns null if camera is unavailable
    }
    
	private void startUploadTimer(long interval) {
		// set up timer to upload video every <interval> milliseconds
		// this is so that we don't breach file size limits on Picasa
		uploadTimer = new Timer();
		uploadTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				// first stop recording
	        	mediaRecorder.stop();
	        	releaseMediaRecorder();
	        	// then upload video in background AsyncTask
	        	uploadVideo();
	        	// now reinitialize MediaRecorder and Camera and start recording video again
	        	startRecordingVideo(surfHolder);
			}
		}, interval, interval);
	}
}