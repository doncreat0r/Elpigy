package com.bobrik.elpigy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;


public class Elpigy extends Activity implements View.OnClickListener {
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_PARAMETERS = 3;

	//public static final int ORIENTATION_SENSOR    = 0;
	public static final int ORIENTATION_PORTRAIT  = 1;
	public static final int ORIENTATION_LANDSCAPE = 2;

    public static final int REQUEST_MODE_DATA = 1;
    public static final int REQUEST_MODE_OSA = 2;

    // Name of the connected device
    private String mConnectedDeviceName = null;

    /**
     * Set to true to add debugging code and logging.
     */
    public static final boolean DEBUG = false;

    /**
     * The tag we use when logging, so that our messages can be distinguished
     * from other messages in the log. Public because it's used by several
     * classes.
     */
	public static final String LOG_TAG = "Elpigy";

    // Message types sent from the BluetoothReadService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_VALUE = 6;

    private static final int DISPLAY_DATA = 7;

    private static final int UPDATE_MODE_VALUES = 0;
    private static final int UPDATE_MODE_ALL = 1;
    private static final int UPDATE_MODE_TAGS = 2;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
	
	private BluetoothAdapter mBluetoothAdapter = null;

    private static BluetoothSerialService mSerialService = null;
	
	private boolean mEnablingBT;
    private int requestsPending = 0;
    private boolean mAllowInsecureConnections = true;
    private boolean mAutoConnect = false;
    private boolean mActivityVisible = false;
    private boolean mTripLogged = true;
    private boolean mLogData = false;

    private FileOutputStream mLogDataFile = null;
    private String mAutoConnectMac = null;

    private int mScreenOrientation = 0;
    private int mRequestMode = REQUEST_MODE_DATA;

    private static final String ALLOW_INSECURE_CONNECTIONS_KEY = "allowinsecureconnections";
    private static final String AUTO_CONNECT_KEY = "autoconnect";
    private static final String AUTO_CONNECT_MAC = "autoconnectmac";
    private static final String SMALL_VIEW_TAG = "smallviewtag_%d";
    private static final String BIG_VIEW_TAG = "bigviewtag_%d";
    private static final String SCREENORIENTATION_KEY = "screenorientation";

    public static final int COLOR_WHITE = 0xffffffff;
    public static final int COLOR_BLACK = 0xff000000;
//    public static final int BLUE = 0xff344ebd;
    public static final int DARK_BLUE = 0xFF0000BA;
    public static final int DARK_RED = 0xFFBA0000;
    public static final int LIGHT_RED = 0xFFFF0000;
    public static final int COLOR_PETROL = 0xFFFFFF00;
    public static final int COLOR_LPG = 0xFF00BAFF;
    public static final int COLOR_WAIT = 0xFFFFBB00;
    public static final int COLOR_IGNITION = 0xFF997000;
    public static final int COLOR_ERROR = 0xFFFF00BB;
    public static final int COLOR_YELLOW = 0xffffff00;
    public static final int COLOR_GREEN = 0xff02ff00;

    private SharedPreferences mPrefs;
	
    private MenuItem mMenuItemConnect;

    private Dialog mAboutDialog;
    private Dialog mOSADialog;
    private Dialog mPADialog = null;
    private ResponseParser mParser;

    // Main hor/vert. layout
    private LinearLayout mLayout;
    private LinearLayout mLayoutBig;
    private LinearLayout mLayoutSmall;

    // label elements in 2x2
    private LinearLayout mLayoutBigs[] = new LinearLayout[4];
    // label elements in 3x3
    private LinearLayout mLayoutSmalls[] = new LinearLayout[9];

    private TextView tvPulse;

    /*
     * Not sure if it's a good way to put the onClick into main class
     * Tag contains currently chosen value (i.e. STFT, LTFT, Load or TA for the top-middle item in small views)
     */
    public void onClick(View v) {
        if (v.getTag() != null && v.getParent() != null) {
            // increment Tag on click (only 0..3 values are allowed)
            int i = ((Integer) v.getTag()) + 1;
            if (i > 3) i = 0;
            v.setTag(i);
            // update a textview (text + caption + unit) with a new tag
            if (v.getParent().getParent() == mLayoutBig)
                updateBigTextViews((LinearLayout) v, -1, UPDATE_MODE_ALL);
            else
                if (v.getParent().getParent() == mLayoutSmall)
                    updateSmallTextViews((LinearLayout) v, -1, UPDATE_MODE_ALL);
        }
    }

