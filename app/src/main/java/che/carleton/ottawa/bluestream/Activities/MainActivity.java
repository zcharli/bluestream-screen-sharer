package che.carleton.ottawa.bluestream.Activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import java.util.ArrayList;

import che.carleton.ottawa.NavDrawer.NavDrawerItem;
import che.carleton.ottawa.NavDrawer.NavDrawerListAdapter;
import che.carleton.ottawa.activity.ActivityResultBus;
import che.carleton.ottawa.activity.ActivityResultEvent;
import che.carleton.ottawa.bluestream.Fragments.BluetoothCaptureFragment;
import che.carleton.ottawa.bluestream.Fragments.SettingsFragment;
import che.carleton.ottawa.bluestream.R;


public class MainActivity extends AppCompatActivity {

    private boolean FLOATING_ACTION = false;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;

    private CharSequence drawerTitle;
    private CharSequence appTitle;
    private String[] drawerMenuItems;
    private TypedArray drawerMenuIcons;
    private ArrayList<NavDrawerItem> navDrawerItems;
    private NavDrawerListAdapter drawerListAdapater;

    // Bad to have this here, but we need to reuse the same fragment
    private BluetoothCaptureFragment mBTCaptureFragment = null;
    private SettingsFragment mSettingsFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up tool bar for sliding drawer
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(appTitle);
        toolbar.setLogo(R.drawable.ic_home);
        appTitle = drawerTitle = getTitle();

        //get the components on the view
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.list_slidermenu);


        //get the slide out menu items from string.xml, currently its getting more than it needs
        drawerMenuItems = getResources().getStringArray(R.array.nav_drawer_items);
        drawerMenuIcons = getResources().obtainTypedArray(R.array.nav_drawer_icons);

        //Set up Nav Drawer
        navDrawerItems = new ArrayList<>();

        for (int i = 0; i < drawerMenuItems.length; i++) {
            navDrawerItems.add(new NavDrawerItem(drawerMenuItems[i], drawerMenuIcons.getResourceId(i, -1)));
        }

        drawerMenuIcons.recycle();

        drawerListAdapater = new NavDrawerListAdapter(getApplicationContext(), navDrawerItems);
        drawerList.setAdapter(drawerListAdapater);
        drawerList.setOnItemClickListener(new SlideMenuClickListerner());
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                toolbar,//R.drawable.ic_drawer,//toolbar,//
                R.string.app_name,
                R.string.app_name) {
            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(appTitle);
                //getActionBar().setTitle(appTitle);
                invalidateOptionsMenu();
            }

            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(drawerTitle);
                //getActionBar().setTitle(drawerTitle);
                invalidateOptionsMenu();
            }
        };

        drawerLayout.setDrawerListener(drawerToggle);

        // Set up floating button on bottom right
        final FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater layoutInflater =
                        (LayoutInflater) getBaseContext()
                                .getSystemService(LAYOUT_INFLATER_SERVICE);
                if (!FLOATING_ACTION) {
                    FLOATING_ACTION = !FLOATING_ACTION;
                    View popupView = layoutInflater.inflate(R.layout.floating_action, null);
                    final PopupWindow popupWindow = new PopupWindow(
                            popupView, ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);

                    popupWindow.showAsDropDown(floatingActionButton, Gravity.TOP, -1050, 0);

                    final Button btnDismiss = (Button) popupView.findViewById(R.id.button_dismiss);
                    btnDismiss.setOnClickListener(new Button.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            FLOATING_ACTION = !FLOATING_ACTION;
                            popupWindow.dismiss();
                        }
                    });

                    final Button btnExit = (Button) popupView.findViewById(R.id.button_exit);
                    btnExit.setOnClickListener(new Button.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            FLOATING_ACTION = !FLOATING_ACTION;
                            popupWindow.dismiss();
                            int totalViewsInFragmentStack = getSupportFragmentManager().getBackStackEntryCount();
                            while (totalViewsInFragmentStack != 0) {
                                getSupportFragmentManager().popBackStack();
                                totalViewsInFragmentStack--;
                            }

                            findViewById(R.id.fab).setVisibility(View.INVISIBLE);
                        }
                    });

                    popupView.setOnTouchListener(new View.OnTouchListener() {
                        int orgX, orgY;
                        int offsetX, offsetY;

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            switch (event.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    orgX = (int) event.getX();
                                    orgY = (int) event.getY();
                                    break;
                                case MotionEvent.ACTION_MOVE:
                                    offsetX = (int) event.getRawX() - orgX;
                                    offsetY = (int) event.getRawY() - orgY;
                                    popupWindow.update(offsetX, offsetY, -1, -1, true);
                                    break;
                            }
                            return true;
                        }
                    });
                }
            }
        });

        floatingActionButton.setVisibility(View.INVISIBLE);

        findViewById(R.id.enter_app).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBTCaptureFragment = new BluetoothCaptureFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, mBTCaptureFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ActivityResultBus.getInstance().postQueue(
                new ActivityResultEvent(requestCode, resultCode, data));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState(); //called after onRestoreInstanceState occured
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setTitle(CharSequence title) {
        drawerTitle = title;
        getSupportActionBar().setTitle(drawerTitle);
        //getActionBar().setTitle(drawerTitle);
    }



    public void displayView(int position) {
        Fragment fragment = null;
        switch (position) {
            case 0:
                Toast.makeText(this,"Nothing to see here...",Toast.LENGTH_SHORT).show();
//                if(mBTCaptureFragment == null) {
//                    mBTCaptureFragment = new BluetoothCaptureFragment();
//                }
//                fragment = mBTCaptureFragment;
                break;
            case 1:
                if(mBTCaptureFragment == null) {
                    mBTCaptureFragment = new BluetoothCaptureFragment();
                }
                fragment = mBTCaptureFragment;
                break;
            case 2:
//                if(mBTCaptureFragment != null)
//                    mBTCaptureFragment.setChildScreenCaptureFragment();
                Toast.makeText(this,"Not Implemented Yet",Toast.LENGTH_SHORT).show();
                break;
            case 3:
                //fragment = new SettingsFragment();
                Toast.makeText(this,"Not Implemented Yet",Toast.LENGTH_SHORT).show();
                break;
            case 4:
                if(mSettingsFragment == null) {
                    mSettingsFragment = new SettingsFragment();
                }
                fragment = mSettingsFragment;
                break;
            default:
                break;
        }
        if (fragment != null) {
            FragmentTransaction fragmentManager = getSupportFragmentManager().beginTransaction();
            //fragmentManager.beginTransaction().add(R.id.frame_container, fragment);
            fragmentManager.replace(R.id.fragment_container, fragment);
            fragmentManager.addToBackStack(null);
            fragmentManager.commit();
            drawerList.setItemChecked(position, true);
            drawerList.setSelection(position);
            setTitle(drawerMenuItems[position]);
            drawerLayout.closeDrawer(drawerList);
        } else {
            Log.e("Main Activity", "Error in creating fragment!");
        }
    }

    private class SlideMenuClickListerner implements ListView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            // display view for selected nav drawer item
            displayView(position);
        }

    }
}
