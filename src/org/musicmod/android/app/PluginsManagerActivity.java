/*
 *              Copyright (C) 2011 The MusicMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.musicmod.android.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.musicmod.android.Constants;
import org.musicmod.android.R;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class PluginsManagerActivity extends ListActivity implements Constants {

	private ListView mListView;
	private PluginsListAdapter mAdapter;
	private ArrayList<HashMap<String, Object>> mItems;

	private static final int CONFIGURE_PLUGIN = 1;
	private static final int UNINSTALL_PLUGIN = 2;
	private static final int CANCEL_MENU = 3;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.plugins_manager);
	}

	@Override
	public void onResume() {

		super.onResume();
		refreshList();
	}

	OnItemClickListener mPluginClickedListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

			configurePlugin(position);
		}
	};

	private class PluginsListAdapter extends SimpleAdapter {

		private int[] appTo;
		private String[] appFrom;
		private ViewBinder appViewBinder;
		private List<? extends Map<String, ?>> appData;
		private int appResource;
		private LayoutInflater appInflater;

		public PluginsListAdapter(Context context, List<? extends Map<String, ?>> data,
				int resource, String[] from, int[] to) {

			super(context, data, resource, from, to);
			appData = data;
			appResource = resource;
			appFrom = from;
			appTo = to;
			appInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			return createViewFromResource(position, convertView, parent, appResource);

		}

		private View createViewFromResource(int position, View convertView, ViewGroup parent,
				int resource) {

			View v;
			if (convertView == null) {
				v = appInflater.inflate(resource, parent, false);
				final int[] to = appTo;
				final int count = to.length;
				final View[] holder = new View[count];

				for (int i = 0; i < count; i++) {
					holder[i] = v.findViewById(to[i]);
				}
				v.setTag(holder);
			} else {
				v = convertView;
			}
			bindView(position, v);
			return v;
		}

		private void bindView(int position, View view) {

			final Map<?, ?> dataSet = appData.get(position);
			if (dataSet == null) {
				return;
			}

			final ViewBinder binder = appViewBinder;
			final View[] holder = (View[]) view.getTag();
			final String[] from = appFrom;
			final int[] to = appTo;
			final int count = to.length;

			for (int i = 0; i < count; i++) {
				final View v = holder[i];
				if (v != null) {
					final Object data = dataSet.get(from[i]);
					String text = data == null ? "" : data.toString();
					if (text == null) {
						text = "";
					}

					boolean bound = false;
					if (binder != null) {
						bound = binder.setViewValue(v, data, text);
					}

					if (!bound) {
						if (v instanceof TextView) {
							setViewText((TextView) v, text);
						} else if (v instanceof ImageView) {
							setViewImage((ImageView) v, (Drawable) data);
						} else {
							throw new IllegalStateException(v.getClass().getName() + " is not a "
									+ "view that can be bounds by this SimpleAdapter");
						}
					}
				}
			}
		}

		public void setViewImage(ImageView v, Drawable value) {

			v.setImageDrawable(value);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		int itemId = Integer.valueOf(String.valueOf(info.position));

		switch (item.getItemId()) {
			case CONFIGURE_PLUGIN:
				configurePlugin(itemId);
				break;
			case UNINSTALL_PLUGIN:
				uninstallPlugin(itemId);
				break;
			case CANCEL_MENU:
				break;
		}
		return super.onContextItemSelected(item);
	}

	private void refreshList() {

		PackageManager mPackageManager = getPackageManager();
		List<PackageInfo> mPluginsInfo = mPackageManager.getInstalledPackages(0);
		mItems = new ArrayList<HashMap<String, Object>>();

		for (PackageInfo mPluginInfo : mPluginsInfo) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			String mPluginPname = mPluginInfo.applicationInfo.packageName;
			if (mPluginPname.contains(PLUGINS_PNAME_PATTERN)) {
				Drawable mPluginIcon = mPluginInfo.applicationInfo.loadIcon(mPackageManager);
				String mPluginName = (String) mPluginInfo.applicationInfo
						.loadLabel(mPackageManager);
				String mPluginDescription = (String) mPluginInfo.applicationInfo
						.loadDescription(mPackageManager);
				map.put("plugin_icon", mPluginIcon);
				map.put("plugin_name", mPluginName);
				map.put("plugin_description", mPluginDescription);
				map.put("plugin_pname", mPluginPname);
				mItems.add(map);
			}

		}

		mAdapter = new PluginsListAdapter(this, mItems, R.layout.plugin_list_item, new String[] {
				"plugin_icon", "plugin_name", "plugin_description" }, new int[] { R.id.plugin_icon,
				R.id.plugin_name, R.id.plugin_description });

		mListView = (ListView) findViewById(android.R.id.list);
		mListView.setOnItemClickListener(mPluginClickedListener);
		mListView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

				menu.add(0, CONFIGURE_PLUGIN, 0, getString(R.string.open_configure));
				menu.add(0, UNINSTALL_PLUGIN, 0, getString(R.string.uninstall));
				menu.add(0, CANCEL_MENU, 0, getString(android.R.string.cancel));
			}
		});
		mListView.setAdapter(mAdapter);
	}

	public void configurePlugin(int position) {

		String pname = (String) mItems.get(position).get("plugin_pname");
		Intent intent = new Intent();
		intent.setPackage(pname);
		try {
			intent.setAction(pname + ".CONFIGURE_PLUGIN");
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			try {
				intent.setAction(pname + ".OPEN_PLUGIN");
				startActivity(intent);
			} catch (ActivityNotFoundException e2) {
				try {
					intent.setAction(INTENT_CONFIGURE_PLUGIN);
					startActivity(intent);
				} catch (ActivityNotFoundException e3) {
					try {
						intent.setAction(INTENT_OPEN_PLUGIN);
						startActivity(intent);
					} catch (ActivityNotFoundException e4) {
						Toast.makeText(PluginsManagerActivity.this, R.string.plugin_not_supported,
								Toast.LENGTH_SHORT).show();
					}
				}
			}
		}
	}

	public void uninstallPlugin(int position) {

		String pname = (String) mItems.get(position).get("plugin_pname");
		Uri data = Uri.fromParts("package", pname, null);
		this.startActivity(new Intent(Intent.ACTION_DELETE).setData(data));
	}

}
