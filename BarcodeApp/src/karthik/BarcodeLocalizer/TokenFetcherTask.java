package karthik.BarcodeLocalizer;

import java.io.IOException;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

class TokenFetcherTask extends AsyncTask<Void, Void, String>{

	private static final String TAG = "PicasaTokenFetcherTask";
	public static final String TOKEN_MSG = "ACCESS_TOKEN";
	public static final String EMAIL_MSG = "USERNAME";
	protected LoginActivity mActivity;
	
	protected String mScope;
	protected String mEmail;
	protected String token;
	
	TokenFetcherTask(LoginActivity activity, String email, String scope) {
		this.mActivity = activity;
		this.mScope = scope;
		this.mEmail = email;
	}
	
	protected String doInBackground(Void... params) {		
		try {
			token = fetchToken();
		}		
		catch (IOException ex) {
			onError("IO Exception occured, please try again. "
					+ ex.getMessage(), ex);
		}
		return token;
	}
	
	protected void onPostExecute(String token){
		// takes token and starts new BarcodeActivity

	/*	checks for null because the way GoogleAuthUtil.getToken works is as follows
		on the first call, it throws a UserRecoverableAuthException which is when the authorization
		window pops up. The AsyncTask TokenFetcherTask terminates at this point and returns null.
		This then filters back through the LoginActivity and, if the user authorizes it, 
		gets to method handleAuthorizeResult which starts another TokenFetcherTask and only on this second attempt
		does the task successfully return a token and start the BarcodeActivity. 
	*/
		if(token != null){
			mActivity.setToken(token);
			Intent intent = new Intent(mActivity, BarcodeActivity.class);
			intent.putExtra(TOKEN_MSG, token);
			intent.putExtra(EMAIL_MSG, mEmail);
			mActivity.startActivity(intent);
			Log.i(TAG, "Started new BarcodeActivity with token for " + mEmail);
		}
	}
	
	/**
	 * Get a authentication token if one is not available. If the error is not
	 * recoverable then it displays the error message on parent activity right
	 * away.
	 */
	protected String fetchToken() throws IOException {
		/**
		 * Contacts the user info server to get the profile of the user and
		 * extracts the first name of the user from the profile. In order to
		 * authenticate with the user info server the method first fetches an
		 * access token from Google Play services.
		 * 
		 * @throws IOException
		 *             if communication with user info server failed.
		 */
		try {
			String token = GoogleAuthUtil.getToken(mActivity, mEmail, mScope); 
			Log.i(TAG, "Got token for scope " + mScope + " for user " + mEmail);
			return token;
			
		} catch (UserRecoverableAuthException userRecoverableException) {
			// GooglePlayServices.apk is either old, disabled, or not present,
			// which is recoverable, so we need to show the user some UI through the
			// activity. Also gets thrown the first time getToken is called 
			// in order to get the user to authorize it
			Log.i(TAG, "UserRecoverableAuthException thrown for " + mEmail + " and " + mScope);
			mActivity.handleException(userRecoverableException);
		} catch (GoogleAuthException fatalException) {
			onError("Unrecoverable error " + fatalException.getMessage(),
					fatalException);
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
