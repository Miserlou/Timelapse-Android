package pro.dbro.timelapse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import pro.dbro.timelapse.R.id;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class CameraActivity extends Activity {
	private Camera mCamera;
	private static CameraPreview mCameraPreview;
	private static TimeLapseApplication c;
	private int timelapse_id;
	
	// Determine whether or not to update ListView onPause
	public static Boolean picture_taken;
	
	// ImageView overlayed on the Camera preview
	private static ImageView cameraOverlay;
	
	// TAG to associate with all debug logs originating from this class
	private static final String TAG = "TimeLapseActivity";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.camera);
        
        // Store context for use by static methods
        c = (TimeLapseApplication)getApplicationContext();
        
        cameraOverlay = (ImageView) findViewById(id.camera_overlay);
        cameraOverlay.setAlpha(100);

        // Obtain camera instance
        mCamera = getCameraInstance();
        
        picture_taken = false;
        
        if (mCamera == null){
        	showCameraErrorDialog();
        }
        else{
        	// Camera is available. Onward Ho!
        	
        	// Assign Camera parameters
        	setupCamera();
	        // Obtain SurfaceView for displaying camera preview
	        mCameraPreview = new CameraPreview(this, mCamera);
	        FrameLayout preview = (FrameLayout) findViewById(id.camera_preview);
	        preview.addView(mCameraPreview);
	        
	        // Set shutter touch listener to layout
	        RelativeLayout container = (RelativeLayout) findViewById(id.container_layout);
	        container.setOnTouchListener(shutterListener);
        }
       
    }
    /** End OnCreate() */
    
    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        	Log.d("getCameraInstance",e.toString());
        }
        return c; // returns null if camera is unavailable
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	
    	// Release camera for other applications
    	releaseCamera();
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	// If there is no camera instance, create one
    	if(mCamera == null){
    		Log.d("OnResume","mCamera null");
    		mCamera = getCameraInstance();
    	}
    	else{
    		Log.d("OnResume","mCamera not null");
    	}
    	
    	Intent intent = getIntent();
        
        timelapse_id = intent.getExtras().getInt("timelapse_id");
        
        if(c.time_lapse_map.get(timelapse_id).last_image_path != null)
        	setCameraOverlay(c.time_lapse_map.get(timelapse_id).last_image_path);
    	
    }

    private OnTouchListener shutterListener = new OnTouchListener(){

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if ( (event.getAction()) == event.ACTION_DOWN ) {
				Log.d(TAG,"Taking picture...");
				// takePicture must be called after mCamera.startPreview()
				
				// Called by Camera when a picture's data is ready for processing
				// Restart Camera preview after snapping, and set just-captured photo as overlay
				//TimeLapsePictureCallback tlpc = CameraUtils.TimeLapsePictureCallback(timelapse_id);
				mCamera.takePicture(CameraUtils.mShutterFeedback, null, null, new CameraUtils.TimeLapsePictureCallback(timelapse_id));
				// Consume touch event
				return true;
            }
			return false;
		}
    };
    
    /** Display the just-captured picture as the camera overlay */
    public static void setCameraOverlay(String filepath){
    	// Decode the just-captured picture from file and display it 
    	// in the cameraOverlay ImageView
    	Log.d("setCameraOverlay","path:"+filepath);
    	File imgFile = new  File(filepath);
    	if(imgFile.exists()){
    	    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

    	    cameraOverlay.setImageBitmap(myBitmap);
    	    // Ensure camera_overlay is visible
    	    // visibility: 0 : visible, 1 : invisible, 2 : gone
        	cameraOverlay.setVisibility(0);
    	}
    	
    	mCameraPreview.restartPreview();
    }

    /** Handle shutter action feedback 
     *  CALLED BY: CameraUtils.mShutterFeedback (callback method passed to mCamera.takePicture(...) via shutterListener) */
	public static void showShutterFeedback() {
		CharSequence text = "Snap!";
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(c, text, duration);
		toast.show();
		
	}
	
	/** Release Camera when application is finished */
	private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }
	
	/** Show an AlertDialog corresponding to a Camera Error */
	private void showCameraErrorDialog(){
		AlertDialog noCameraAlertDialog = new AlertDialog.Builder(c)
		.setTitle(getResources().getStringArray(R.array.camera_error_dialog)[0])
		.setMessage(getResources().getStringArray(R.array.camera_error_dialog)[1])
		.setNeutralButton(getString(R.string.dialog_ok), new OnClickListener(){
			@Override
			public void onClick(DialogInterface thisDialog, int arg1) {
				// Cancel the dialog
				thisDialog.cancel();	
			}
		}).create();

		noCameraAlertDialog.show();
	}
	
	private void setupCamera(){
		// set preview size and make any resize, rotate or
        // reformatting changes here
        
        List supportedPictureSizes = mCamera.getParameters().getSupportedPictureSizes();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureSize(((Camera.Size)supportedPictureSizes.get(0)).width, ((Camera.Size)supportedPictureSizes.get(0)).height);
        // Set autoFocus mode
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        
        // Set Camera preview params
        List<Size> sizes = parameters.getSupportedPreviewSizes();
        // match preview to output picture aspect ratio
        Size size = parameters.getPictureSize();
        
        Size optimalSize = CameraUtils.getOptimalPreviewSize(this,
                sizes, (double) size.width / size.height);
        
        Size original = parameters.getPreviewSize();
        if (!original.equals(optimalSize)) {
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
        }
        
        //parameters.setPreviewSize(((Camera.Size)supportedPictureSizes.get(8)).width, ((Camera.Size)supportedPictureSizes.get(8)).height);
        //Log.d("onSurfaceChanged","width: "+ String.valueOf(((Camera.Size)parameters.getPreviewSize()).width) + " x "+ String.valueOf(((Camera.Size)parameters.getPreviewSize()).height));
        mCamera.setParameters(parameters);
	}
   
}