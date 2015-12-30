package com.android.test.imagesearch;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private List<String> mImagePaths = new ArrayList<String>();
	private GridView mGridView;
	private ImageAdapter mImageLoadAdapter;
	private String mServerURL = "https://en.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&piprop=thumbnail&pithumbsize=50&pilimit=50&generator=prefixsearch&gpslimit=50&gpssearch=";
	private SearchTask mSearchTask = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mGridView = (GridView) findViewById(R.id.gridView);

		mImageLoadAdapter = new ImageAdapter(this, mImagePaths);

		EditText editTextImageSearch = (EditText) findViewById(R.id.editTextImageSearch);
		editTextImageSearch.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
				if (s != null && s.length() > 0) {
					if (mGridView.getAdapter() == null) {
						mGridView.setAdapter(mImageLoadAdapter);
					}
					if (mSearchTask != null && !mSearchTask.isCancelled()) {
						mSearchTask.cancel(true);
					}
					String param = s.toString();
					try {
						param = URLEncoder.encode(s.toString(), "utf-8");
						param = param.replaceAll("\\+", "%20");
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mSearchTask = null;
					mSearchTask = new SearchTask();
					mSearchTask.execute(mServerURL + param);
				} else {
					mGridView.setAdapter(null);
					clearImagePaths();
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				if (s == null || s.length() == 0) {
					mGridView.setAdapter(null);
					clearImagePaths();
				}
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}
		});
	}

	private void clearImagePaths() {
		mImagePaths.clear();
	}

	@Override
	public void onDestroy() {
		mGridView.setAdapter(null);
		super.onDestroy();
	}

	public void onItemClick(int mPosition) {
		String imageURL = mImagePaths.get(mPosition);
		Toast.makeText(MainActivity.this, "Image URL : " + imageURL, Toast.LENGTH_LONG).show();
	}

	private class SearchTask extends AsyncTask<String, Void, Void> {
		private String Content;
		private String Error = null;
		private String data = "";

		protected void onPreExecute() {
		}

		protected Void doInBackground(String... urls) {
			BufferedReader reader = null;
			try {

				// Defined URL where to send data
				URL url = new URL(urls[0]);

				// Send POST data request
				URLConnection conn = url.openConnection();
				conn.setDoOutput(true);
				OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
				wr.write(data);
				wr.flush();

				// Get the server response
				reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String line = null;

				// Read Server Response
				while ((line = reader.readLine()) != null) {
					// Append server response in string
					sb.append(line + "");
				}
				// Append Server Response To Content String
				Content = sb.toString();
			} catch (Exception ex) {
				Error = ex.getMessage();
			} finally {
				try {
					reader.close();
				} catch (Exception ex) {
				}
			}
			return null;
		}

		protected void onPostExecute(Void unused) {
			if (Error == null) {
				try {
					clearImagePaths();

					// Parsing JSON response
					JSONObject content = new JSONObject(Content);
					if (content.optString("query") == "")
						return;

					JSONObject query = content.getJSONObject("query");
					JSONObject pages = query.optJSONObject("pages");
					JSONArray pageKeys = pages.names();

					String[] imagePaths = new String[pageKeys.length()];
					for (int i = 0; i < pageKeys.length(); i++) {
						JSONObject page = pages.getJSONObject(pageKeys.getString(i));
						String imagePath = "NA";
						int index = 0;
						if (page.optString("title") != "" && page.optInt("index") != 0) {
							index = page.optInt("index");
							if (page.optString("thumbnail") != "") {
								JSONObject image = page.getJSONObject("thumbnail");
								if (image != null) {
									imagePath = image.optString("source");
								}
							}
							imagePaths[index - 1] = imagePath;
						}
					}

					for (int i = 0; i < imagePaths.length; i++) {
						mImagePaths.add(i, imagePaths[i]);
					}

				} catch (JSONException e) {
					e.printStackTrace();
				} finally {
					mImageLoadAdapter.imageLoader.clearCache();
					mImageLoadAdapter.notifyDataSetChanged();
				}
			}
		}
	}
}
