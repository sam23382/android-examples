package com.android.test.imagesearch;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter implements OnClickListener {

	private Activity mActivity;
	private String[] mData;
	private static LayoutInflater sInflater = null;
	public ImageLoader imageLoader;

	public ImageAdapter(Activity a, String[] d) {
		mActivity = a;
		mData = d;
		sInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		imageLoader = new ImageLoader(mActivity.getApplicationContext());
	}

	public int getCount() {
		return mData.length;
	}

	public Object getItem(int position) {
		return position;
	}

	public long getItemId(int position) {
		return position;
	}

	public static class ViewHolder {
		public ImageView image;
	}

	public View getView(int position, View convertView, ViewGroup parent) {

		View vi = convertView;
		ViewHolder holder;

		if (convertView == null) {
			vi = sInflater.inflate(R.layout.grid_item, null);

			holder = new ViewHolder();
			holder.image = (ImageView) vi.findViewById(R.id.grid_item_image);

			vi.setTag(holder);
		} else
			holder = (ViewHolder) vi.getTag();

		ImageView image = holder.image;

		imageLoader.DisplayImage(mData[position], image);

		vi.setOnClickListener(new OnItemClickListener(position));
		return vi;
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
	}

	private class OnItemClickListener implements OnClickListener {
		private int mPosition;

		OnItemClickListener(int position) {
			mPosition = position;
		}

		@Override
		public void onClick(View arg0) {
			MainActivity sct = (MainActivity) mActivity;
			sct.onItemClick(mPosition);
		}
	}
}