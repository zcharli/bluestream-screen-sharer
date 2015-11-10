package che.carleton.ottawa.bluestream;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.Set;

import che.carleton.ottawa.mjpeg.MjpegInputStream;
import che.carleton.ottawa.mjpeg.MjpegView;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothCaptureFragment extends Fragment {

    private static final String TAG = "BluetoothCaptureFragment"; // change this later

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private int DISPLAY_WIDTH = 480;
    private int DISPLAY_HEIGHT = 640;

    // Layout Views
    private Button mStartCaptureButton;
    private Button mServeScreen;
    private Button mRecvScreen;
    private ScreenCaptureFragment mCaptureFragment = null;

    private String mConnectedDeviceName = null;

    private ArrayAdapter<String> mConversationArrayAdapter;

    private StringBuffer mOutStringBuffer;

    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothService mBluetoothService = null;

    /* Mjpeg viewer */
    private MjpegView mMovieSurface = null;
    private boolean suspending = false;
    private boolean isBroadcasting = false;
    private MjpegInputStream mJpegInputStream = null;

    public int profile_index = 0;

    private String [] mac_address = new String[8];

    private final int [] profile_id = {R.id.profile_icon_1, R.id.profile_icon_2, R.id.profile_icon_3,
            R.id.profile_icon_4,R.id.profile_icon_5,R.id.profile_icon_6,
            R.id.profile_icon_7,R.id.profile_icon_8};
    private final int [] profile_icon = {R.drawable.profile_icon_1, R.drawable.profile_icon_2,
            R.drawable.profile_icon_3, R.drawable.profile_icon_4,R.drawable.profile_icon_5,
            R.drawable.profile_icon_6,R.drawable.profile_icon_7,R.drawable.profile_icon_8};

    private final int [] profile_switch ={R.id.profile_switch_1, R.id.profile_switch_2,
            R.id.profile_switch_3, R.id.profile_switch_4, R.id.profile_switch_5,
            R.id.profile_switch_6, R.id.profile_switch_7, R.id.profile_switch_8};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        //TODO: onStart() everybody is a client, draw client UI, show option to become server
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            //TODO: just enable the button to start capture here
            // Otherwise, setup the chat session
        } else if (mBluetoothService == null) {
            setupChat();
        }

        mMovieSurface = (MjpegView) getView().findViewById(R.id.mjpeg_surface);
        getView().findViewById(R.id.mjpeg_surface).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Profile Information
        // Get a set of currently paired devices
        final Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();

        ImageButton system_profile = (ImageButton) getView().findViewById(profile_id[profile_index]);
        system_profile.setBackgroundResource(profile_icon[profile_index]);

        TextView system_name = (TextView) getView().findViewById(R.id.profile_name);
        String system_name_txt = BluetoothAdapter.getDefaultAdapter().getName()+"\nConnected";
        system_name.setText(system_name_txt);

        TextView system_mac_address = (TextView) getView().findViewById(R.id.mac_address);
        String sys_mac_add_txt = "Mac Address: \n" + BluetoothAdapter.getDefaultAdapter().getAddress();
        system_mac_address.setText(sys_mac_add_txt);

        mac_address[profile_index] = BluetoothAdapter.getDefaultAdapter().getAddress();

        Button system_connection = (Button) getView().findViewById(R.id.profile_connection);
        system_connection.setEnabled(true);
        system_connection.setText(R.string.device_connect_all);
        system_connection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, profile_index == 0 ? "Connection All" : "Connecting",
                        Snackbar.LENGTH_LONG).setAction("Action", null).show();

                getView().findViewById(R.id.mjpeg_surface).setVisibility(View.VISIBLE);

                getView().findViewById(R.id.relativeLayout).setVisibility(View.INVISIBLE);
                getView().findViewById(R.id.frameLayout).setVisibility(View.INVISIBLE);

                isBroadcasting = true;

                setHasOptionsMenu(false);

                // Create the result Intent and include the MAC address
                Intent intent = new Intent();
                intent.putExtra("device_address", mac_address[profile_index]);

                connectDevice(intent, true);

                if (mCaptureFragment != null) {
                    // Capture Frag exists, so this is the server!
                    mCaptureFragment.setBluetoothService(mBluetoothService);
                }

                mCaptureFragment = new ScreenCaptureFragment();
                getChildFragmentManager().beginTransaction()
                        .add(R.id.screen_capture_fragment, mCaptureFragment, "ScreenCaptureFragment")
                        .commit();
            }
        });

        Switch system_switch = (Switch) getView().findViewById(R.id.profile_switch_1);
        system_switch.setChecked(true);
        system_switch.setClickable(false);
        final int [] temp_switch = profile_switch;

        system_profile.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                profile_index = 0;
                ImageView profile_img = (ImageView) getView().findViewById(R.id.profile_img);
                profile_img.setBackgroundResource(profile_icon[profile_index]);

                Button profile_connection = (Button) getView().findViewById(R.id.profile_connection);
                profile_connection.setText(R.string.device_connect_all);
                profile_connection.setEnabled(true);

                TextView profile_name = (TextView) getView().findViewById(R.id.profile_name);
                profile_name.setText(BluetoothAdapter.getDefaultAdapter().getName()+"\n(Connected)");

                TextView mac_address = (TextView) getView().findViewById(R.id.mac_address);
                mac_address.setText("Mac Address: \n" + BluetoothAdapter.getDefaultAdapter().getAddress());
            }
        });


        for (BluetoothDevice device : pairedDevices) {
            ImageButton profile = (ImageButton) getView().findViewById(profile_id[++profile_index]);
            final int index = profile_index;
            final BluetoothDevice temp_device = device;

            profile.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    profile_index = index;
                    ImageView profile_img = (ImageView) getView().findViewById(R.id.profile_img);
                    profile_img.setBackgroundResource(profile_icon[profile_index]);

                    Button profile_connection = (Button) getView().findViewById(R.id.profile_connection);
                    profile_connection.setText(R.string.device_connect);
                    profile_connection.setEnabled(true);

                    TextView profile_name = (TextView) getView().findViewById(R.id.profile_name);
                    profile_name.setText(temp_device.getName()+"\n(Disconnected)");

                    TextView mac_address = (TextView) getView().findViewById(R.id.mac_address);
                    mac_address.setText("Mac Address: \n" + temp_device.getAddress());
                }
            });

            Switch profile_switch = (Switch) getView().findViewById(temp_switch[profile_index]);
            profile_switch.setEnabled(false);

            mac_address[profile_index] = temp_device.getAddress();
        }

        while(profile_index < 7) {
            ImageButton profile = (ImageButton) getView().findViewById(profile_id[++profile_index]);

            final int index = profile_index;
            profile.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    profile_index = index;
                    ImageView profile_img = (ImageView) getView().findViewById(R.id.profile_img);
                    profile_img.setBackgroundResource(profile_icon[index]);

                    Button profile_connection = (Button) getView().findViewById(R.id.profile_connection);
                    profile_connection.setText(R.string.device_no_pairing);
                    profile_connection.setEnabled(false);

                    TextView profile_name = (TextView) getView().findViewById(R.id.profile_name);
                    profile_name.setText(R.string.device_profile);

                    TextView mac_address = (TextView) getView().findViewById(R.id.mac_address);
                    mac_address.setText(R.string.device_mac_address);
                }
            });

            Switch profile_switch = (Switch) getView().findViewById(temp_switch[index]);
            profile_switch.setEnabled(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        getActivity().findViewById(R.id.fab).setVisibility(View.INVISIBLE);

        if (mBluetoothService != null) {
            mBluetoothService.stop();
        }
        if(mMovieSurface != null) {
            mMovieSurface.freeCameraMemory();
        }
    }

    /*@Override
    public void onStop(){
        super.onStop();
    }*/

    public void onPause() {
        super.onPause();

        if (mMovieSurface != null) {
            if (mMovieSurface.isStreaming()) {
                mMovieSurface.stopPlayback();
                suspending = true;
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        //System.out.println("On Resume");
        if (mBluetoothService != null) {
            if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
                mBluetoothService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.device_status, container, false);
    }

    private void setupChat() {
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.setup_chat_message);
        mBluetoothService = new BluetoothService(getActivity(), mHandler);
        mOutStringBuffer = new StringBuffer("");
    }

    private void ensureDiscoverable() {
        //TODO: Make discoverable -> replace fragment_bt_capture_base with the fragment_screen_capture
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    setupChat();
                } else {
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mBluetoothService.connect(device, secure);
    }

    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    private void publishStreamToView(InputStream imgStream) {
        if(mJpegInputStream == null) {
            mJpegInputStream = new MjpegInputStream(imgStream);
            mJpegInputStream.setBluetoothService(mBluetoothService);
            mMovieSurface.setSource(mJpegInputStream);

            if(mJpegInputStream != null) {
                mJpegInputStream.setSkip(1);
                mMovieSurface.setDisplayMode(MjpegView.SIZE_BEST_FIT);
                mMovieSurface.showFps(true);
            }
        }

    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();

                            if (mCaptureFragment != null) {
                                // Capture Frag exists, so this is the server!
                                mCaptureFragment.setBluetoothService(mBluetoothService);
                            }

                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            System.out.println("Disconnected");
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    // Message read is sent to both client and server even though client broadcasts
                    if(!isBroadcasting) {
                        InputStream imgStream = (InputStream) msg.obj;
                        // Pass the image stream object directly to the MJPEG viewer
                        publishStreamToView(imgStream);
                    }
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();

                        getView().findViewById(R.id.mjpeg_surface).setVisibility(View.VISIBLE);

                        getView().findViewById(R.id.relativeLayout).setVisibility(View.INVISIBLE);
                        getView().findViewById(R.id.frameLayout).setVisibility(View.INVISIBLE);

                        setHasOptionsMenu(false);

                        mMovieSurface = (MjpegView) getView().findViewById(R.id.mjpeg_surface);
                        if(mMovieSurface != null) {
                            mMovieSurface.setResolution(DISPLAY_WIDTH,DISPLAY_HEIGHT);
                        }
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

}
