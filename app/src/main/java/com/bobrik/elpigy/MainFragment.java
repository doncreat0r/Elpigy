package com.bobrik.elpigy;

/**
 * Created by DBobrik on 001 01.03.2015.
 */
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.app.Activity;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainFragment extends Fragment {
    private static final int UPDATE_MODE_VALUES = 0;
    private static final int UPDATE_MODE_ALL = 1;
    private static final int UPDATE_MODE_TAGS = 2;
    public static final int DARK_RED = 0xFFBA0000;
    public static final int LIGHT_RED = 0xFFFF0000;
    public static final int COLOR_PETROL = 0xFFFFFF00;
    public static final int COLOR_LPG = 0xFF00BAFF;
    public static final int COLOR_WAIT = 0xFFFFBB00;
    public static final int COLOR_IGNITION = 0xFF997000;
    public static final int COLOR_ERROR = 0xFFFF00BB;
    public static final int COLOR_YELLOW = 0xffffff00;
    public static final int COLOR_GREEN = 0xff02ff00;

    private ResponseParser mParser;

    // Main hor/vert. layout
    private LinearLayout mLayout;

    // label elements in 2x2
    private LinearLayout layBigs[] = new LinearLayout[4];

    // label elements in 3x3
    private LinearLayout laySmalls[] = new LinearLayout[9];

    private TextView tvPulse;

    // newInstance constructor for creating fragment with arguments
    public static MainFragment newInstance(ResponseParser parser) {
        MainFragment fragmentM = new MainFragment();
        Bundle args = new Bundle();
        fragmentM.mParser = parser;
        //args.putInt("someInt", page);
        //args.putString("someTitle", title);
        fragmentM.setArguments(args);
        return fragmentM;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //page = getArguments().getInt("someInt", 0);
        //title = getArguments().getString("someTitle");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View V = inflater.inflate(R.layout.fragment_main, container, false);

        mLayout = (LinearLayout) V.findViewById(R.id.mlayout);
        LinearLayout layBig = (LinearLayout) V.findViewById(R.id.layout_big);
        LinearLayout laySmall = (LinearLayout) V.findViewById(R.id.layout_small);
        tvPulse = (TextView) V.findViewById(R.id.tvPulse);

        return V;
    }

    private void updateViewCaptions(boolean needUpdate, LinearLayout ll, int captionId, int unitId, String value) {
        TextView tv;

        if (ll.getChildAt(0) instanceof TextView && ll.getChildAt(1) instanceof TextView && ll.getChildAt(2) instanceof TextView) {
            if (needUpdate) {
                tv = (TextView) ll.getChildAt(0);
                tv.setText(getString(captionId));
                tv = (TextView) ll.getChildAt(2);
                tv.setText(getString(unitId));
            }
            tv = (TextView) ll.getChildAt(1);
            tv.setText(value);
        }
    }

    private void setViewBackground(LinearLayout ll, boolean Condition, int TrueColor, int FalseColor) {
        ll.setBackgroundColor((Condition) ? TrueColor : FalseColor);
    }

    private void setViewColor(LinearLayout ll, boolean Condition, int TrueColor, int FalseColor) {
        if (ll.getChildAt(1) instanceof TextView)
            ((TextView) ll.getChildAt(1)).setTextColor((Condition) ? TrueColor : FalseColor);
    }

    /*
     * @param updateMode update explicitly or based on current mode or don't update
     */
    private void updateSmallTextViews(LinearLayout ll, int idx, int updateMode) {
        int newType = -1;

        if (ll == null || ll.getChildCount() < 3)
            return;
        if (ll.getTag() != null)
            newType = (Integer)ll.getTag();
        if (idx < 0) {
            for (int i = 0; i < laySmalls.length; i++)
                if (laySmalls[i] == ll) idx = i;
        }
        switch (idx) {
            case ResponseParser.CELL_SMALL_1_1:
                updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.obd_speed, R.string.unit_speed, String.valueOf(mParser.OBDSpeed));
                break;
            case ResponseParser.CELL_SMALL_1_2:
                switch (newType) {
                    case 0:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.obd_STFT, R.string.unit_percent, String.format(Locale.ENGLISH, "%2.2f", mParser.OBDSTFT));
                        break;
                    case 1:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.obd_LTFT, R.string.unit_percent, String.format(Locale.ENGLISH, "%2.2f", mParser.OBDLTFT));
                        break;
                    case 2:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.obd_load, R.string.unit_percent, String.format(Locale.ENGLISH, "%2.2f", mParser.OBDLoad));
                        break;
                    case 3:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.obd_ta, R.string.unit_degree, String.format(Locale.ENGLISH, "%2.1f", mParser.OBDTA));
                        break;
                }
                break;
            case ResponseParser.CELL_SMALL_1_3:
                updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.out_temp, R.string.unit_degreeC, String.format(Locale.ENGLISH, "%2.1f", mParser.OutsideTemp));
                break;
            case ResponseParser.CELL_SMALL_2_1:
                // set TAG if updateMode allows
                if (updateMode == UPDATE_MODE_TAGS) {
                    newType = (mParser.LPGStatus > 4)? 0 : 1;
                    ll.setTag(newType);
                }
                switch (newType) {
                    case 0:
                    case 2:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.lpg_in_tank, R.string.unit_liter, String.format(Locale.ENGLISH, "%2.1f", mParser.LPGInTank));
                        setViewBackground(ll, mParser.LPGInTank < 10.0, DARK_RED, 0x00);
                        setViewColor(ll, true, COLOR_LPG, COLOR_LPG);
                        break;
                    case 1:
                    case 3:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.pet_in_tank, R.string.unit_liter, String.format(Locale.ENGLISH, "%2.1f", mParser.PETInTank));
                        setViewBackground(ll, mParser.PETInTank < 10.0, DARK_RED, 0x00);
                        setViewColor(ll, true, COLOR_PETROL, COLOR_PETROL);
                        break;
                }
                break;
            case ResponseParser.CELL_SMALL_2_2:
                switch (newType) {
                    case 0:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.lpg_psys, R.string.unit_bar, String.format(Locale.ENGLISH, "%1.2f", mParser.LPGPsys));
                        break;
                    case 1:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.lpg_pcol, R.string.unit_bar, String.format(Locale.ENGLISH, "%1.2f", mParser.LPGPcol));
                        break;
                    case 2:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.obd_map, R.string.unit_bar, String.format(Locale.ENGLISH, "%1.2f", mParser.OBDMap));
                        break;
                    case 3:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.obd_tps, R.string.unit_percent, String.format(Locale.ENGLISH, "%2.1f", mParser.OBDTPS));
                        break;
                }
                break;
            case ResponseParser.CELL_SMALL_2_3:
                // if injector time deviation - set red background
                setViewBackground(ll, (mParser.LPGerrBits & 0x3) > 0, DARK_RED, 0x00);
                // set TAG if updateMode allows
                if (updateMode == UPDATE_MODE_TAGS) {
                    newType = (mParser.LPGStatus > 4)? 0 : 1;
                    ll.setTag(newType);
                }
                switch (newType) {
                    case 0:
                    case 2:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.lpg_inj_time, R.string.unit_ms, String.format(Locale.ENGLISH, "%2.2f", mParser.LPGAvgInjTime));
                        setViewColor(ll, (mParser.LPGerrBits & 0x3) > 0, LIGHT_RED, COLOR_LPG);
                        break;
                    case 1:
                    case 3:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.pet_inj_time, R.string.unit_ms, String.format(Locale.ENGLISH, "%2.2f", mParser.PETAvgInjTime));
                        switch (mParser.LPGStatus) {
                            case 1: setViewColor(ll, (mParser.LPGerrBits & 0x3) > 0, LIGHT_RED, COLOR_IGNITION); break;
                            case 2: setViewColor(ll, (mParser.LPGerrBits & 0x3) > 0, LIGHT_RED, COLOR_ERROR); break;
                            case 4: setViewColor(ll, (mParser.LPGerrBits & 0x3) > 0, LIGHT_RED, COLOR_WAIT); break;
                            default: setViewColor(ll, (mParser.LPGerrBits & 0x3) > 0, LIGHT_RED, COLOR_PETROL); break;
                        }
                        break;
                }
                break;
            case ResponseParser.CELL_SMALL_3_1:
                setViewColor(ll, true, (newType < 2)?COLOR_YELLOW:COLOR_GREEN, COLOR_YELLOW);
                switch (newType) {
                    case 0:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.lpg_tred, R.string.unit_degreeC, String.format(Locale.ENGLISH, "%3.1f", mParser.LPGTred));
                        break;
                    case 1:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.lpg_tgas, R.string.unit_degreeC, String.format(Locale.ENGLISH, "%3.1f", mParser.LPGTgas));
                        break;
                    case 2:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.obd_ect, R.string.unit_degreeC, String.format(Locale.ENGLISH, "%d", mParser.OBDECT));
                        break;
                    case 3:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.obd_iat, R.string.unit_degreeC, String.format(Locale.ENGLISH, "%d", mParser.OBDIAT));
                        break;
                }
                break;
            case ResponseParser.CELL_SMALL_3_2:
                setViewColor(ll, true, (newType < 2)?COLOR_GREEN:COLOR_YELLOW, COLOR_YELLOW);
                switch (newType) {
                    case 0:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.lpg_vbat, R.string.unit_v, String.format(Locale.ENGLISH, "%.1f", mParser.LPGVbat));
                        break;
                    case 1:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.eeprom_update, R.string.unit_none, String.format(Locale.ENGLISH, "%d", mParser.EEPROMUpdateCount));
                        break;
                    case 2:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.lpg_inj_flow, R.string.unit_none, String.format(Locale.ENGLISH, "%d", mParser.LPGinjFlow));
                        break;
                    case 3:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.pet_inj_flow, R.string.unit_none, String.format(Locale.ENGLISH, "%d", mParser.PETinjFlow));
                        break;
                }
                break;
            case ResponseParser.CELL_SMALL_3_3:
                switch (newType) {
                    case 0:
                    case 2:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.lpg_rpm, R.string.unit_none, String.format(Locale.ENGLISH, "%d", mParser.LPGRPM));
                        break;
                    case 1:
                    case 3:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.obd_rpm, R.string.unit_none, String.format(Locale.ENGLISH, "%d", mParser.OBDRPM));
                        break;
                }
                break;
        }
    }

    private void updateBigTextViews(LinearLayout ll, int idx, int updateMode) {
        int newType = -1;

        if (ll == null || ll.getChildCount() < 3)
            return;
        if (ll.getTag() != null)
            newType = (Integer)ll.getTag();
        if (idx < 0) {
            for (int i = 0; i < layBigs.length; i++)
                if (layBigs[i] == ll) idx = i;
        }
        switch (idx) {
            case ResponseParser.CELL_BIG_1_1:
            case ResponseParser.CELL_BIG_1_2:
                setViewColor(ll, mParser.LPGStatus == 5, COLOR_LPG, COLOR_PETROL);
                switch (mParser.LPGStatus) {
                    case 5:
                        switch (newType) {
                            case 0:
                                updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.avg_lpg_hourly, R.string.unit_lhour, String.format(Locale.ENGLISH, "%2.1f", mParser.LPGPerHour));
                                break;
                            case 1:
                                updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.avg_lpg_short, R.string.unit_l100, String.format(Locale.ENGLISH, "%2.1f", mParser.LPGPer100Short));
                                break;
                            case 2:
                                updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.avg_lpg_long, R.string.unit_l100, String.format(Locale.ENGLISH, "%2.1f", mParser.LPGPer100Long));
                                break;
                            case 3:
                                updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.avg_lpg_trip, R.string.unit_l100, String.format(Locale.ENGLISH, "%2.1f", mParser.LPGPer100Trip));
                                break;
                        }
                        break;
                    default:
                        switch (newType) {
                            case 0:
                                updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.avg_pet_hourly, R.string.unit_lhour, String.format(Locale.ENGLISH, "%2.1f", mParser.PETPerHour));
                                break;
                            case 1:
                                updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.avg_pet_short, R.string.unit_l100, String.format(Locale.ENGLISH, "%2.1f", mParser.PETPer100Short));
                                break;
                            case 2:
                                updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.avg_pet_long, R.string.unit_l100, String.format(Locale.ENGLISH, "%2.1f", mParser.PETPer100Long));
                                break;
                            case 3:
                                updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.avg_pet_trip, R.string.unit_l100, String.format(Locale.ENGLISH, "%2.1f", mParser.PETPer100Trip));
                                break;
                        }
                        break;
                }
                break;
            case ResponseParser.CELL_BIG_2_1:
                switch (newType) {
                    case 0:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.distance_lpg, R.string.unit_km, String.format(Locale.ENGLISH, "%4.1f", mParser.LPGTripDist));
                        break;
                    case 1:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.distance_pet, R.string.unit_km, String.format(Locale.ENGLISH, "%4.1f", mParser.PETTripDist));
                        break;
                    case 2:
                    case 3:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.distance_all, R.string.unit_km, String.format(Locale.ENGLISH, "%4.1f", mParser.LPGTripDist + mParser.PETTripDist));
                        break;
                }
                break;
            case ResponseParser.CELL_BIG_2_2:
                int TripTime;
                switch (newType) {
                    case 0:
                        TripTime = Double.valueOf(mParser.LPGTripTime * 10).intValue();
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.time_lpg, R.string.unit_time,
                                String.format(Locale.ENGLISH, "%02d:%02d",
                                        TimeUnit.MILLISECONDS.toHours(TripTime),
                                        TimeUnit.MILLISECONDS.toMinutes(TripTime) % 60));
                        break;
                    case 1:
                        TripTime = Double.valueOf(mParser.PETTripTime * 10).intValue();
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.time_pet, R.string.unit_time,
                                String.format(Locale.ENGLISH, "%02d:%02d",
                                        TimeUnit.MILLISECONDS.toHours(TripTime),
                                        TimeUnit.MILLISECONDS.toMinutes(TripTime) % 60));
                        break;
                    case 2:
                    case 3:
                        TripTime = Double.valueOf((mParser.LPGTripTime+mParser.PETTripTime) * 10).intValue();
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.time_all, R.string.unit_time,
                                String.format(Locale.ENGLISH, "%02d:%02d",
                                        TimeUnit.MILLISECONDS.toHours(TripTime),
                                        TimeUnit.MILLISECONDS.toMinutes(TripTime) % 60));
                        break;
                }
                break;
        }
    }

}
