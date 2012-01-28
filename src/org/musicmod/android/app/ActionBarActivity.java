package org.musicmod.android.app;

import org.musicmod.android.R;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ActionBarActivity extends FragmentActivity {

	private LinearLayout mCustomViewContainer;

	@Override
	public void onCreate(Bundle saveInstanceState) {
		super.onCreate(saveInstanceState);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
			requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	}

	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(layoutResID);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mCustomViewContainer = new LinearLayout(this);
			mCustomViewContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT));
			getActionBar().setCustomView(mCustomViewContainer);
		} else {
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.actionbar_common);
			mCustomViewContainer = (LinearLayout) findViewById(R.id.custom_view_container);
		}
		setTitle(super.getTitle());
	}

	public View setCustomView(View view) {
		mCustomViewContainer.removeAllViews();
		mCustomViewContainer.addView(view, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		return view;
	}

	public View setCustomView(int layoutResID) {
		return setCustomView(getLayoutInflater().inflate(layoutResID, null));
	}

	public void setCustomViewEnabled(boolean enabled) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayShowCustomEnabled(enabled);
		} else {
			findViewById(R.id.custom_view_container).setVisibility(
					enabled ? View.VISIBLE : View.GONE);
		}
	}

	public void setTitleViewEnabled(boolean enabled) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayShowTitleEnabled(enabled);
		} else {
			findViewById(R.id.title_view).setVisibility(enabled ? View.VISIBLE : View.GONE);
		}
	}

	public void setHomeButtonEnabled(boolean enabled) {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayShowHomeEnabled(enabled);
		} else {
			findViewById(R.id.home).setVisibility(enabled ? View.VISIBLE : View.GONE);
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setTitle(title);
		} else {
			TextView mTitleView = (TextView) findViewById(R.id.title);
			if (mTitleView != null) {
				mTitleView.setText(title);
			}
		}

	}

	@Override
	public void setTitle(int resId) {
		setTitle(getString(resId));

	}

	public void setSubtitle(CharSequence subtitle) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setSubtitle(subtitle);
		} else {
			TextView mSubtitleView = (TextView) findViewById(R.id.subtitle);
			if (mSubtitleView == null) return;
			if (subtitle != null) {
				mSubtitleView.setVisibility(View.VISIBLE);
				mSubtitleView.setText(subtitle);
			} else {
				mSubtitleView.setVisibility(View.GONE);
				mSubtitleView.setText("");
			}
		}
	}

	public void setSubtitle(int resId) {
		setSubtitle(getString(resId));
	}
}
