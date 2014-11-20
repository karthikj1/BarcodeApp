package karthik.BarcodeLocalizer;
  
import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

class ToastDisplayer implements Runnable{
	// helper class to access UI context - find a better way to do this
	Activity parent;
	String text;
	
	public static ToastDisplayer getToastDisplayer(Activity parentActivity){
		return new ToastDisplayer(parentActivity);
	}
	
	private ToastDisplayer(Activity parentActivity){
		parent = parentActivity;
	}

	public void setText(String toastText){
		text = toastText;
	}
	
	public void showText(String toastText){
		setText(toastText);	
		parent.runOnUiThread(this);
	}
	
	public void run(){
	    Toast.makeText(parent, text, Toast.LENGTH_SHORT).show();        	    			
		}
	}   
