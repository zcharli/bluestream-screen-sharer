package che.carleton.ottawa.NavDrawer;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import java.util.ArrayList;

import che.carleton.ottawa.bluestream.R;

/**
 * Created by CZL on 1/17/2015.
 */
public class NavDrawerListAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<NavDrawerItem> navDrawerItemsList;

    public NavDrawerListAdapter(Context context, ArrayList<NavDrawerItem> navDrawerItemsList) {
        this.context = context;
        this.navDrawerItemsList = navDrawerItemsList;
    }
    @Override
    public int getCount() {
        return navDrawerItemsList.size();
    }

    @Override
    public Object getItem(int position) {
        if(position < 0 || position > getCount() - 1)
            throw new IndexOutOfBoundsException("No nav item exists at that index");
        return navDrawerItemsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.drawer_list_item, null);
        }

        ImageView imgIcon = (ImageView) convertView.findViewById(R.id.icon);
        TextView txtTitle = (TextView) convertView.findViewById(R.id.title);
        TextView txtCount = (TextView) convertView.findViewById(R.id.counter);

        imgIcon.setImageResource(navDrawerItemsList.get(position).getIcon());
        txtTitle.setText(navDrawerItemsList.get(position).getTitle());

        //display count here, check visibility first
        if(navDrawerItemsList.get(position).isCounterVisible()) {
            txtCount.setText(navDrawerItemsList.get(position).getCount());
        }else{
            txtCount.setVisibility((View.GONE));
        }
        return convertView;
    }
}
