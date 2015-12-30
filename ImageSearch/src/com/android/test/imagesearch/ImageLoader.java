package com.android.test.imagesearch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.widget.ImageView;

public class ImageLoader {
	private MemoryCache mMemoryCache = new MemoryCache();
	private FileCache mFileCache;
	private Map<ImageView, String> mImageViews = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
	private ExecutorService mExecutorService;
	private Handler mHandler = new Handler();
	final int defaultImageId = R.drawable.default_image;

	public ImageLoader(Context context) {
		mFileCache = new FileCache(context);
		mExecutorService = Executors.newFixedThreadPool(5);
	}

	public void DisplayImage(String url, ImageView imageView) {
		mImageViews.put(imageView, url);

		Bitmap bitmap = mMemoryCache.get(url);
		if (bitmap != null) {
			imageView.setImageBitmap(bitmap);
		} else {
			if (url != "") {
				queueImage(url, imageView);
			}
			imageView.setImageResource(defaultImageId);
		}
	}

	private void queueImage(String url, ImageView imageView) {
		ImageToLoad imageToLoad = new ImageToLoad(url, imageView);
		mExecutorService.submit(new ImagesLoader(imageToLoad));
	}

	private class ImageToLoad {
		public String url;
		public ImageView imageView;

		public ImageToLoad(String u, ImageView i) {
			url = u;
			imageView = i;
		}
	}

	class ImagesLoader implements Runnable {
		ImageToLoad imageToLoad;

		ImagesLoader(ImageToLoad imageToLoad) {
			this.imageToLoad = imageToLoad;
		}

		@Override
		public void run() {
			try {
				if (imageViewReused(imageToLoad))
					return;
				Bitmap bmp = getBitmap(imageToLoad.url);

				mMemoryCache.put(imageToLoad.url, bmp);

				if (imageViewReused(imageToLoad))
					return;

				BitmapDisplayer bd = new BitmapDisplayer(bmp, imageToLoad);
				mHandler.post(bd);

			} catch (Throwable th) {
				th.printStackTrace();
			}
		}
	}

	private Bitmap getBitmap(String url) {
		File f = mFileCache.getFile(url);

		Bitmap b = decodeFile(f);
		if (b != null)
			return b;

		try {

			Bitmap bitmap = null;
			URL imageUrl = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
			conn.setConnectTimeout(30000);
			conn.setReadTimeout(30000);
			conn.setInstanceFollowRedirects(true);
			InputStream is = conn.getInputStream();
			OutputStream os = new FileOutputStream(f);
			Utils.CopyStream(is, os);

			os.close();
			conn.disconnect();

			bitmap = decodeFile(f);

			return bitmap;

		} catch (Throwable ex) {
			ex.printStackTrace();
			if (ex instanceof OutOfMemoryError)
				mMemoryCache.clear();
			return null;
		}
	}

	private Bitmap decodeFile(File f) {
		try {
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			FileInputStream stream1 = new FileInputStream(f);
			BitmapFactory.decodeStream(stream1, null, o);
			stream1.close();

			final int REQUIRED_SIZE = 50;
			int width_tmp = o.outWidth, height_tmp = o.outHeight;
			int scale = 1;
			while (true) {
				if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE)
					break;
				width_tmp /= 2;
				height_tmp /= 2;
				scale *= 2;
			}

			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			FileInputStream stream2 = new FileInputStream(f);
			Bitmap bitmap = BitmapFactory.decodeStream(stream2, null, o2);
			stream2.close();
			return bitmap;

		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	boolean imageViewReused(ImageToLoad imageToLoad) {
		String tag = mImageViews.get(imageToLoad.imageView);
		if (tag == null || !tag.equals(imageToLoad.url))
			return true;
		return false;
	}

	class BitmapDisplayer implements Runnable {
		Bitmap bitmap;
		ImageToLoad imageToLoad;

		public BitmapDisplayer(Bitmap b, ImageToLoad p) {
			bitmap = b;
			imageToLoad = p;
		}

		public void run() {
			if (imageViewReused(imageToLoad))
				return;

			if (bitmap != null)
				imageToLoad.imageView.setImageBitmap(bitmap);
			else
				imageToLoad.imageView.setImageResource(defaultImageId);
		}
	}

	public void clearCache() {
		mMemoryCache.clear();
		mFileCache.clear();
	}

}