    /*
     * fill layout arrays with items that contains 3 textviews as children
     */
    private void updateLayoutArray(LinearLayout parentLayout, LinearLayout[] layoutArray) {
        // 1st layout children are rows
        for (int i=0; i < parentLayout.getChildCount(); i++) {
            LinearLayout layTmp = (LinearLayout) parentLayout.getChildAt(i);
            // 2nd layout children are cols
            for (int j=0; j < layTmp.getChildCount(); j++) {
                int idx = i * layTmp.getChildCount() + j;
                if (idx < layoutArray.length) { // check if we still fit into the array
                    layoutArray[idx] = (LinearLayout) layTmp.getChildAt(j);
                    layoutArray[idx].setOnClickListener(this);
                }
            }
        }
    }

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		if (DEBUG)
			Log.e(LOG_TAG, "+++ ON CREATE +++");

        //Locale.setDefault(new Locale("en", "US"));
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Set up the window layout
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        mParser = new ResponseParser();

        mLayout = (LinearLayout) findViewById(R.id.mlayout);
        mLayoutBig = (LinearLayout) findViewById(R.id.layout_big);
        mLayoutSmall = (LinearLayout) findViewById(R.id.layout_small);
        tvPulse = (TextView) findViewById(R.id.tvPulse);

        updateLayoutArray(mLayoutBig, mLayoutBigs);
        if (DEBUG) Log.e(LOG_TAG, "big done");
        updateLayoutArray(mLayoutSmall, mLayoutSmalls);
        if (DEBUG) Log.e(LOG_TAG, "small done");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            //finishDialogNoBluetooth();
            //return;
        } else
            mSerialService = new BluetoothSerialService(this, mHandlerBT, mParser);

		if (DEBUG)
			Log.e(LOG_TAG, "+++ DONE IN ON CREATE +++");
    }

	@Override
	public void onStart() {
		super.onStart();
        if (DEBUG)
			Log.e(LOG_TAG, "++ ON START ++");
		mEnablingBT = false;
	}

	@Override
    public synchronized void onResume() {
		super.onResume();

		if (DEBUG) Log.e(LOG_TAG, "+ ON RESUME +");

        mActivityVisible = true;

        readPrefs();
        updateScreenLayout(getResources().getConfiguration().orientation);
        updateValues(UPDATE_MODE_ALL);
		
		if (!mEnablingBT) { // If we are turning on the BT we cannot check if it's enable
		    if ( (mBluetoothAdapter != null)  && (!mBluetoothAdapter.isEnabled()) ) {
			
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.alert_dialog_turn_on_bt)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.alert_dialog_warning_title)
                    .setCancelable( false )
                    .setPositiveButton(R.string.alert_dialog_yes, new DialogInterface.OnClickListener() {
                    	public void onClick(DialogInterface dialog, int id) {
                    		mEnablingBT = true;
                    		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);			
                    	}
                    })
                    .setNegativeButton(R.string.alert_dialog_no, new DialogInterface.OnClickListener() {
                    	public void onClick(DialogInterface dialog, int id) {
                    		finishDialogNoBluetooth();            	
                    	}
                    });
                AlertDialog alert = builder.create();
                alert.show();
		    }		
		
		    if (mSerialService != null) {
		    	// Only if the state is STATE_NONE, do we know that we haven't started already
		    	if (mSerialService.getState() == BluetoothSerialService.STATE_NONE) {
		    		// Start the Bluetooth chat services
		    		mSerialService.start();
		    	}
		    }

		    if (mBluetoothAdapter != null) {
		    	updatePrefs();

                if (mAutoConnect && mAutoConnectMac != null && !mAutoConnectMac.equals(""))
                    connectDevice(mAutoConnectMac);
		    }
		}
        if (DEBUG)
            Log.e(LOG_TAG, "+ END ON RESUME +");
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateScreenLayout(newConfig.orientation);
    }

    /*
     * Reaction to screen layout change
     */
    public void updateScreenLayout(int newLayout) {
        if (mLayout == null) return;
        if (newLayout == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || newLayout == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE ||
                newLayout == ActivityInfo.SCREEN_ORIENTATION_USER)
            mLayout.setOrientation(LinearLayout.HORIZONTAL);
        if (newLayout == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || newLayout == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT)
            mLayout.setOrientation(LinearLayout.VERTICAL);
        //Toast.makeText(getApplicationContext(), String.valueOf(newLayout), Toast.LENGTH_SHORT).show();
    }

	@Override
	public synchronized void onPause() {
		super.onPause();

        mActivityVisible = false;
		if (DEBUG)
			Log.e(LOG_TAG, "- ON PAUSE -");

	}

    @Override
    public void onStop() {
        super.onStop();

        writePrefs();
        if (mSerialService != null)
            mSerialService.stop();
        try {
            if (mLogDataFile != null)
                mLogDataFile.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Exception while closing data log file");
        }
        if(DEBUG)
        	Log.e(LOG_TAG, "-- ON STOP --");
    }


	@Override
	public void onDestroy() {
		super.onDestroy();
		if (DEBUG)
			Log.e(LOG_TAG, "--- ON DESTROY ---");
		
        if (mSerialService != null)
        	mSerialService.stop();
        
	}

    private void readPrefs() {
        mAllowInsecureConnections = mPrefs.getBoolean( ALLOW_INSECURE_CONNECTIONS_KEY, mAllowInsecureConnections);
        mAutoConnect = mPrefs.getBoolean( AUTO_CONNECT_KEY, mAutoConnect);
        mAutoConnectMac = mPrefs.getString( AUTO_CONNECT_MAC, "");
		mScreenOrientation = readIntPref(SCREENORIENTATION_KEY, mScreenOrientation, 2);

        for (int i=0; i< mLayoutSmalls.length; i++)
            if (mLayoutSmalls[i] != null)
                mLayoutSmalls[i].setTag(mPrefs.getInt(String.format(SMALL_VIEW_TAG, i), 0));
        for (int i=0; i< mLayoutBigs.length; i++)
            if (mLayoutBigs[i] != null)
                mLayoutBigs[i].setTag(mPrefs.getInt(String.format(BIG_VIEW_TAG, i), 0));
        //Toast.makeText(getApplicationContext(), "prefs read", Toast.LENGTH_SHORT).show();
    }

    private void writePrefs() {
        //Toast.makeText(getApplicationContext(), "writing prefs", Toast.LENGTH_SHORT).show();
        SharedPreferences.Editor editor = mPrefs.edit();
        for (int i=0; i< mLayoutSmalls.length; i++)
            if (mLayoutSmalls[i] != null && mLayoutSmalls[i].getTag() != null)
                editor.putInt(String.format(SMALL_VIEW_TAG, i), (Integer) mLayoutSmalls[i].getTag());
        for (int i=0; i< mLayoutBigs.length; i++)
            if (mLayoutBigs[i] != null)
                editor.putInt(String.format(BIG_VIEW_TAG, i), (Integer) mLayoutBigs[i].getTag());
        editor.apply();
    }

    private void updatePrefs() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        mSerialService.setAllowInsecureConnections(mAllowInsecureConnections);
        
		switch (mScreenOrientation) {
		case ORIENTATION_PORTRAIT:
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			break;
		case ORIENTATION_LANDSCAPE:
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			break;
		default:
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		}
    }

    private int readIntPref(String key, int defaultValue, int maxValue) {
        int val;
        try {
            val = Integer.parseInt( mPrefs.getString(key, Integer.toString(defaultValue)) );
        } catch (NumberFormatException e) {
            val = defaultValue;
        }
        val = Math.max(0, Math.min(val, maxValue));
        return val;
    }
    
	public int getConnectionState() {
		return mSerialService.getState();
	}

    public void send(byte[] out) {
    	if ( mSerialService != null && out.length > 0 ) {
    		mSerialService.write(out);
    	}
    }

    /*
     * Update texts of the value view and title/unit views if needed
     */
    private void updateViewCaptions(boolean needUpdate, LinearLayout ll, int captionId, int unitId, String value) {
        // ll is a parent layout for 3 textviews, first check if those 3 views are available
        if (ll.getChildAt(0) instanceof TextView && ll.getChildAt(1) instanceof TextView && ll.getChildAt(2) instanceof TextView) {
            if (needUpdate) {
                ((TextView) ll.getChildAt(0)).setText(getString(captionId));
                ((TextView) ll.getChildAt(2)).setText(getString(unitId));
            }
            ((TextView) ll.getChildAt(1)).setText(value);
        }
    }

    private void setViewBackground(LinearLayout ll, boolean Condition, int TrueColor, int FalseColor) {
        if (Condition)
            ll.setBackgroundColor(TrueColor);
        else
            ll.setBackgroundColor(FalseColor);
    }

    private void setViewColor(LinearLayout ll, boolean Condition, int TrueColor, int FalseColor) {
        if (ll.getChildAt(1) instanceof TextView) {
            if (Condition)
                ((TextView) ll.getChildAt(1)).setTextColor(TrueColor);
            else
                ((TextView) ll.getChildAt(1)).setTextColor(FalseColor);
        }
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
        // if no specified idx, then look for the specified view in the array
        if (idx < 0) {
            for (int i = 0; i < mLayoutSmalls.length; i++)
                if (mLayoutSmalls[i] == ll) idx = i;
        }
        switch (idx) {
            case ResponseParser.CELL_SMALL_1_1:
                updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.obd_speed, R.string.unit_speed, String.valueOf(mParser.OBDSpeed));
                break;
            case ResponseParser.CELL_SMALL_1_2:
                switch (newType) {
                    case 0:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.obd_STFT, R.string.unit_percent, String.format(Locale.ENGLISH, "%2.1f", mParser.OBDSTFT));
                        break;
                    case 1:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.obd_LTFT, R.string.unit_percent, String.format(Locale.ENGLISH, "%2.1f", mParser.OBDLTFT));
                        mParser.LPGLTFTChanged = false;
                        break;
                    case 2:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.obd_load, R.string.unit_percent, String.format(Locale.ENGLISH, "%2.1f", mParser.OBDLoad));
                        break;
                    case 3:
                        updateViewCaptions(updateMode > UPDATE_MODE_VALUES, ll, R.string.obd_ta, R.string.unit_degree, String.format(Locale.ENGLISH, "%2.1f", mParser.OBDTA));
                        break;
                }
                if (mParser.LPGLTFTChanged) {
                    setViewBackground(ll, mParser.LPGLTFTChanged, DARK_RED, 0x00);
                } else {
                    setViewBackground(ll, (mParser.LPGerrBits & 0x10) != 0x10, DARK_BLUE, 0x00);
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
                    newType = (mParser.LPGStatus > 4 && (mParser.LPGerrBits & 0x8) == 0)? 0 : 1;
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
            for (int i = 0; i < mLayoutBigs.length; i++)
                if (mLayoutBigs[i] == ll) idx = i;
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

    /*
     * Called when new data packet received from the BluetoothService
     */
    private void updateValues(int updateMode) {
        for (int i = 0; i < mLayoutBigs.length; i++)
            if (mLayoutBigs[i] != null)
                updateBigTextViews(mLayoutBigs[i], i, updateMode);
        for (int i = 0; i < mLayoutSmalls.length; i++)
            if (mLayoutSmalls[i] != null)
                updateSmallTextViews(mLayoutSmalls[i], i, updateMode);
    }
    
    /*
     * The Handler that gets information back from the BluetoothService
     */
    private static class MyHandler extends Handler {
        private final WeakReference<Elpigy> mAct;

        public MyHandler(Elpigy activity) {
            mAct = new WeakReference<Elpigy>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Elpigy act = mAct.get();
            if (act != null) {
                switch (msg.what) {
                    case MESSAGE_STATE_CHANGE:
                        if (DEBUG) Log.i(LOG_TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                        switch (msg.arg1) {
                            case BluetoothSerialService.STATE_CONNECTED:
                                if (act.mMenuItemConnect != null) {
                                    act.mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
                                    act.mMenuItemConnect.setTitle(R.string.disconnect);
                                }

                                act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                act.writeLOG(act.getString(R.string.log_started));
                                Toast.makeText(act.getApplicationContext(), act.getString(R.string.toast_connected_to) + " "
                                        + act.mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                                act.sendGetDataRequest(); // to update RARE data
                                sendEmptyMessageDelayed(DISPLAY_DATA, 1000);
                                break;

                            case BluetoothSerialService.STATE_CONNECTING:
                                act.mLayout.setBackgroundColor(COLOR_BLACK);
                                // mTitle.setText(R.string.title_connecting);
                                Toast.makeText(act.getApplicationContext(), R.string.title_connecting, Toast.LENGTH_SHORT).show();
                                break;

                            case BluetoothSerialService.STATE_LISTEN:
                            case BluetoothSerialService.STATE_NONE:
                                act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                if (act.mMenuItemConnect != null) {
                                    act.mMenuItemConnect.setIcon(android.R.drawable.ic_menu_search);
                                    act.mMenuItemConnect.setTitle(R.string.connect);
                                }
                                act.mLayout.setBackgroundColor(COLOR_BLACK);

                                //  mTitle.setText(R.string.title_not_connected);

                                break;
                        }
                        break;
                    case MESSAGE_WRITE:
                        break;

                    case MESSAGE_READ:
                        try {
                            if ((act.mParser.LPGerrBits & 0x4) > 0) {
                                act.updatePADialog();
                            } else {
                                if (act.mPADialog != null)
                                    act.mPADialog.dismiss();
                            }
                            // data already in mParser.DATA
                            if (act.mParser.LPGRPM > 1000)  act.mTripLogged = false;
                            act.requestsPending = 0;
                            if (!act.mTripLogged && act.mParser.PETAvgInjTime == 0 && act.mParser.LPGRPM < 1000) {
                                act.mTripLogged = true;
                                int LPGTripTime = Double.valueOf(act.mParser.LPGTripTime * 10).intValue();
                                int PETTripTime = Double.valueOf(act.mParser.PETTripTime * 10).intValue();
                                act.writeLOG(String.format(act.getString(R.string.log_tripdone),
                                        act.mParser.LPGTripSpent / 10000000.0,
                                        act.mParser.LPGTripDist,
                                        TimeUnit.MILLISECONDS.toHours(LPGTripTime),
                                        (TimeUnit.MILLISECONDS.toMinutes(LPGTripTime) % 60),
                                        act.mParser.LPGInTank,
                                        act.mParser.PETTripSpent / 10000000.0,
                                        act.mParser.PETTripDist,
                                        TimeUnit.MILLISECONDS.toHours(PETTripTime),
                                        (TimeUnit.MILLISECONDS.toMinutes(PETTripTime) % 60),
                                        act.mParser.PETInTank,
                                        act.mParser.OutsideTemp
                                ));
                                Toast.makeText(act.getApplicationContext(), act.getString(R.string.toast_trip_logged), Toast.LENGTH_SHORT).show();
                            }

                            if (act.mActivityVisible) {
                                if (act.tvPulse.getCurrentTextColor() == 0xFFFFFFFF)
                                    act.tvPulse.setTextColor(0xFFA0A0A0);
                                else
                                    act.tvPulse.setTextColor(0xFFFFFFFF);
                                act.updateValues((act.mParser.LPGStatus != act.mParser.LPGStatus_old || act.mParser.LPGerrBits_old != (act.mParser.LPGerrBits & 0xF)) ? UPDATE_MODE_TAGS : UPDATE_MODE_VALUES);
                                act.mParser.LPGStatus_old = act.mParser.LPGStatus;
                                act.mParser.LPGerrBits_old = act.mParser.LPGerrBits & 0xF;  // count only 4 bits
                                act.updateOSADialog(false);
                                // data log
                                if (act.mLogData) {
                                    act.LogData();
                                    act.tvPulse.setTextColor(COLOR_GREEN);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "MESSAGE_READ() failed", e);
                        }

                        break;

                    case MESSAGE_VALUE:
                        act.tvPulse.setText( String.format(Locale.ENGLISH, "%d,%d,%d", msg.arg1, msg.arg2, act.mParser.packetType)); // packettype is from prev.packet!
                        break;

                    case MESSAGE_DEVICE_NAME:
                        // save the connected device's name
                        act.mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                        Toast.makeText(act.getApplicationContext(), act.getString(R.string.toast_connected_to) + " "
                                + act.mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                        break;
                    case MESSAGE_TOAST:
                        Toast.makeText(act.getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                        break;
                    case DISPLAY_DATA:
                        try {
                            if (mSerialService.getState() == BluetoothSerialService.STATE_CONNECTED) {
                                act.mLayout.setBackgroundColor(((act.requestsPending > 3) ? DARK_RED : 0xFF000000)); // no data in 5 sec - error indication
                                if (!msg.getData().containsKey(ParametersActivity.REQUEST)) {
                                    if (act.requestsPending > 1)
                                        act.sendGetDataRequest(); // if no data since last 2 sec - send another GetData request
                                    if (act.mActivityVisible) act.requestsPending++;
                                } else {
                                    act.sendParameterRequest(msg);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "DISPLAY_DATA() failed", e);
                        }
                        sendEmptyMessageDelayed(DISPLAY_DATA, 1000);
                        break;
                }
            }
        }
    }

    private final MyHandler mHandlerBT = new MyHandler(this);

    private byte[] toBytes(int i)
    {
        byte[] result = new byte[4];

        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i /*>> 0*/);

        return result;
    }

    private void sendGetDataRequest() {
        switch (mRequestMode) {
            case REQUEST_MODE_OSA:
                send(ResponseParser.REQUEST_GET_OSA);
                break;
            default:
                send(ResponseParser.REQUEST_GET_DATA);
                break;
        }
    }

    private void sendParameterRequest(Message msg) {
       // Toast.makeText(getApplicationContext(), msg.getData().getString("request"), Toast.LENGTH_SHORT).show();
        Double tmp;
        int flow;
        byte[] bytes;

        switch (msg.getData().getInt(ParametersActivity.REQUEST)) {
            case (ParametersActivity.REQUEST_ADD_LPG):
                tmp = Double.valueOf(msg.getData().getString(ParametersActivity.PARAM)) * 10000000.0;

                bytes = toBytes(tmp.intValue());
                ResponseParser.REQUEST_ADD_LPG[2] = bytes[3];
                ResponseParser.REQUEST_ADD_LPG[3] = bytes[2];
                ResponseParser.REQUEST_ADD_LPG[4] = bytes[1];
                ResponseParser.REQUEST_ADD_LPG[5] = bytes[0];

                mParser.UpdateChecksum(ResponseParser.REQUEST_ADD_LPG);
                send(ResponseParser.REQUEST_ADD_LPG);

                writeLOG(String.format(getString(R.string.log_lpgfill), tmp / 10000000.0, mParser.LPGInTank));
                break;
            case (ParametersActivity.REQUEST_ADD_PET):
                tmp = Double.valueOf(msg.getData().getString(ParametersActivity.PARAM)) * 10000000.0;

                bytes = toBytes(tmp.intValue());
                ResponseParser.REQUEST_ADD_PET[2] = bytes[3];
                ResponseParser.REQUEST_ADD_PET[3] = bytes[2];
                ResponseParser.REQUEST_ADD_PET[4] = bytes[1];
                ResponseParser.REQUEST_ADD_PET[5] = bytes[0];

                mParser.UpdateChecksum(ResponseParser.REQUEST_ADD_PET);
                send(ResponseParser.REQUEST_ADD_PET);
                writeLOG(String.format(getString(R.string.log_petfill), tmp / 10000000.0, mParser.PETInTank));
                break;
            case (ParametersActivity.REQUEST_SET_LPG):
                flow = Integer.valueOf(msg.getData().getString(ParametersActivity.PARAM));

                ResponseParser.REQUEST_SET_LPG_FLOW[2] = (byte) flow;
                mParser.UpdateChecksum(ResponseParser.REQUEST_SET_LPG_FLOW);
                send(ResponseParser.REQUEST_SET_LPG_FLOW);
                break;
            case (ParametersActivity.REQUEST_SET_PET):
                flow = Integer.valueOf(msg.getData().getString(ParametersActivity.PARAM));

                ResponseParser.REQUEST_SET_PET_FLOW[2] = (byte) flow;
                mParser.UpdateChecksum(ResponseParser.REQUEST_SET_PET_FLOW);
                send(ResponseParser.REQUEST_SET_PET_FLOW);
                break;
            case (ParametersActivity.REQUEST_SET_SPEED_CORR):
                flow = Integer.valueOf(msg.getData().getString(ParametersActivity.PARAM));

                ResponseParser.REQUEST_SET_SPEED_CORR[2] = (byte) flow;
                mParser.UpdateChecksum(ResponseParser.REQUEST_SET_SPEED_CORR);
                send(ResponseParser.REQUEST_SET_SPEED_CORR);
                break;
            case (ParametersActivity.REQUEST_RESET_TRIP):
                send(ResponseParser.REQUEST_RESET_TRIP);
                break;
            case (ParametersActivity.REQUEST_TOGGLE_LOG_DATA):
                this.mLogData = !this.mLogData;
                break;
        }
        Toast.makeText(getApplicationContext(), getString(R.string.msg_sent), Toast.LENGTH_SHORT).show();
    }
    
    public void finishDialogNoBluetooth() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_dialog_no_bt)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setTitle(R.string.app_name)
        .setCancelable( false )
        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       finish();            	
                   }
               });
        AlertDialog alert = builder.create();
        alert.show(); 
    }

    private void connectDevice(String address) {
        // Get the BluetoothDevice object
        if (getConnectionState() == BluetoothSerialService.STATE_NONE) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            // Attempt to connect to the device
            mSerialService.connect(device);
        }
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(DEBUG) Log.d(LOG_TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        
        case REQUEST_CONNECT_DEVICE:

            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // put mac to preferences
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putString(AUTO_CONNECT_MAC, address);
                editor.apply();

                connectDevice(address);
            }
            break;

        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode != Activity.RESULT_OK) {
                Log.d(LOG_TAG, "BT not enabled");
                
                finishDialogNoBluetooth();                
            }
            break;

        case REQUEST_PARAMETERS:
            if (resultCode == Activity.RESULT_OK) {
                Message msg = new Message();
                msg.what = DISPLAY_DATA;
                msg.setData(data.getExtras());
                mHandlerBT.removeMessages(DISPLAY_DATA);
                mHandlerBT.sendMessageDelayed(msg, 1000);
            }
            break;
        }


    }

