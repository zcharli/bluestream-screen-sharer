package che.carleton.ottawa.bluestream.Fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import che.carleton.ottawa.bluestream.Constants;
import che.carleton.ottawa.bluestream.R;
import che.carleton.ottawa.bluestream.Settings;

/**
 * Created by CZL on 12/11/2015.
 */
public class SettingsFragment extends Fragment {

    public SettingsFragment() {}

    private NumberPicker numQualityPicker;

    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container,
                             Bundle savedInstances) {


        return layoutInflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        numQualityPicker = (NumberPicker)view.findViewById(R.id.numQualityPicker);
        numQualityPicker.setMaxValue(98);
        numQualityPicker.setMinValue(10);
        numQualityPicker.setValue(50);
        numQualityPicker.setWrapSelectorWheel(false);
        numQualityPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                // May be an unsafe way since theres ABSOLUTELY no bounds checking
                // Only set max and min by the UI
                Settings.QUALITY_LEVEL = newVal;
            }
        });
    }
}
