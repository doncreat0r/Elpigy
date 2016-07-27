package com.bobrik.elpigy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * This activity allows to add fuel and change some params
 */
public class ParametersActivity extends Activity implements View.OnClickListener {

    public static final int REQUEST_ADD_LPG = 1;
    public static final int REQUEST_ADD_PET = 2;
    public static final int REQUEST_SET_LPG = 3;
    public static final int REQUEST_SET_PET = 4;
    public static final int REQUEST_RESET_TRIP = 5;
    public static final int REQUEST_SET_SPEED_CORR = 6;
    public static final int REQUEST_TOGGLE_LOG_DATA = 7;

    public static final String REQUEST = "request";
    public static final String PARAM = "param";

    private EditText edLPGFlow;
    private EditText edPETFlow;
    private EditText edAddLPG;
    private EditText edAddPET;
    private EditText edSpeedCorr;

    public void onClick(View v) {
        if (v.getTag() == null) return;

        Intent intent = new Intent();
        switch ((Integer) v.getTag()) {
            case REQUEST_ADD_LPG:
                intent.putExtra(PARAM, edAddLPG.getText().toString());
                break;
            case REQUEST_ADD_PET:
                intent.putExtra(PARAM, edAddPET.getText().toString());
                break;
            case REQUEST_SET_LPG:
                intent.putExtra(PARAM, edLPGFlow.getText().toString());
                break;
            case REQUEST_SET_PET:
                intent.putExtra(PARAM, edPETFlow.getText().toString());
                break;
            case REQUEST_SET_SPEED_CORR:
                intent.putExtra(PARAM, edSpeedCorr.getText().toString());
                break;
        }
        intent.putExtra(REQUEST, (Integer) v.getTag());
        // Set result and finish this Activity
        setResult(Activity.RESULT_OK, intent);

        finish();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.parameters);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        edLPGFlow = (EditText) findViewById(R.id.edLPGFlow);
        edPETFlow = (EditText) findViewById(R.id.edPETFlow);
        edAddLPG = (EditText) findViewById(R.id.edLPG);
        edAddPET = (EditText) findViewById(R.id.edPET);
        edSpeedCorr = (EditText) findViewById(R.id.edSpeedCorr);

        Button backButton = (Button) findViewById(R.id.btnBack);
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.btnAddLPG).setOnClickListener(this);
        findViewById(R.id.btnAddLPG).setTag(REQUEST_ADD_LPG);
        findViewById(R.id.btnAddPET).setOnClickListener(this);
        findViewById(R.id.btnAddPET).setTag(REQUEST_ADD_PET);
        findViewById(R.id.btnSetLPG).setOnClickListener(this);
        findViewById(R.id.btnSetLPG).setTag(REQUEST_SET_LPG);
        findViewById(R.id.btnSetPET).setOnClickListener(this);
        findViewById(R.id.btnSetPET).setTag(REQUEST_SET_PET);
        findViewById(R.id.btnResetTrip).setOnClickListener(this);
        findViewById(R.id.btnResetTrip).setTag(REQUEST_RESET_TRIP);
        findViewById(R.id.btnToggleLogData).setOnClickListener(this);
        findViewById(R.id.btnToggleLogData).setTag(REQUEST_TOGGLE_LOG_DATA);
        findViewById(R.id.btnSetSpeedCorr).setOnClickListener(this);
        findViewById(R.id.btnSetSpeedCorr).setTag(REQUEST_SET_SPEED_CORR);

    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            //Toast.makeText(getApplicationContext(), "toast", Toast.LENGTH_SHORT).show();
            edLPGFlow.setText(intent.getExtras().getString("LPG"));
            edPETFlow.setText(intent.getExtras().getString("PET"));
            edSpeedCorr.setText(intent.getExtras().getString("SPEEDCORR"));
        }
    }
}