/*    private boolean isSystemKey(int keyCode, KeyEvent event) {
        return event.isSystem();
    }
*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        mMenuItemConnect = menu.getItem(0);
        // don't allow connect when there's no Bluetooth (running in emulator)
        mMenuItemConnect.setEnabled(mBluetoothAdapter != null);
        //mMenuItemStartStopRecording = menu.getItem(3);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.connect:
        	
        	if (getConnectionState() == BluetoothSerialService.STATE_NONE) {
        		// Launch the DeviceListActivity to see devices and do scan
        		Intent serverIntent = new Intent(this, DeviceListActivity.class);
        		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        	}
        	else
            	if (getConnectionState() == BluetoothSerialService.STATE_CONNECTED) {
            		mSerialService.stop();
		    		mSerialService.start();
            	}
            return true;

        case R.id.parameters:
            doParameters();
            return true;
        case R.id.preferences:
        	doPreferences();
            return true;
        case R.id.osa_table:
            Toast.makeText(getApplicationContext(), "OSA reading...", Toast.LENGTH_SHORT).show();
            showOSADialog();
            return true;
        case R.id.menu_about:
            //updatePADialog();
        	showAboutDialog();
            return true;
        }
        return false;
    }

    private void doParameters() {
        Intent serverIntent = new Intent(this, ParametersActivity.class);
        serverIntent.putExtra("LPG", String.valueOf(mParser.LPGinjFlow));
        serverIntent.putExtra("PET", String.valueOf(mParser.PETinjFlow));
        serverIntent.putExtra("SPEEDCORR", String.valueOf(mParser.SpeedCorr));
        startActivityForResult(serverIntent, REQUEST_PARAMETERS);
    }

    private void doPreferences() {
        startActivity(new Intent(this, TermPreferences.class));
    }
    
/*    public void doOpenOptionsMenu() {
        openOptionsMenu();
    }
*/
    private void showOSADialog() {
        mOSADialog = new Dialog(Elpigy.this);
        mOSADialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mOSADialog.setContentView(R.layout.osa_table);
        mRequestMode = REQUEST_MODE_OSA;
        mParser.ClearOSA();

        mOSADialog.setOnDismissListener( new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                mRequestMode = REQUEST_MODE_DATA;
                sendGetDataRequest();
            }
        });
        mOSADialog.setOnCancelListener( new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                mRequestMode = REQUEST_MODE_DATA;
                sendGetDataRequest();
            }
        });

        TableLayout osa = (TableLayout) mOSADialog.findViewById(R.id.osatable);
        osa.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mOSADialog.dismiss();
            }

        });
        sendGetDataRequest();
        mOSADialog.show();
    }

    private void updateOSADialog(boolean force) {
        if (mRequestMode == REQUEST_MODE_OSA && (mParser.OSAChanged || force) ) {
            TableLayout osa = (TableLayout) mOSADialog.findViewById(R.id.osatable);

            // looping through osa_table children, skip the top one
            for (int i = 1; i < osa.getChildCount(); i++) {
                TableRow row = (TableRow) osa.getChildAt(i);
                TextView injTime = (TextView) row.getChildAt(0);
                // also skip the zero TextView
                for (int j = 1; j < row.getChildCount(); j++) {
                    TextView txt = (TextView) row.getChildAt(j);
                    int idx = (i-1)*5 + (j-1); // OSA value index in the table
                    int inj = Integer.valueOf(injTime.getText().toString());
                    if (idx < mParser.OSATable.length) {
                        txt.setText(String.valueOf(mParser.OSATable[idx]));
                        // this isn't working yet
                        if (mParser.LPGAvgInjTime < inj && mParser.LPGRPM > ((j+1)*1000) )
                            txt.setTextColor(COLOR_WHITE);
                        else
                            txt.setTextColor(COLOR_GREEN);
                    }
                }
            }
        }
    }

    private void updatePADialog() {

        if (mPADialog == null) {
            mPADialog = new Dialog(Elpigy.this);
            mPADialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mPADialog.setContentView(R.layout.park_assist);

            ScrollView pa = (ScrollView) mPADialog.findViewById(R.id.mainView);
            pa.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPADialog.dismiss();
                    mPADialog = null;
                }

            });
        }
        // now update with PA distances
        if (mParser != null) {
            ((TextView) mPADialog.findViewById(R.id.tvA)).setText(String.format(Locale.ENGLISH, "%1.2f", mParser.PA[0]));
            ((TextView) mPADialog.findViewById(R.id.tvB)).setText(String.format(Locale.ENGLISH, "%1.2f", mParser.PA[1]));
            ((TextView) mPADialog.findViewById(R.id.tvC)).setText(String.format(Locale.ENGLISH, "%1.2f", mParser.PA[2]));
            ((TextView) mPADialog.findViewById(R.id.tvD)).setText(String.format(Locale.ENGLISH, "%1.2f", mParser.PA[3]));
            ((PABarView) mPADialog.findViewById(R.id.barA)).setDistance(mParser.PA[0]);
            ((PABarView) mPADialog.findViewById(R.id.barB)).setDistance(mParser.PA[1]);
            ((PABarView) mPADialog.findViewById(R.id.barC)).setDistance(mParser.PA[2]);
            ((PABarView) mPADialog.findViewById(R.id.barD)).setDistance(mParser.PA[3]);
        }

        mPADialog.show();
    }

	private void showAboutDialog() {
		mAboutDialog = new Dialog(Elpigy.this);
		mAboutDialog.setContentView(R.layout.about);
		mAboutDialog.setTitle(getString(R.string.app_name) + " " + getString(R.string.app_version));
		
		Button buttonOpen = (Button) mAboutDialog.findViewById(R.id.buttonDialog);
		buttonOpen.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                mAboutDialog.dismiss();
            }
        });
		
		mAboutDialog.show();
    }

    private File ensureLogFileExists(String fileName) {
        try {
            final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + getString(R.string.app_name) + "/" );

            if (!dir.exists())
                if (!dir.mkdirs())
                    throw new IOException("Unable to create work dir");

            final File txt = new File(dir, fileName);

            if (!txt.exists())
                if (!txt.createNewFile())
                    throw new IOException("Unable to create log file");

            return txt;
        } catch (IOException e) {
            Log.e(LOG_TAG, "ensureLogFileExists() failed: ", e);
            return null;
        }
    }

    private void writeLOG(String data)
    {
        StringBuilder logText = new StringBuilder();

        try {
            String logDateTime = (new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)).format(Calendar.getInstance().getTime());

            logText.append(logDateTime);
            logText.append(" : ");
            logText.append(data);
            logText.append("\r\n");

            File txt = ensureLogFileExists("tripdata.txt");

            if (txt != null) {
                FileOutputStream fos = new FileOutputStream(txt, true);  // open in append mode

                fos.write(logText.toString().getBytes());
                fos.close();
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "writeToFile() failed", e);
        }
    }

    public void LogData()
    {
        StringBuilder logText = new StringBuilder();

        try
        {
            String logPointTime = (new SimpleDateFormat("HH:mm:ss.SSS", Locale.US)).format(Calendar.getInstance().getTime());
            String logDate = (new SimpleDateFormat("yyyy-MM-dd", Locale.US)).format(Calendar.getInstance().getTime());

            // currently hardcoded log fields
            logText.append(String.format(Locale.ENGLISH,
                    "%s;%2.2f;%2.2f;%d;%3.0f;%2.1f;%2.1f;%d;%2.2f;%2.2f;%2.2f;%2.2f;",
                    logPointTime, mParser.LPGAvgInjTime,
                    mParser.PETAvgInjTime, mParser.LPGRPM, mParser.OBDLoad, mParser.OBDSTFT, mParser.OBDLTFT,
                    mParser.OBDSpeed, mParser.OBDMap, mParser.OBDTA, mParser.LPGPcol, mParser.LPGPsys));
            logText.append(String.format(Locale.ENGLISH, "%2.1f;%2.1f;%d;%d;%2.1f",
                    mParser.LPGTgas, mParser.LPGTred, mParser.OBDECT, mParser.OBDIAT, mParser.OBDTPS));
            logText.append("\r\n");

            if (mLogDataFile == null)
            {
                File txt = ensureLogFileExists(logDate + ".csv");
                if (txt != null) {
                    mLogDataFile = new FileOutputStream(txt, true);  // open in append mode
                    if (txt.length() == 0)
                        mLogDataFile.write(getString(R.string.log_data_header).getBytes());
                        mLogDataFile.write("\r\n".getBytes());
                }

            }
            mLogDataFile.write(logText.toString().getBytes());

        } catch (IOException e) {
            Log.e(LOG_TAG, "writeToFile() failed", e);
        }

    }
}

