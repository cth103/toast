package net.carlh.toast;

public class Fragment extends android.support.v4.app.Fragment {

    public void update() {}

    public void onResume() {
        super.onResume();
        update();
    }

    protected State getState() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) {
            return null;
        }
        
        return activity.getState();
    }

    protected boolean getConnected() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) {
            return false;
        }

        return activity.getConnected();
    }

    protected boolean getEnabled() {
        return getState() != null && getState().getEnabled();
    }
}
                    
