package karthik.BarcodeLocalizer;

import java.io.IOException;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

class TokenClearerTask extends AsyncTask<Void, Void, Void>{

	private static final String TAG = "PicasaTokenClearerTask";
	protected LoginActivity mActivity;
	
	protected String mToken;
	
	TokenClearerTask(LoginActivity activity, String token) {
		this.mActivity = activity;
		this.mToken = token;
	}
	
	protected Void doInBackground(Void... params) {		
		try {
			mActivity.showToast("Logging out ...");
			GoogleAuthUtil.clearToken(mActivity, mToken);
			mActivity.setToken(null);
			mActivity.showToast("Logged out");
		}		
		catch (IOException ex) {
			onError("IO Exception occured, please try again. "
					+ ex.getMessage(), ex);
		}
		catch(Exception e){
			onError("Exception occured while logging out, please try again. "
					+ e.getMessage(), e);
		}
		return null;
	}
	

	protected void onError(String msg, Exception e) {
		if (e != null) {
			Log.e(TAG, "Exception: ", e);
		}
		mActivity.show(msg); // will be run in UI thread
	}

}
