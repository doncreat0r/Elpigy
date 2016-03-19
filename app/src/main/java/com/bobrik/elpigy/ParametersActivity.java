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
public class ParametersActivity extends Activity {

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

        Button addLPG = (Button) findViewById(R.id.btnAddLPG);
        addLPG.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(PARAM, edAddLPG.getText().toString());
                intent.putExtra(REQUEST, String.valueOf(REQUEST_ADD_LPG));

                // Set result and finish this Activity
                setResult(Activity.RESULT_OK, intent);

                finish();
            }
        });
        Button addPET = (Button) findViewById(R.id.btnAddPET);
        addPET.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(PARAM, edAddPET.getText().toString());
                intent.putExtra(REQUEST, String.valueOf(REQUEST_ADD_PET));

                // Set result and finish this Activity
                setResult(Activity.RESULT_OK, intent);

                finish();
            }
        });
        Button setLPG = (Button) findViewById(R.id.btnSetLPG);
        setLPG.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(PARAM, edLPGFlow.getText().toString());
                intent.putExtra(REQUEST, String.valueOf(REQUEST_SET_LPG));

                // Set result and finish this Activity
                setResult(Activity.RESULT_OK, intent);

                finish();
            }
        });
        Button setPET = (Button) findViewById(R.id.btnSetPET);
        setPET.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(PARAM, edPETFlow.getText().toString());
                intent.putExtra(REQUEST, String.valueOf(REQUEST_SET_PET));

                // Set result and finish this Activity
                setResult(Activity.RESULT_OK, intent);

                finish();
            }
        });
        Button resetTrip = (Button) findViewById(R.id.btnResetTrip);
        resetTrip.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(REQUEST, String.valueOf(REQUEST_RESET_TRIP));

                // Set result and finish this Activity
                setResult(Activity.RESULT_OK, intent);

                finish();
            }
        });
        Button toggleLogData = (Button) findViewById(R.id.btnToggleLogData);
        toggleLogData.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(REQUEST, String.valueOf(REQUEST_TOGGLE_LOG_DATA));

                // Set result and finish this Activity
                setResult(Activity.RESULT_OK, intent);

                finish();
            }
        });
        Button setSpeedCorr = (Button) findViewById(R.id.btnSetSpeedCorr);
        setSpeedCorr.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(REQUEST, String.valueOf(REQUEST_SET_SPEED_CORR));

                // Set result and finish this Activity
                setResult(Activity.RESULT_OK, intent);

                finish();
            }
        });

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