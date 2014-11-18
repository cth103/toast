package net.carlh.toast;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class OnSwipeTouchListener implements View.OnTouchListener {

    private Activity activity;
    private Intent leftIntent;
    private Intent rightIntent;
    float start = 0;
    
    public OnSwipeTouchListener(Activity activity, Intent left, Intent right) {
        this.activity = activity;
        this.leftIntent = left;
        this.rightIntent = right;
    }
    
    public boolean onTouch(final View v, final MotionEvent event) {
        switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
            start = event.getX();
            return true;
        case MotionEvent.ACTION_UP:
            float diff = event.getX() - start;
            if (diff < -100 && rightIntent != null) {
                activity.startActivity(rightIntent);
                activity.overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_left);
                return true;
            } else if (diff > 100 && leftIntent != null) {
                activity.startActivity(leftIntent);
                activity.overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_right);
                return true;
            }
            break;
        }

        return false;
    }
}
