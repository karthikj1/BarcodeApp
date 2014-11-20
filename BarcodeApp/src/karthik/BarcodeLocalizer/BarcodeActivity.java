
package karthik.BarcodeLocalizer;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import karthik.Barcode.Barcode;
import karthik.Barcode.CandidateResult;
import karthik.Barcode.MatrixBarcode;
import karthik.Barcode.TryHarderFlags;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

public class BarcodeActivity extends Activity implements CvCameraViewListener2{
	
    private static final String  TAG = "PicasaBarcodeActivity";
    private CameraBridgeViewBase mOpenCvCameraView;
    private static int frameCount = 0;
    private ToastDisplayer toastDisplay;
    private final Reader reader = new MultiFormatReader();

    private final Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
    private final Scalar barcode_area_colour = new Scalar(0, 0, 255); 

    private Barcode barcode = null;
    private int cameraIndex = 0;
    
    private SubMenu mCameraMenu;      
    private MenuItem[] cameraChoice;
    
    private String mEmail;
    private String mToken;
    
    private static Mat rgba;
    private static Bitmap bmp;
    // map used to store barcodes found in this session 
    // used to prevent from uploading multiple images of same barcode within short time span
    private Map<String, Boolean> foundCodes = new HashMap<String, Boolean>();
    
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public BarcodeActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.barcode_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.barcode_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        frameCount = 0;
        
        toastDisplay = ToastDisplayer.getToastDisplayer(this);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        
        Intent intent = getIntent();
        mEmail = intent.getStringExtra(TokenFetcherTask.EMAIL_MSG);
        mToken = intent.getStringExtra(TokenFetcherTask.TOKEN_MSG);
        Log.i(TAG, "Scanning for barcodes with id " + mEmail);
    }

    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        barcode = null;
        foundCodes = null;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
      mCameraMenu = menu.addSubMenu("Switch Camera");      
      cameraChoice = new MenuItem[2];
      
      cameraChoice[0] = mCameraMenu.add(1, 0, Menu.NONE, "Rear Camera");
      cameraChoice[1] = mCameraMenu.add(1, 1, Menu.NONE, "Front Camera");

      return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	if(item.getGroupId() == 1){
	        cameraIndex = item.getItemId();
	    	Log.i(TAG, "called onOptionsItemSelected; selected item: " + item + " " + item.getItemId());
	        Toast.makeText(this,"Switching to " + (String) item.getTitle(), Toast.LENGTH_SHORT).show();;
    	}
    	
    	mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(cameraIndex);
        mOpenCvCameraView.enableView();
        
        return true;
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	rgba = inputFrame.rgba();
    	// flip Mat if using front camera to prevent preview being upside down
    	// fixes a quirk caused by difference in coordinate systems between Android and OpenCV    	
    	
    	if(cameraIndex == 1) 
    		Core.flip(rgba, rgba, 1);
    	Size sizeRgba = rgba.size();        
    	Log.d(TAG, "Frame count is " + frameCount++);
    	try{
            if (barcode == null)
                barcode = new MatrixBarcode("", rgba, TryHarderFlags.VERY_SMALL_MATRIX);
            else{
                if(!Barcode.updateImage(barcode, rgba)){
                    barcode = new MatrixBarcode("", rgba, TryHarderFlags.VERY_SMALL_MATRIX);
                }
            }

        // findBarcode() returns a List<CandidateResult> with all possible candidate barcode regions from
        // within the image. These images then get passed to a decoder(we use ZXing here but could be any decoder)
        List<CandidateResult> results = barcode.locateBarcode();
        for (CandidateResult candidate: results){               
        	String barcodeText =decodeBarcode(candidate); 
            if(!barcodeText.equals("")){ // we found a barcode
                Log.d(TAG, "Barcode text is " + barcodeText);            
                toastDisplay.showText(barcodeText);
            	// first mark it onscreen
            	for (int j = 0; j < 3; j++)
            		Core.line(rgba, candidate.ROI_coords[j], candidate.ROI_coords[j + 1], barcode_area_colour, 2, Core.LINE_AA, 0);
            	Core.line(rgba, candidate.ROI_coords[3], candidate.ROI_coords[0], barcode_area_colour, 2, Core.LINE_AA, 0);
            	if (!(foundCodes.containsKey(barcodeText))){
	            	// we haven't seen this barcode before so it is worth uploading it to Picasa
            		// first store it in map
            		Log.i(TAG, "Found new code " + barcodeText);
            		foundCodes.put(barcodeText, true);
            		// now upload it to Picasa
	            	bmp = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
	            	Utils.matToBitmap(rgba, bmp);
	            	new ImageUploader(this, mEmail, bmp, toastDisplay, mToken).execute();
	            	}
            }
        }
    	}
    	catch(IOException ioe){
            Log.e(TAG, "IO Exception when finding barcode " + ioe.getMessage());
            toastDisplay.showText("IOException when finding barcode");
    	}

        return rgba;
    }

    private String decodeBarcode(CandidateResult cr) {
        // decodes barcode using ZXing and either print the barcode text or says no barcode found
        Result result = null;
        Bitmap bMap = null;

        Log.v(TAG, "Decoding barcode");
    	bMap = Bitmap.createBitmap(cr.ROI.width(), cr.ROI.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(cr.ROI, bMap);
        int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];  
      //copy pixel data from the Bitmap into the 'intArray' array  
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());  

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(),intArray);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        try {
            result = reader.decode(bitmap, hints);
            String barcode_text = result.getText();
            return barcode_text;
            
        } catch (ReaderException re) {
            Log.d(TAG, " - no barcode found - " + cr.getROI_coords());                
        }
        
        return "";

    }
    
 }

