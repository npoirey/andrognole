package bobbarteam.andrognole.configuration;

import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

import bobbarteam.andrognole.R;
import bobbarteam.andrognole.engine.location.ParkingLocationDataSource;


public class AppPreferences extends PreferenceActivity implements NFCDialogFragment.NoticeDialogListener {
    private static final String TAG = AppPreferences.class.getSimpleName();
    NfcAdapter adapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    Tag mytag;
    Context ctx;
    private NFCDialogFragment readyDialog;
    private boolean dialogActive = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readyDialog = new NFCDialogFragment();
        adapter = NfcAdapter.getDefaultAdapter(this);
        if(adapter != null) {
            pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
            writeTagFilters = new IntentFilter[]{tagDetected};
            dialogActive = false;
        }

        addPreferencesFromResource(R.xml.preferences);

        ctx=this;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Preference setUpNFC = preferenceScreen.findPreference(getString(R.string.preferences_nfc_setup));
        Preference resetData = preferenceScreen.findPreference(getString(R.string.preferences_reset_database));
        if(preference == setUpNFC && adapter != null){
            readyDialog.show(getFragmentManager(), "tag");
        }
        else if(preference == resetData){
            ParkingLocationDataSource datasource = new ParkingLocationDataSource(getApplicationContext());
            datasource.open();
            datasource.deleteAllParkingLocation();
            datasource.close();
            Log.i(TAG, "deleted all parkings");
            Log.d(TAG, "Broadcasting RESET_PARKINGS");
            Intent intent = new Intent("INTENT_NEW_PARKING");
            intent.putExtra("case", 2);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            Toast.makeText(ctx, getString(R.string.data_reset_toast_success), Toast.LENGTH_LONG).show();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { NdefRecord.createUri(text) };
        NdefMessage message = new NdefMessage(records);
        // Get an instance of Ndef for the tag.
        Ndef ndef = Ndef.get(tag);
        // Enable I/O
        ndef.connect();
        // Write the message
        ndef.writeNdefMessage(message);
        // Close the connection
        ndef.close();
    }

    @Override
    protected void onNewIntent(Intent intent){
        Log.i(TAG, "new intent");
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            mytag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Log.i(TAG, "Tag detected : " + mytag.toString() + "writeMode:"+writeMode);
            if(writeMode){
                Log.i(TAG, "ready to write on tag");
                try {
                    if(mytag==null){
                        Toast.makeText(ctx, getString(R.string.nfc_tag_error), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "mytag is null");
                    }else{
                        write("andrognole://register", mytag);
                        Log.i(TAG, "Write operation succeded on tag");
                        Toast.makeText(ctx, getString(R.string.nfc_tag_wrote), Toast.LENGTH_LONG ).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(ctx, getString(R.string.nfc_tag_error), Toast.LENGTH_LONG ).show();
                    Log.e(TAG, "IO error");
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(ctx, getString(R.string.nfc_tag_error) , Toast.LENGTH_LONG ).show();
                    Log.e(TAG, "format error");
                    e.printStackTrace();
                }
            }

            if(dialogActive) {
                readyDialog.dismiss();
            }
        }
    }



    @Override
    protected void onResume() {
        Log.i(TAG, "resume");
        super.onResume();
        if(adapter != null)
            adapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "pause");
        super.onPause();
        if(adapter != null)
            adapter.disableForegroundDispatch(this);
    }

    private void WriteModeOn(){
        writeMode = true;
        Log.v(TAG, "nfc write mode on");
    }

    private void WriteModeOff(){
        writeMode = false;
        Log.v(TAG, "nfc write mode off");
    }

    @Override
    public void onBeforeCreateDialog(DialogFragment dialog) {
        dialogActive = true;
        WriteModeOn();
    }

    @Override
    public void onAfterDismiss(DialogFragment dialog) {
        dialogActive = true;
        WriteModeOff();
    }
}
