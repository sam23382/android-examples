package com.android.test.imagesearch;

import java.io.File;

import android.content.Context;

public class FileCache {

	private File mCacheDir;

	public FileCache(Context context) {
		if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
			mCacheDir = new File(android.os.Environment.getExternalStorageDirectory(), "LazyList");
		} else {
			mCacheDir = context.getCacheDir();
		}

		if (!mCacheDir.exists()) {
			mCacheDir.mkdirs();
		}
	}

	public File getFile(String url) {
		String filename = String.valueOf(url.hashCode());
		File f = new File(mCacheDir, filename);
		return f;
	}

	public void clear() {
		File[] files = mCacheDir.listFiles();
		if (files == null)
			return;
		for (File f : files)
			f.delete();
	}

}