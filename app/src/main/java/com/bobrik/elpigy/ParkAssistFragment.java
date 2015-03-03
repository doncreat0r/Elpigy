/**
 * Created by DBobrik on 001 01.03.2015.
 */
package com.bobrik.elpigy;

import com.bobrik.elpigy.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ParkAssistFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_parkassist, container, false);

        return rootView;
    }
}