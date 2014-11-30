/*
    Copyright (C) 2014 Carl Hetherington <cth@carlh.net>

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

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
                    
