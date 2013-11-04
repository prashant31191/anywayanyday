package de.duester.anywayanyday;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.duester.anywayanyday.viewpager.InfinitePagerAdapter;

public class MainActivity extends FragmentActivity {
	private TextView tvName;
	private TextView tvAddress;
	private ViewPager vpPager;
	private TextView tvDescription;

	private JSONObject json;
	private String jsonString;
	private String name;
	private String address;
	private String description;
	private String[] imageLinks;
	private int currentItem;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		init(savedInstanceState);
	}

	private void init(Bundle savedInstanceState) {
		tvName = (TextView) findViewById(R.id.tvName);
		tvAddress = (TextView) findViewById(R.id.tvAddress);
		tvDescription = (TextView) findViewById(R.id.tvDescription);

		vpPager = (ViewPager) findViewById(R.id.vpPager);

		boolean forceLoad = false;
		if (savedInstanceState == null)
			forceLoad = true;
		else {
			jsonString = savedInstanceState.getString("json");
			if (jsonString == null)
				forceLoad = true;
		}

		if (forceLoad)
			new LoadJsonTask().execute("https://www.anywayanyday.com/hotels/Hotel/Details/"
					+ "?Id=king-grove-new-york&Language=ru&Currency=USD");
		else
			try {
				json = new JSONObject(jsonString).getJSONObject("Result");
				name = savedInstanceState.getString("name");
				address = savedInstanceState.getString("address");
				description = savedInstanceState.getString("description");
				imageLinks = savedInstanceState.getStringArray("imageLinks");
				currentItem = savedInstanceState.getInt("currentItem");

				if (name == null || address == null || description == null || imageLinks == null)
					parse();
				showData();
				showImages();
			} catch (JSONException e) {
				Log.e(MainActivity.class.toString(), "Error parsing data " + e.toString());
			}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("json", jsonString);
		outState.putString("name", name);
		outState.putString("address", address);
		outState.putString("description", description);
		outState.putStringArray("imageLinks", imageLinks);
		outState.putInt("currentItem", vpPager.getCurrentItem());
	}

	private void parse() {
		try {
			name = json.getString("Name");

			address = json.getString("Address") + ", " + json.getString("City");

			description = json.getString("Description");

			JSONArray jsonImages = json.getJSONArray("Images");
			imageLinks = new String[jsonImages.length()];
			for (int i = 0; i < jsonImages.length(); i++)
				imageLinks[i] = jsonImages.getString(i);

			ImageHelper.imagesCache = new Bitmap[imageLinks.length];
		} catch (JSONException e) {
			Log.e(MainActivity.class.toString(), "Error parsing data " + e.toString());
		}
	}

	private void showData() {
		tvName.setText(name);

		tvAddress.setText(address);

		if (tvDescription != null)
			tvDescription.setText(description);
	}

	private void showImages() {
		View[] pages = new View[ImageHelper.imagesCache.length];

		for (int i = 0; i < ImageHelper.imagesCache.length; i++) {
			if (ImageHelper.imagesCache[i] == null) {
				ProgressBar pb = new ProgressBar(MainActivity.this, null,
						android.R.attr.progressBarStyle);
				pb.setIndeterminate(true);
				pages[i] = pb;
			} else {
				ImageView iv = new ImageView(this);
				iv.setImageBitmap(ImageHelper.imagesCache[i]);
				pages[i] = iv;
			}
		}
		InfinitePagerAdapter pagerAdapter = new InfinitePagerAdapter(pages);
		vpPager.setAdapter(pagerAdapter);
		vpPager.setCurrentItem(currentItem);

		for (int i = 0; i < ImageHelper.imagesCache.length; i++) {
			if (ImageHelper.imagesCache[i] == null)
				loadImage(imageLinks[i], i);
		}
	}

	private synchronized void setImage(int position) {
		InfinitePagerAdapter adapter = (InfinitePagerAdapter) vpPager.getAdapter();
		vpPager.setAdapter(null);

		ImageView iv = new ImageView(this);
		iv.setImageBitmap(ImageHelper.imagesCache[position]);
		Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
		iv.startAnimation(anim);

		adapter.replaceView(iv, position);
		vpPager.setAdapter(adapter);
	}

	private void loadImage(final String urlString, final int position) {
		new Thread() {

			public void run() {
				try {
					URL url = new URL(urlString);
					HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
					httpConnection.setRequestMethod("GET");
					httpConnection.connect();
					InputStream is = null;
					if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
						is = httpConnection.getInputStream();
						ImageHelper.imagesCache[position] = BitmapFactory.decodeStream(is);
						is.close();
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								setImage(position);
							}
						});
					}
				} catch (Exception e) {
					Log.e(MainActivity.class.toString(), e.toString());
				}
			}
		}.start();
	}

	class LoadJsonTask extends AsyncTask<String, Void, String> {
		private AlertDialog dialog;

		@Override
		protected void onPreExecute() {
			createDialog(R.string.txtLoading);
		}

		@Override
		protected String doInBackground(String... params) {
			StringBuilder builder = new StringBuilder();
			HttpClient client = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(params[0]);
			try {
				HttpResponse response = client.execute(httpGet);
				StatusLine statusLine = response.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				if (statusCode == 200) {
					HttpEntity entity = response.getEntity();
					InputStream content = entity.getContent();
					BufferedReader reader = new BufferedReader(new InputStreamReader(content));
					String line;
					while ((line = reader.readLine()) != null) {
						builder.append(line);
					}
				} else {
					Log.e(MainActivity.class.toString(), "Failed to download file");
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return builder.toString();
		}

		@Override
		protected void onPostExecute(String result) {
			try {
				dialog.setMessage(getString(R.string.txtProcessing));
				json = new JSONObject(result).getJSONObject("Result");
				jsonString = result;
				parse();
				dialog.dismiss();
				showData();
				showImages();
			} catch (JSONException e) {
				Log.e(MainActivity.class.toString(), "Error parsing data " + e.toString());
			}
		}

		private void createDialog(int resId) {
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			builder.setMessage(resId);
			dialog = builder.create();
			dialog.setCanceledOnTouchOutside(false);
			dialog.show();
		}
	}
}
