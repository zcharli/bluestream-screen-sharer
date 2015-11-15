package che.carleton.ottawa.bluestream;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;

import che.carleton.ottawa.activity.ActivityResultBus;
import che.carleton.ottawa.activity.ActivityResultEvent;


public class MainActivity extends AppCompatActivity{

    private  boolean FLOATING_ACTION = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
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

                    popupWindow.showAsDropDown(fab, Gravity.TOP, -1050, 0);

                    /*final Button btnRecord = (Button) popupView.findViewById(R.id.button_record);
                    btnRecord.setOnClickListener(new Button.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            getSupportFragmentManager().popBackStack();
                            BluetoothCaptureFragment newFragment = new BluetoothCaptureFragment();
                            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                            transaction.replace(R.id.fragment_container, newFragment);
                            transaction.addToBackStack(null);
                            transaction.commit();
                        }
                    });*/

                    /*final Button btnReceive = (Button) popupView.findViewById(R.id.button_receive);
                    btnReceive.setOnClickListener(new Button.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            getSupportFragmentManager().popBackStack();
                            BluetoothCaptureFragment newFragment = new BluetoothCaptureFragment();
                            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                            transaction.replace(R.id.fragment_container, newFragment);
                            transaction.addToBackStack(null);
                            transaction.commit();
                        }
                    });*/

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
                            getSupportFragmentManager().popBackStack();
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

        fab.setVisibility(View.INVISIBLE);

        findViewById(R.id.enter_app).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, new BluetoothCaptureFragment());
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
}
