package org.musicmod.android.app;

import java.util.List;

import org.musicmod.android.R;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PluginFragment extends ListFragment implements LoaderCallbacks<List<ApplicationInfo>> {

	private static PackageManager mPackageManager;
	private PluginAdapter mAdapter;
	private List<ApplicationInfo> mPluginsList;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.plugins_manager, container, false);
		return view;
	}

	@Override
	public Loader<List<ApplicationInfo>> onCreateLoader(int id, Bundle args) {
		return new AppListLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<List<ApplicationInfo>> loader, List<ApplicationInfo> data) {
		mAdapter = new PluginAdapter(getActivity(), R.layout.playlist_list_item, data);
		setListAdapter(mAdapter);

	}

	@Override
	public void onLoaderReset(Loader<List<ApplicationInfo>> loader) {

	}

	private class PluginAdapter extends ArrayAdapter<ApplicationInfo> {

		private List<ApplicationInfo> mList;

		public PluginAdapter(Context context, int resource, List<ApplicationInfo> objects) {
			super(context, resource, objects);
			mList = objects;
		}

		private class ViewHolder {

			ImageView plugin_icon;
			TextView plugin_name;
			TextView plugin_description;

			public ViewHolder(View view) {

				plugin_icon = (ImageView) view.findViewById(R.id.plugin_icon);
				plugin_name = (TextView) view.findViewById(R.id.plugin_name);
				plugin_description = (TextView) view.findViewById(R.id.plugin_description);
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			ViewHolder viewholder = view != null ? (ViewHolder) view.getTag() : null;

			if (viewholder == null) {
				view = getLayoutInflater(getArguments()).inflate(R.layout.playlist_list_item, null);
				viewholder = new ViewHolder(view);
				view.setTag(viewholder);
			}

			viewholder.plugin_icon.setImageDrawable(mList.get(position).loadIcon(mPackageManager));
			viewholder.plugin_name.setText(mList.get(position).loadLabel(mPackageManager));
			viewholder.plugin_description.setText(mList.get(position).loadDescription(
					mPackageManager));

			return view;
		}

	}

	public static class AppListLoader extends AsyncTaskLoader<List<ApplicationInfo>> {

		public AppListLoader(Context context) {
			super(context);
			mPackageManager = context.getPackageManager();
		}

		@Override
		public List<ApplicationInfo> loadInBackground() {
			return mPackageManager.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES
					| PackageManager.GET_DISABLED_COMPONENTS);
		}

	}
}
