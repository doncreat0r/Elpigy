package com.bobrik.elpigy;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
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


public class Elpigy extends FragmentActivity implements
        ActionBar.TabListener {
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_PARAMETERS = 3;

	//public static final int ORIENTATION_SENSOR    = 0;
	public static final int ORIENTATION_PORTRAIT  = 1;
	public static final int ORIENTATION_LANDSCAPE = 2;


    //private static TextView mTitle;

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
//    private boolean mLocalEcho = false;
//    private int mFontSize = 9;
//    private int mColorId = 2;
//    private int mControlKeyId = 0;
    private boolean mAllowInsecureConnections = true;
    private boolean mAutoConnect = false;
    private boolean mActivityVisible = false;
    private String mAutoConnectMac = null;

    private int mScreenOrientation = 0;

    private static final String ALLOW_INSECURE_CONNECTIONS_KEY = "allowinsecureconnections";
    private static final String AUTO_CONNECT_KEY = "autoconnect";
    private static final String AUTO_CONNECT_MAC = "autoconnectmac";
    private static final String SCREENORIENTATION_KEY = "screenorientation";

//    public static final int WHITE = 0xffffffff;
//    public static final int BLACK = 0xff000000;
//    public static final int BLUE = 0xff344ebd;
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
    private MenuItem mMenuItemStartStopRecording;
    
    private Dialog mAboutDialog;
    private ResponseParser mParser;

    MyAdapter mAdapter;
    ViewPager mPager;
    // Main hor/vert. layout
    private LinearLayout mLayout;

    MainFragment fragmentMain;
    ParkAssistFragment fragmentPA;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		if (DEBUG)
			Log.e(LOG_TAG, "+++ ON CREATE +++");

        //Locale.setDefault(new Locale("en", "US"));

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Set up the window layout
        //requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        //getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        //mTitle = (TextView) findViewById(R.id.title_left_text);
        //mTitle.setText(R.string.app_name);
        //mTitle = (TextView) findViewById(R.id.title_right_text);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mParser = new ResponseParser();

        mLayout = (LinearLayout) findViewById(R.id.mlayout);
        fragmentMain = new MainFragment();

        if (DEBUG) Log.e(LOG_TAG, "small done");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            finishDialogNoBluetooth();
            return;
        }
        mSerialService = new BluetoothSerialService(this, mHandlerBT, mParser);

		if (DEBUG)
			Log.e(LOG_TAG, "+++ DONE IN ON CREATE +++");
	}

    public static class MyAdapter extends FragmentPagerAdapter {
        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            return MainFragment.newInstance(mParser);
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return "Page " + position;
        }

    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
	public void onStart() {
		super.onStart();

        //mLayout.setOrientation(LinearLayout.VERTICAL);
		if (DEBUG)
			Log.e(LOG_TAG, "++ ON START ++");
		
		mEnablingBT = false;
	}

	@Override
	public synchronized void onResume() {
		super.onResume();

		if (DEBUG) {
			Log.e(LOG_TAG, "+ ON RESUME +");
		}

        mActivityVisible = true;

        readPrefs();
        updateScreenLayout(getResources().getConfiguration().orientation);
        if (fragmentMain != null)
            fragmentMain.updateValues(UPDATE_MODE_ALL);
		
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
//		    	readPrefs();
		    	updatePrefs();

                if (mAutoConnect && mAutoConnectMac != null && !mAutoConnectMac.equals(""))
                    connectDevice(mAutoConnectMac);
		    }
		}
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateScreenLayout(newConfig.orientation);
    }

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

        writePrefs();
        mActivityVisible = false;
		if (DEBUG)
			Log.e(LOG_TAG, "- ON PAUSE -");

	}

    @Override
    public void onStop() {
        super.onStop();

        if (mSerialService != null)
            mSerialService.stop();
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

        if (fragmentMain != null)
            fragmentMain.readPrefs(mPrefs);

        //Toast.makeText(getApplicationContext(), "prefs read", Toast.LENGTH_SHORT).show();
    }

    private void writePrefs() {
        //Toast.makeText(getApplicationContext(), "writing prefs", Toast.LENGTH_SHORT).show();
        if (fragmentMain != null)
            fragmentMain.writePrefs(mPrefs);
    }

    private void updatePrefs() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        //mControlKeyCode = CONTROL_KEY_SCHEMES[mControlKeyId];
        mSerialService.setAllowInsecureConnections( mAllowInsecureConnections );
        
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
    	if ( out.length > 0 ) {
    		mSerialService.write( out );
    	}
    }

    // The Handler that gets information back from the BluetoothService
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

                                //mTitle.setText( R.string.title_connected_to );
                                //mTitle.append(" " + mConnectedDeviceName);
                                Toast.makeText(act.getApplicationContext(), act.getString(R.string.toast_connected_to) + " "
                                        + act.mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                                act.sendGetDataRequest(); // to update RARE data
                                sendEmptyMessageDelayed(DISPLAY_DATA, 1000);
                                break;

                            case BluetoothSerialService.STATE_CONNECTING:
                                act.mLayout.setBackgroundColor(0xFF000000);
                                // mTitle.setText(R.string.title_connecting);
                                Toast.makeText(act.getApplicationContext(), R.string.title_connecting, Toast.LENGTH_SHORT).show();
                                break;

                            case BluetoothSerialService.STATE_LISTEN:
                            case BluetoothSerialService.STATE_NONE:
                                if (act.mMenuItemConnect != null) {
                                    act.mMenuItemConnect.setIcon(android.R.drawable.ic_menu_search);
                                    act.mMenuItemConnect.setTitle(R.string.connect);
                                }
                                act.mLayout.setBackgroundColor(0xFF000000);

                                //  mTitle.setText(R.string.title_not_connected);

                                break;
                        }
                        break;
                    case MESSAGE_WRITE:
                        //if (mLocalEcho) {
                        //byte[] writeBuf = (byte[]) msg.obj;
                        //mEmulatorView.write(writeBuf, msg.arg1);
                        //}

                        break;

                    case MESSAGE_READ:
                        try {
                            // data already in mParser.DATA
                            //if (mParser.Parse()) {
                            act.requestsPending = 0;
                            if (act.mActivityVisible) {
                                if (act.tvPulse.getCurrentTextColor() == 0xFFFFFFFF)
                                    act.tvPulse.setTextColor(0xFFA0A0A0);
                                else
                                    act.tvPulse.setTextColor(0xFFFFFFFF);
                                act.updateValues((act.mParser.LPGStatus != act.mParser.LPGStatus_old) ? UPDATE_MODE_TAGS : UPDATE_MODE_VALUES);
                                act.mParser.LPGStatus_old = act.mParser.LPGStatus;
                            }
                            //}
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "MESSAGE_READ() failed", e);
                        }

                        break;

                    case MESSAGE_VALUE:
                        act.tvPulse.setText(String.valueOf(msg.arg1) + "," + String.valueOf(msg.arg2));
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
        send(ResponseParser.REQUEST_GET_DATA);
    }

    private void sendParameterRequest(Message msg) {
       // Toast.makeText(getApplicationContext(), msg.getData().getString("request"), Toast.LENGTH_SHORT).show();
        Double tmp;
        int flow;
        byte[] bytes;

        switch (Integer.valueOf(msg.getData().getString(ParametersActivity.REQUEST))) {
            case (ParametersActivity.REQUEST_ADD_LPG):
                tmp = Double.valueOf(msg.getData().getString(ParametersActivity.PARAM)) * 10000000.0;

                bytes = toBytes(tmp.intValue());
                ResponseParser.REQUEST_ADD_LPG[2] = bytes[3];
                ResponseParser.REQUEST_ADD_LPG[3] = bytes[2];
                ResponseParser.REQUEST_ADD_LPG[4] = bytes[1];
                ResponseParser.REQUEST_ADD_LPG[5] = bytes[0];

                mParser.UpdateChecksum(ResponseParser.REQUEST_ADD_LPG);
                send(ResponseParser.REQUEST_ADD_LPG);
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
            case (ParametersActivity.REQUEST_RESET_TRIP):
                send(ResponseParser.REQUEST_RESET_TRIP);
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
        mMenuItemStartStopRecording = menu.getItem(3);        
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
        case R.id.menu_start_stop_save:
        	if (mMenuItemStartStopRecording.getTitle() == getString(R.string.menu_stop_logging) ) {
        		doStopRecording();
        	}	
        	else {
        		doStartRecording();
        	}
            return true;
            
        case R.id.menu_about:
        	showAboutDialog();
            return true;
        }
        return false;
    }

    private void doParameters() {
        Intent serverIntent = new Intent(this, ParametersActivity.class);
        serverIntent.putExtra("LPG", String.valueOf(mParser.LPGinjFlow));
        serverIntent.putExtra("PET", String.valueOf(mParser.PETinjFlow));
        startActivityForResult(serverIntent, REQUEST_PARAMETERS);
    }

    private void doPreferences() {
        startActivity(new Intent(this, TermPreferences.class));
    }
    
