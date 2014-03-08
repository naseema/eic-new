package com.naseemapps.electricimpcontroller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    MenuItem mRefreshAction;


    public final static String SENDER_ID = "911436184574";

    public static final int RES_OK = 200;
    public static final int RES_ERR = 999;
    public static final int RES_ERR_DEV_OFFLINE = 901;
    public static final int RES_ERR_DEV_BAD_USER_ID = 908;
    public static final int RES_ERR_DEV_BAD_AGENT_ID = 909;

    public static final String RESPONSE_ELECTRIC_IMP_AGENT_NOT_FOUND = "Not Found";
    public static final String RESPONSE_USER_ID_NOT_FOUND = "...";
    public static final String RESPONSE_DEVICE_OFFLINE_BODY = ":(";
    public static final String RESPONSE_DEVICE_ONLINE_BODY = ":)";
    public static final String APP_TAG = "ELECTRIC_IMP_CONTROLLER";

    private String mElectricImpAgentId = "";
    private String mUserId = "";

    public static final String URL_DEVICE_AGENT = "http://agent.electricimp.com/";

    public static final int STATE_ON = 0;
    public static final int STATE_OFF = 1;

    public static final String STATE_STR_ON = "ON";
    public static final String STATE_STR_OFF = "OFF";

    private int mDevState[] = { -1, -1, -1, -1, -1, -1 };
    private ToggleButton[] mDevStateTB = new ToggleButton[6];
    private boolean mTryToRefresh = false;
    private int mWaitingRequest = 0;
    private Button mRefreshB;

    Context context;

    ProgressDialog connectingDialog;

    ProgressBar mProgressBar;
    SharedPreferences mSharedPrefs;



    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        return super.onCreatePanelMenu(featureId, menu);
    }


    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.



        context = this;

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        // USER_ID = mSharedPrefs.getString(PREF_USER_ID, USER_ID);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);

        mRefreshB = (Button) findViewById(R.id.refresh_b);
        mRefreshB.setOnClickListener(this);

        connectingDialog = new ProgressDialog(context);
        connectingDialog.setTitle(R.string.connecting);

        mDevStateTB[0] = (ToggleButton) findViewById(R.id.dev1_tb);
        mDevStateTB[1] = (ToggleButton) findViewById(R.id.dev2_tb);
        mDevStateTB[2] = (ToggleButton) findViewById(R.id.dev3_tb);
        mDevStateTB[3] = (ToggleButton) findViewById(R.id.dev4_tb);
        mDevStateTB[4] = (ToggleButton) findViewById(R.id.dev5_tb);
        mDevStateTB[5] = (ToggleButton) findViewById(R.id.dev6_tb);

        mDevStateTB[0].setOnClickListener(this);
        mDevStateTB[1].setOnClickListener(this);
        mDevStateTB[2].setOnClickListener(this);
        mDevStateTB[3].setOnClickListener(this);
        mDevStateTB[4].setOnClickListener(this);
        mDevStateTB[5].setOnClickListener(this);


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        mRefreshAction = menu.findItem(R.id.action_refresh);

        return true;
    }

    public void setProgressVisible(boolean visible) {
        if (mRefreshAction != null) {
            mRefreshAction.setVisible(visible);
        }
        setProgressBarIndeterminateVisibility(!visible);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, UserPreferencesActivity.class);
            startActivityForResult(i, 1);
            return true;
        } else if (id == R.id.action_refresh) {
            syncStatus();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }









    ///////////////////////////////////////////////////////////////////////////////////








    @Override
    protected void onResume() {
        super.onResume();

        loadUserIdNElectricImpId();

        syncStatus();

    }

    public int postData(int value, int index) {
        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost;

        try {
            if (index == -1)
                httppost = new HttpPost(URL_DEVICE_AGENT + mElectricImpAgentId
                        + "?status=0" + "&uid=" + mUserId);
            else
                httppost = new HttpPost(URL_DEVICE_AGENT + mElectricImpAgentId
                        + "?dev" + index + "=" + value + "&uid=" + mUserId);
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs
                    .add(new BasicNameValuePair("dev" + index, value + ""));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            httppost.setHeader("Connection", "keep-alive");
            // Execute HTTP Post Request

            long startTime = System.currentTimeMillis();

            HttpResponse response = httpclient.execute(httppost);

            /*response = httpclient.execute(httppost);
            response = httpclient.execute(httppost);
            response = httpclient.execute(httppost);
            response = httpclient.execute(httppost);
            response = httpclient.execute(httppost);
            response = httpclient.execute(httppost);
            response = httpclient.execute(httppost);
            response = httpclient.execute(httppost);
            response = httpclient.execute(httppost);
            response = httpclient.execute(httppost);
            response = httpclient.execute(httppost);*/

            long endTime   = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            System.out.println(totalTime);
            Log.e(APP_TAG, "###### total time.... : "+totalTime );

            HttpEntity en = response.getEntity();
            String resBody = "";
            if (en != null) {
                resBody = EntityUtils.toString(en);
                if (resBody.equals(RESPONSE_ELECTRIC_IMP_AGENT_NOT_FOUND)) {
                    Log.e(APP_TAG, "Invalid agent id");
                    return RES_ERR_DEV_BAD_AGENT_ID;
                }
                if (resBody.equals(RESPONSE_USER_ID_NOT_FOUND)) {
                    Log.e(APP_TAG, "Invalid user id");
                    return RES_ERR_DEV_BAD_USER_ID;
                }
                Log.i(APP_TAG, "index=" + index + " - " + resBody);
                if (index == -2
                        && (resBody == null || resBody
                        .equals(RESPONSE_DEVICE_OFFLINE_BODY))) {
                    // Log.e(APP_TAG,"OFFLINE->" + )
                    return RES_ERR_DEV_OFFLINE;
                }
            }

            String[] resArr = resBody.split(",");
            if (resArr.length != 6) {
                Log.e(APP_TAG, "Bad lenght...!!!");
                throw new Exception("Bad lenght");
            }
            for (int i = 0; i < resArr.length; i++) {
                mDevState[i] = Integer.parseInt(resArr[i]);
            }

            return RES_OK;

        } catch (ClientProtocolException e) {
            Log.e(APP_TAG, "error postData: " + e);
            return RES_ERR;
        } catch (IllegalArgumentException e) {
            Log.e(APP_TAG, "error postData: " + e);
            return RES_ERR_DEV_BAD_AGENT_ID;
        } catch (IOException e) {
            Log.e(APP_TAG, "error postData: " + e);
            return RES_ERR;
        } catch (Exception e) {
            Log.e(APP_TAG, "Exception: " + e);
            return RES_ERR;
        }
    }

    private class SwitchDevice extends AsyncTask<Object, Void, Integer> {
        int mIndex;

        public SwitchDevice(int index) {
            mWaitingRequest++;
            mProgressBar.setMax(mWaitingRequest);
            // mProgressBar.setProgress(progress);
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshB.setEnabled(false);
//            setProgressBarIndeterminateVisibility(true);
            setProgressVisible(false);
            mRefreshB.setText(R.string.connecting);
            if (index != -1) {
                mDevState[index] = -1;
                mDevStateTB[index].setEnabled(false);
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // connectingDialog.show();

        }

        @Override
        protected Integer doInBackground(Object... param) {
            mIndex = (Integer) param[1];
            return postData((Integer) param[0], mIndex);
        }

        @Override
        protected void onPostExecute(Integer result) {
            Log.i(APP_TAG, "switchDevice::Result->index=" + mIndex
                    + ", result=" + result);

            mWaitingRequest--;
            mProgressBar.setProgress(mProgressBar.getMax() - mWaitingRequest);

            if (mWaitingRequest == 0) {

                if (mIndex != -1 || mTryToRefresh == true) {
                    mTryToRefresh = false;
                    new SwitchDevice(-1).execute(STATE_ON, -1, null);
                    return;
                }
                // connectingDialog.hide();
//                setProgressBarIndeterminateVisibility(false);
                mProgressBar.setProgress(0);
                mProgressBar.setVisibility(View.INVISIBLE);
                setProgressVisible(true);

                refreshButtonStatus();
                mRefreshB.setEnabled(true);
                mRefreshB.setText(R.string.connect);

                if (result == RES_ERR_DEV_BAD_AGENT_ID) {
                    invalidAgentId();
                    setEnableAvaliableButtons(false);
                    return;
                }

                if (result == RES_ERR_DEV_BAD_USER_ID) {
                    invalidUserId();
                    setEnableAvaliableButtons(false);
                    return;
                }

                if (result == RES_ERR) {
                    showDialog(R.string.err_connection_error, RES_ERR);
//					Toast.makeText(context, R.string.err_connection_error,
//							Toast.LENGTH_LONG).show();
                    setEnableAvaliableButtons(false);
                    return;
                }

                if (mDevState[0] == -1) {
                    showDialog(R.string.err_imp_disconnected, RES_ERR);
//					Toast.makeText(context, R.string.err_imp_disconnected,
//							Toast.LENGTH_LONG).show();
                    setEnableAvaliableButtons(false);
                } else {
                    setEnableAvaliableButtons(true);
                }
            }

        }
    }

    private boolean isValid() {
        if (!isNetworkConnected()) {
            Toast.makeText(context, R.string.err_no_internet, Toast.LENGTH_LONG)
                    .show();
            setEnableAvaliableButtons(false);
            return false;
        }

        if (!isElectricImpAgentIdValid()) {
            invalidAgentId();
            return false;
        }

        if (!isUserIdValid()) {
            Toast.makeText(context, R.string.err_user_id, Toast.LENGTH_LONG)
                    .show();
            return false;
        }

        return true;

    }

    @Override
    public void onClick(View v) {
        EasyTracker.getInstance(this).send(MapBuilder
                        .createEvent("ui_action",     // Event category (required)
                                "button_press" + Settings.Secure.getString(MainActivity.this.getContentResolver(), Settings.Secure.ANDROID_ID),  // Event action (required)
                                "click_button",   // Event label
                                (long) 1000)            // Event value
                        .build()
        );
        if (!isValid()) {
            return;
        }

        switch (v.getId()) {
            case R.id.dev1_tb:
                new SwitchDevice(0).execute(
                        mDevStateTB[0].getText().equals(STATE_STR_ON) ? STATE_ON
                                : STATE_OFF, 1);
                break;
            case R.id.dev2_tb:
                new SwitchDevice(1).execute(
                        mDevStateTB[1].getText().equals(STATE_STR_ON) ? STATE_ON
                                : STATE_OFF, 2);
                break;
            case R.id.dev3_tb:
                new SwitchDevice(2).execute(
                        mDevStateTB[2].getText().equals(STATE_STR_ON) ? STATE_ON
                                : STATE_OFF, 3);
                break;
            case R.id.dev4_tb:
                new SwitchDevice(3).execute(
                        mDevStateTB[3].getText().equals(STATE_STR_ON) ? STATE_ON
                                : STATE_OFF, 4);
                break;
            case R.id.dev5_tb:
                new SwitchDevice(4).execute(
                        mDevStateTB[4].getText().equals(STATE_STR_ON) ? STATE_ON
                                : STATE_OFF, 5);
                break;
            case R.id.dev6_tb:
                new SwitchDevice(5).execute(
                        mDevStateTB[5].getText().equals(STATE_STR_ON) ? STATE_ON
                                : STATE_OFF, 6);
                break;

            default:
                syncStatus();
                break;
        }

    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            // There are no active networks.
            return false;
        } else
            return true;
    }

    private void refreshButtonStatus() {
        for (int i = 0; i < mDevState.length - 0; i++) {
            if (mDevState[i] == -1) {
                mDevStateTB[i].setEnabled(false);
            } else {
                mDevStateTB[i].setChecked(mDevState[i] == STATE_ON);

            }
        }
//		mDevStateTB[4].setChecked(false);
//		mDevStateTB[5].setChecked(false);

    }

    private void setEnableAvaliableButtons(boolean stat) {
        for (int i = 0; i < mDevState.length; i++) {
            mDevStateTB[i].setEnabled(stat);
        }
        if (stat) {
            mRefreshB.setVisibility(View.GONE);
        } else {
            mRefreshB.setVisibility(View.VISIBLE);
            mRefreshB.setText(R.string.connect);
        }
    }

    private void syncStatus() {

        if (!isValid()) {
            return;
        }
        setProgressVisible(false);
//        setProgressBarIndeterminateVisibility(true);
        mTryToRefresh = true;
        new SwitchDevice(-1).execute(STATE_ON, -1);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);

        loadUserIdNElectricImpId();
    }

    private void loadUserIdNElectricImpId() {
        if (!mSharedPrefs.getString(
                UserPreferencesActivity.PREF_ELECTRIC_IMP_AGENT_ID, "").equals(
                this.getString(R.string.val_not_set)))
            mElectricImpAgentId = mSharedPrefs.getString(
                    UserPreferencesActivity.PREF_ELECTRIC_IMP_AGENT_ID, "");

        mUserId = mSharedPrefs.getString(UserPreferencesActivity.PREF_USER_ID,
                "");
    }

    private boolean isElectricImpAgentIdValid() {
        return !mElectricImpAgentId.equals("");
    }

    private boolean isUserIdValid() {
        return !mUserId.equals("");
    }

    private void invalidAgentId() {
        showDialog(R.string.err_electric_imp_agent_id, RES_ERR_DEV_BAD_AGENT_ID);

		/*Toast.makeText(context, R.string.err_electric_imp_agent_id,
				Toast.LENGTH_LONG).show();
		Intent i = new Intent(this, UserPreferencesActivity.class);
		i.putExtra(UserPreferencesActivity.EXTRA_PREFRENCE,
				RES_ERR_DEV_BAD_AGENT_ID);
		startActivityForResult(i, 1);*/
    }

    private void invalidUserId() {
        showDialog(R.string.err_user_id, RES_ERR_DEV_BAD_USER_ID);
//		Toast.makeText(context, , Toast.LENGTH_LONG).show();
//		Intent i = new Intent(this, UserPreferencesActivity.class);
//		i.putExtra(UserPreferencesActivity.EXTRA_PREFRENCE,
//				RES_ERR_DEV_BAD_USER_ID);
//		startActivityForResult(i, 1);
    }

    private void showDialog(int errMsg, final int SettingsErr) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(getString(errMsg))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (SettingsErr != RES_ERR) {
                            Intent i = new Intent(getApplicationContext(), UserPreferencesActivity.class);
                            i.putExtra(UserPreferencesActivity.EXTRA_PREFRENCE,
                                    SettingsErr);
                            startActivityForResult(i, 1);
                        }
                    }
                })
                .setIcon(android.R.drawable.presence_offline)
                .show();
    }

    private void gg() {

    }



    private void makeRegisterToGCM() {
    /*    // Check if Google account exist for gcm push service, if not exit.
        if (AccountManager.get(getApplicationContext()).getAccountsByType(
                "com.google").length == 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Google Account needed")
                    .setMessage(
                            "This app cant work without a google account registered.")
                    .setNeutralButton("OK",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    finish();
                                }
                            }).setCancelable(false).show();

            return;
        }*/


/*        // The checkDevice() method verifies that the device supports GCM and
        // throws an exception if it does not.
        GCMRegistrar.checkDevice(this);
        // The checkManifest() method verifies that the application manifest
        // contains meets all the requirements described in Writing the Android
        // Application
        GCMRegistrar.checkManifest(this);
        String regId = GCMRegistrar.getRegistrationId(this);
        if (regId.equals("")) {
            // to register the device, passing the SENDER_ID you got when you
            // signed up for GCM.
            GCMRegistrar.register(this, AppConstants.SENDER_ID);
        } else {
            Log.v("Reg - MyApp", "Already registered");
        }
    */}

    @Override
    public void onStart() {
        super.onStart();
//	    ... // The rest of your onStart() code.
        EasyTracker.getInstance(this).activityStart(this);  // Add this method.
    }

    @Override
    public void onStop() {
        super.onStop();
//	    ... // The rest of your onStop() code.
        EasyTracker.getInstance(this).activityStop(this);  // Add this method.
    }
}
