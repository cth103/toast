package net.carlh.toast;

public class Fragment extends android.support.v4.app.Fragment {

    protected State getState() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) {
            return null;
        }
        
        return activity.getState();
    }
}
                    