/*    public void doOpenOptionsMenu() {
        openOptionsMenu();
    }
*/
    private void doStartRecording() {
    	File sdCard = Environment.getExternalStorageDirectory();
    	
    	SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH);
    	String currentDateTimeString = format.format(new Date());
    	String fileName = sdCard.getAbsolutePath() + "/elpigy_" + currentDateTimeString + ".log";

    	mMenuItemStartStopRecording.setTitle(R.string.menu_stop_logging);
        Toast.makeText(getApplicationContext(), getString(R.string.menu_logging_started) + "\n\n" + fileName, Toast.LENGTH_LONG).show();
    }
    
    private void doStopRecording() {
    	mMenuItemStartStopRecording.setTitle(R.string.menu_start_logging);    	
        Toast.makeText(getApplicationContext(), getString(R.string.menu_logging_stopped), Toast.LENGTH_SHORT).show();
    }
    

	private void showAboutDialog() {
		mAboutDialog = new Dialog(Elpigy.this);
		mAboutDialog.setContentView(R.layout.about);
		mAboutDialog.setTitle( getString( R.string.app_name ) + " " + getString( R.string.app_version ));
		
		Button buttonOpen = (Button) mAboutDialog.findViewById(R.id.buttonDialog);
		buttonOpen.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		 
				mAboutDialog.dismiss();
			}
		});		
		
		mAboutDialog.show();
    }
}

