package org.musicmod.android.dialog;

import org.musicmod.android.view.VerticalTextSpinner;

import android.app.AlertDialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.widget.LinearLayout;


public class VerticalTextSpinnerDialog extends AlertDialog {

	private VerticalTextSpinner mVerticalTextSpinner;
	
	public VerticalTextSpinnerDialog(Context context, String[] items, int position) {
		super(context);
		init(context,items,position);
	}
	
	private void init(Context context, String[] items, int position) {
		DisplayMetrics dm = new DisplayMetrics();
		dm = context.getResources().getDisplayMetrics();
		
		LinearLayout mContainer = new LinearLayout(context);
		mContainer.setGravity(Gravity.CENTER);

		mVerticalTextSpinner = new VerticalTextSpinner(context);
		mVerticalTextSpinner.setItems(items);
		mVerticalTextSpinner.setWrapAround(true);
		mVerticalTextSpinner.setScrollInterval(200);
		mVerticalTextSpinner.setSelectedPos(position);
		mContainer.addView(mVerticalTextSpinner, (int) (120 * dm.density),
				(int) (100 * dm.density));
		
		setView(mContainer);
	}
	
	public int getCurrentSelectedPos() {
		if (mVerticalTextSpinner != null) {
			return mVerticalTextSpinner.getCurrentSelectedPos();
		}
		return 0;
	}

}
