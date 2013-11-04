package de.duester.anywayanyday.viewpager;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

public class InfinitePagerAdapter extends PagerAdapter {
	private View[] pages;

	public InfinitePagerAdapter(View[] pages) {
		this.pages = pages;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		position %= getRealCount();
		View v = pages[position];
		container.addView(v);
		return v;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		position %= getRealCount();
		container.removeView(pages[position]);
	}

	@Override
	public int getCount() {
		return Integer.MAX_VALUE;
	}

	public int getRealCount() {
		return pages.length;
	}

	@Override
	public boolean isViewFromObject(View arg0, Object arg1) {
		return arg0.equals(arg1);
	}

	public void replaceView(View v, int position) {
		pages[position] = v;
	}

}
