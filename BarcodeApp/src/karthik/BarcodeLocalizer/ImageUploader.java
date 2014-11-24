/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package karthik.BarcodeLocalizer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.Link;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.media.MediaByteArraySource;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.GphotoFeed;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;
import com.google.gdata.util.ServiceForbiddenException;

/**
 * Display personalized greeting.
 */
public class ImageUploader extends AsyncTask<Void, Void, Boolean> {
	private static final String TAG = "PicasaTokenTask";
	protected BarcodeActivity mActivity;
	private static final String PICASA_PREFIX = "https://picasaweb.google.com/data/feed/api/user/";
	private static final String SMARTHOME_ALBUM_NAME = "smarter";
	PicasawebService picasaService;
	private ToastDisplayer toastDisplay;
		
	protected String mEmail;
	protected Bitmap img;
	protected String token;
	private Location pic_location;
	
	ImageUploader(BarcodeActivity activity, String email, Bitmap bmp, ToastDisplayer toaster
			, String tok, Location loc) {
		this.mActivity = activity;		
		this.mEmail = email;
		this.img = bmp;
		this.toastDisplay = toaster;
		this.token = tok;
		this.pic_location = loc;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			picasaService = new PicasawebService("ImageUploader");
			picasaService.setAuthSubToken(token);

			List<AlbumEntry> albums = null;
			AlbumEntry HeliosAlbum = null;
			albums = getAlbums(mEmail);
			Log.d(TAG, "Got " + albums.size() + " albums ");
			for (AlbumEntry myAlbum : albums) {
				String albumName = myAlbum.getTitle().getPlainText();
				Log.d(TAG, "Album " + albumName + " " + myAlbum.getId());
				if (albumName.equals(SMARTHOME_ALBUM_NAME))
					HeliosAlbum = myAlbum;
			}
			if (HeliosAlbum == null)
				HeliosAlbum = createAlbum(SMARTHOME_ALBUM_NAME, "Helios SmartHome Project Pics");

			Log.i(TAG, "Trying to upload ...");

			Link albumFeedLink = HeliosAlbum.getLink(
					com.google.gdata.data.Link.Rel.FEED, null);
			URL albumFeedURL = new URL(albumFeedLink.getHref());
			uploadImage(img, albumFeedURL);

			Log.i(TAG, "Upload successful");
			toastDisplay.showText("Upload successful");

			return true;
		}

		catch (IOException ex) {
			onError("Following Error occured, please try again. "
					+ ex.getMessage(), ex);
		} catch (ServiceForbiddenException e) {
			onError("ServiceForbiddenException", e);
		} catch (ServiceException e) {
			onError("ServiceException", e);
		}
		return false;
	}

	protected void onError(String msg, Exception e) {
		if (e != null) {
			Log.e(TAG, "Exception: ", e);
		}
		toastDisplay.showText(msg); // will be run in UI thread

	}

	public <T extends GphotoFeed> T getFeed(String feedHref, Class<T> feedClass)
			throws IOException, ServiceException {
		Log.v(TAG, "Get Feed URL: " + feedHref);
		return picasaService.getFeed(new URL(feedHref), feedClass);
	}

	public List<AlbumEntry> getAlbums(String userId) throws IOException,
			ServiceException {

		String albumUrl = PICASA_PREFIX + userId;
		UserFeed userFeed = getFeed(albumUrl, UserFeed.class);
		List<GphotoEntry> entries = userFeed.getEntries();
		List<AlbumEntry> albums = new ArrayList<AlbumEntry>();
		for (GphotoEntry entry : entries) {
			AlbumEntry ae = new AlbumEntry(entry);
			albums.add(ae);
		}

		return albums;
	}

	protected void uploadImage(Bitmap bmp, URL albumURL)
			throws IOException, ServiceException {

		PhotoEntry myPhoto = new PhotoEntry();
		
		SimpleDateFormat s = new SimpleDateFormat("dd_MM_yyyy_hh_mm_ss");
		String timeStamp = s.format(new Date());
		String title = "Barcode_" + timeStamp;
		myPhoto.setTitle(new PlainTextConstruct(title));

		// convert bitmap to jpeg and make it a byte array
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bmp.compress(CompressFormat.JPEG, 75, bos);

		MediaByteArraySource myMedia = new MediaByteArraySource(bos.toByteArray(), "image/jpeg");
		myPhoto.setMediaSource(myMedia);
		
		if(pic_location != null) // only set location if it is a valid Location object
			myPhoto.setGeoLocation(pic_location.getLatitude(), pic_location.getLongitude());
		
		try {
			picasaService.insert(albumURL, myPhoto);
		} catch (Exception e) {
			Log.i(TAG, "Insertion error: " + e);
			toastDisplay.showText("Insertion error: " + e);
		}
		Log.i(TAG, "Photo uploaded");
	}

	private AlbumEntry createAlbum(final String albumName, final String albumDescription) 
			throws IOException, ServiceException{
		
		AlbumEntry myAlbum = new AlbumEntry();
		myAlbum.setTitle(new PlainTextConstruct(albumName));
		myAlbum.setDescription(new PlainTextConstruct(albumDescription));
		URL postUrl = new URL(PICASA_PREFIX + mEmail + "/");

		AlbumEntry insertedEntry = picasaService.insert(postUrl, myAlbum);
		if (insertedEntry != null)
			Log.i(TAG, "Album: " + insertedEntry.getName()+ " created sucessfully");
		
		return insertedEntry;
	}
}
