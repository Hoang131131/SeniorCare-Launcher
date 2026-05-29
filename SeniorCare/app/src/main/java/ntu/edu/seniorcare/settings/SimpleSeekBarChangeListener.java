package ntu.edu.seniorcare.settings;

import android.widget.SeekBar;

/**
 * Utility abstract class to implement only the necessary methods of SeekBar.OnSeekBarChangeListener.
 */
public abstract class SimpleSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Not used in this context
    }
}