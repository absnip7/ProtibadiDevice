package com.example.protibadidevice;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class Main2Activity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    String EmergencyTelephoneNumber0, EmergencyTelephoneNumber1, EmergencyTelephoneNumber2, EmergencyTelephoneNumber3;
    String UserName;
    BluetoothAdapter bluetoothAdapter;
    BluetoothConnectThread bluetoothConnectThread;
    CheckBox checkdevice;
    CheckBox checkgps;
    CommunicationThread communicationThread;
    Switch deviceCheck;
    Switch gpsCheck;
    ImageView imageview;
    LocationListener locationListener;
    LocationManager locationManager;
    int messageCounter = 0;
    private UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    TextView nameview;
    Button stopbutton;
    BluetoothDevice targetDevice;
    Button toFacebook;

    // Thread for Bluetooth Connection
    private class BluetoothConnectThread extends Thread {
        private final BluetoothDevice bluetoothDevice;
        private BluetoothSocket bluetoothSocket = null;

        public BluetoothConnectThread(BluetoothDevice bluetoothDevice) {
            Log.d("TAG", "BluetoothConnectThread: ");
            this.bluetoothDevice = bluetoothDevice;
            try {
                this.bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(Main2Activity.this.myUUID);
            } catch (IOException e) {

                Main2Activity.this.deviceCheck.setChecked(false);
                Main2Activity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(Main2Activity.this.getApplicationContext(), "Device Not Found. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        public void run() {
            try {
                this.bluetoothSocket.connect();
                Main2Activity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Main2Activity.this.deviceCheck.setChecked(true);
                        Toast.makeText(Main2Activity.this.getApplicationContext(), "Connected To Device", Toast.LENGTH_SHORT).show();
                    }
                });
                Main2Activity.this.communicationThread = new CommunicationThread(this.bluetoothSocket);
                Main2Activity.this.communicationThread.start();
            } catch (IOException e) {
                Main2Activity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Main2Activity.this.deviceCheck.setChecked(false);
                        Toast.makeText(Main2Activity.this.getApplicationContext(), "Device Not Found. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        public void cancel() {
            try {
                this.bluetoothSocket.close();
            } catch (IOException e) {
                Main2Activity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(Main2Activity.this.getApplicationContext(), "Something went wrong turning off the Connection", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    // Bluetooth communication and calling message sending function
    private class CommunicationThread extends Thread {
        BluetoothSocket connectedBluetoothSocket;
        InputStream connectedInputStream;

        CommunicationThread(BluetoothSocket bluetoothSocket) {
            this.connectedBluetoothSocket = bluetoothSocket;
        }

        public void run() {
            Log.d("run", "run: ");
            byte[] buffer = new byte[1024];
            try {
                this.connectedInputStream = this.connectedBluetoothSocket.getInputStream();
                while (true) {
                    Log.d("loop", "any");
                    String strReceived = new String(buffer, 0, this.connectedInputStream.read(buffer));
//                    Log.d("TAG", "run: penultimate");
                    Log.d("buffer", "run: " + strReceived);
                    if(!strReceived.isEmpty() && strReceived.length() == 1){
                        sendText();

                    }
                    if (Main2Activity.this.messageCounter == 0) {
                        Main2Activity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                if (!(ContextCompat.checkSelfPermission(Main2Activity.this, "android.permission.ACCESS_FINE_LOCATION") == 0 || ContextCompat.checkSelfPermission(Main2Activity.this, "android.permission.ACCESS_COARSE_LOCATION") == 0 || VERSION.SDK_INT < 23)) {
                                    ActivityCompat.requestPermissions(Main2Activity.this,new String[]{"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION", "android.permission.INTERNET"}, 10);
                                }
//                                Main2Activity.this.stopbutton.setVisibility(View.VISIBLE);
//                                Main2Activity.this.locationManager.requestLocationUpdates("gps", 10000, 0.0f, Main2Activity.this.locationListener);
                            }
                        });
                    }
                }
            } catch (IOException e) {
                Main2Activity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(Main2Activity.this, "Device Removed.", Toast.LENGTH_SHORT).show();
                        Main2Activity.this.deviceCheck.setChecked(false);
                    }
                });
            }
        }

        public void cancel() {
            try {
                this.connectedInputStream.close();
                Main2Activity.this.deviceCheck.setChecked(false);
                Main2Activity.this.stopbutton.setVisibility(View.INVISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Message Sender Function
    private void sendText() {
        String postingMessage = "This is an emergency message, your assistance is required RIGHT NOW! ";
        if (ContextCompat.checkSelfPermission(this, "android.permission.SEND_SMS") != 0) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.SEND_SMS"}, 1);
        } else {
            if(!this.EmergencyTelephoneNumber0.isEmpty()) SmsManager.getDefault().sendTextMessage(this.EmergencyTelephoneNumber0, null, "Help !!! ".concat(postingMessage), null, null);
            if(!this.EmergencyTelephoneNumber1.isEmpty()) SmsManager.getDefault().sendTextMessage(this.EmergencyTelephoneNumber1, null, "Help !!! ".concat(postingMessage), null, null);
            if(!this.EmergencyTelephoneNumber2.isEmpty()) SmsManager.getDefault().sendTextMessage(this.EmergencyTelephoneNumber2, null, "Help !!! ".concat(postingMessage), null, null);
            if(!this.EmergencyTelephoneNumber3.isEmpty()) SmsManager.getDefault().sendTextMessage(this.EmergencyTelephoneNumber3, null, "Help !!! ".concat(postingMessage), null, null);
        }
//        Toast.makeText(getApplicationContext(), "Notification Sent", Toast.LENGTH_SHORT).show();
    }

    /* Access modifiers changed, original: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_main2);
        this.nameview = (TextView) findViewById(R.id.textView2);
        this.imageview = (ImageView) findViewById(R.id.imageView);
        UpdateTelePhoneNumber();
        this.deviceCheck = (Switch) findViewById(R.id.checkdevice);
        this.gpsCheck = (Switch) findViewById(R.id.checkGPS);
        this.stopbutton = (Button) findViewById(R.id.stop);
        this.locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(Color.parseColor("#ffd740"));
        toolbar.setTitleTextColor(ViewCompat.MEASURED_STATE_MASK);
        this.deviceCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Main2Activity.this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (Main2Activity.this.bluetoothAdapter.isEnabled()) {
                        Main2Activity.this.InitiateBluetoothConnection();
                        return;
                    }
                    Main2Activity.this.startActivityForResult(new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE"), 1);
                    return;
                }
                if (Main2Activity.this.bluetoothConnectThread != null) {
                    Main2Activity.this.bluetoothConnectThread.cancel();
                }
                Main2Activity.this.deviceCheck.setChecked(false);
            }
        });
        this.stopbutton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Main2Activity.this.messageCounter = 0;
                Main2Activity.this.locationManager.removeUpdates(Main2Activity.this.locationListener);
                Main2Activity.this.bluetoothConnectThread.cancel();
                Main2Activity.this.communicationThread.cancel();
            }
        });

        // Commenting GraphRequest and subsequent code
//        GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), new GraphJSONObjectCallback() {
//            public void onCompleted(JSONObject object, GraphResponse response) {
//                try {
//                    Main2Activity.this.UserName = object.get("name").toString();
//                    Main2Activity.this.nameview.setText(Main2Activity.this.UserName);
//                    Picasso.with(Main2Activity.this).load(object.getJSONObject("picture").getJSONObject(ShareConstants.WEB_DIALOG_PARAM_DATA).get("url").toString()).into(Main2Activity.this.imageview);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        Bundle parameters = new Bundle();
//        parameters.putString(GraphRequest.FIELDS_PARAM, "id,name,picture.type(large)");
//        request.setParameters(parameters);
//        request.executeAsync();

        // Commenting ends here



        if (this.locationManager.isProviderEnabled("gps")) {
            this.gpsCheck.setChecked(true);
        }
        this.gpsCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !Main2Activity.this.locationManager.isProviderEnabled("gps")) {
                    Main2Activity.this.startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
                }
            }
        });
        this.stopbutton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Main2Activity.this.locationManager.removeUpdates(Main2Activity.this.locationListener);
                Main2Activity.this.messageCounter = 0;
                Main2Activity.this.bluetoothConnectThread.cancel();
                Main2Activity.this.communicationThread.cancel();
            }
        });
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        ((NavigationView) findViewById(R.id.nav_view)).setNavigationItemSelectedListener(this);
    }

    private void InitiateBluetoothConnection() {
        for (BluetoothDevice device : this.bluetoothAdapter.getBondedDevices()) {
            if (device.getName().contains("HC-05")) {
                this.targetDevice = device;
                break;
            }
            else {
                Log.d("connection", "InitiateBluetoothConnection: " + device.getName().toString());
            }
        }
        if (this.targetDevice != null) {
            this.bluetoothConnectThread = new BluetoothConnectThread(this.targetDevice);
            this.bluetoothConnectThread.start();
            return;
        }
        Toast.makeText(this, "Device Not Found. Please Try Again.", Toast.LENGTH_SHORT).show();
    }

    private void UpdateTelePhoneNumber() {
        this.EmergencyTelephoneNumber0 = getSharedPreferences("EmergencyPhone0", 0).getString("number", "");
        this.EmergencyTelephoneNumber1 = getSharedPreferences("EmergencyPhone1", 0).getString("number", "");
        this.EmergencyTelephoneNumber2 = getSharedPreferences("EmergencyPhone2", 0).getString("number", "");
        this.EmergencyTelephoneNumber3 = getSharedPreferences("EmergencyPhone3", 0).getString("number", "");
    }

    /* Access modifiers changed, original: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 1) {
            return;
        }
        if (resultCode == -1) {
            InitiateBluetoothConnection();
            return;
        }
        this.deviceCheck.setChecked(false);
        Toast.makeText(this, "This Application Relies on Bluetooth", Toast.LENGTH_SHORT).show();
    }

    /* Access modifiers changed, original: protected */
    public void onDestroy() {
        super.onDestroy();
        this.bluetoothConnectThread.cancel();
        this.communicationThread.interrupt();
    }

    private void send(Double latitude, Double longitude) {
        String message = "http://maps.google.com/?q=" + latitude + "," + longitude;
        String postingMessage = "This is an emergency message, your assistance is required RIGHT NOW! ";
        Toast.makeText(this, "Message Sent", Toast.LENGTH_SHORT).show();
        Bundle params = new Bundle();

//        params.putString(ShareConstants.WEB_DIALOG_PARAM_MESSAGE, postingMessage);
        params.putString("link", message);
          // Commenting GraphRequest codes
//        new GraphRequest(AccessToken.getCurrentAccessToken(), "/me/feed", params, HttpMethod.POST, new Callback() {
//            public void onCompleted(GraphResponse response) {
//                Log.v("VAL", response.toString());
//            }
//        }).executeAsync();
        // Commenting ends here
        if (ContextCompat.checkSelfPermission(this, "android.permission.SEND_SMS") != 0) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.SEND_SMS"}, 1);
        } else {
            SmsManager.getDefault().sendTextMessage(this.EmergencyTelephoneNumber0, null, "Help !!! ".concat(postingMessage), null, null);
            SmsManager.getDefault().sendTextMessage(this.EmergencyTelephoneNumber1, null, "Help !!! ".concat(postingMessage), null, null);
            SmsManager.getDefault().sendTextMessage(this.EmergencyTelephoneNumber2, null, "Help !!! ".concat(postingMessage), null, null);
            SmsManager.getDefault().sendTextMessage(this.EmergencyTelephoneNumber3, null, "Help !!! ".concat(postingMessage), null, null);
        }
        Toast.makeText(getApplicationContext(), "Notification Sent", Toast.LENGTH_SHORT).show();
    }

    public void onBackPressed() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        AlertDialog.Builder alertDialogBuilder;
        if (id == R.id.whatIsProtibadi) {
            alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle((CharSequence) "About Protibadi");
            alertDialogBuilder.setMessage((CharSequence) "Public sexual harassment has emerged as a large and growing concern in urban Bangladesh, with deep and damaging implications for gender security, justice, and rights of public participation. In this paper we describe an integrated program of ethnographic and design work meant to understand and address such problems. For one year we conducted surveys, interviews, and focus groups around sexual harassment with women at three different universities in Dhaka. Based on this input, we developed \"Protibadi\", a web and mobile phone based application designed to report, map, and share women's stories around sexual harassment in public places. In August 2013 the system launched, user studies were conducted, and public responses were monitored to gauge reactions, strengths, and limits of the system. This device looks to provide resistance to event of sexual harrasment by notifying the emergency contacts of the victim.");
            alertDialogBuilder.setPositiveButton((CharSequence) "Ok!", (DialogInterface.OnClickListener) new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alertDialogBuilder.setNegativeButton((CharSequence) "Send Feedback", (DialogInterface.OnClickListener) new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String mailto = "mailto:hello@protibadi.com?cc=&subject=" + Uri.encode("Feedback") + "&body=" + Uri.encode("Write your feedback here: \n");
                    Intent emailIntent = new Intent("android.intent.action.SENDTO");
                    emailIntent.setData(Uri.parse(mailto));
                    try {
                        Main2Activity.this.startActivity(emailIntent);
                    } catch (ActivityNotFoundException e) {
                    }
                }
            });
            alertDialogBuilder.create().show();
        } else if (id == R.id.emergencyPhoneNumber) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog, null);
            dialogBuilder.setView(dialogView);
            final EditText edt1 = (EditText) dialogView.findViewById(R.id.edit1);
            final EditText edt2 = (EditText) dialogView.findViewById(R.id.edit2);
            final EditText edt3 = (EditText) dialogView.findViewById(R.id.edit3);
            final EditText edt4 = (EditText) dialogView.findViewById(R.id.edit4);
            edt1.setText(this.EmergencyTelephoneNumber0);
            edt2.setText(this.EmergencyTelephoneNumber1);
            edt3.setText(this.EmergencyTelephoneNumber2);
            edt4.setText(this.EmergencyTelephoneNumber3);
            dialogBuilder.setTitle((CharSequence) "Emergency Phone");
            dialogBuilder.setMessage((CharSequence) "Enter your emergency phone no:");
            dialogBuilder.setPositiveButton((CharSequence) "Update", (DialogInterface.OnClickListener) new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Editor editor1 = Main2Activity.this.getSharedPreferences("EmergencyPhone0", 0).edit();
                    Editor editor2 = Main2Activity.this.getSharedPreferences("EmergencyPhone1", 0).edit();
                    Editor editor3 = Main2Activity.this.getSharedPreferences("EmergencyPhone2", 0).edit();
                    Editor editor4 = Main2Activity.this.getSharedPreferences("EmergencyPhone3", 0).edit();
                    editor1.putString("number", edt1.getText().toString());
                    editor2.putString("number", edt2.getText().toString());
                    editor3.putString("number", edt3.getText().toString());
                    editor4.putString("number", edt4.getText().toString());
                    editor1.apply();
                    editor2.apply();
                    editor3.apply();
                    editor4.apply();
                    Toast.makeText(Main2Activity.this.getApplicationContext(), "Database Updated!", Toast.LENGTH_SHORT).show();
                    Main2Activity.this.UpdateTelePhoneNumber();
                }
            });
            dialogBuilder.create().show();
        } else if (id == R.id.howToUse) {
            alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle((CharSequence) "How to USE?");
            alertDialogBuilder.setMessage((CharSequence) getString(R.string.nav_howToUse_instruction));
            alertDialogBuilder.setPositiveButton((CharSequence) "Ok! I can use it!", (DialogInterface.OnClickListener) new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alertDialogBuilder.create().show();
        } else if (id == R.id.reportBug) {
            String mailto = "mailto:hello@protibadi.com?cc=&subject=" + Uri.encode("Reports/Bugs") + "&body=" + Uri.encode("Describe the problem you are facing: \n");
            Intent emailIntent = new Intent("android.intent.action.SENDTO");
            emailIntent.setData(Uri.parse(mailto));
            try {
                startActivity(emailIntent);
            } catch (ActivityNotFoundException e) {
            }
        } else if (id == R.id.nav_share) {
            Intent sendIntent = new Intent();
            sendIntent.setAction("android.intent.action.SEND");
            sendIntent.putExtra("android.intent.extra.TEXT", "Download Protibadi. " + Uri.parse("https://www.protibadi.com"));
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        } else if (id == R.id.nav_exit) {

            Toast.makeText(getApplicationContext(), "Logged Out", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, MainActivity.class));
        }
        ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawer(GravityCompat.START);
        return true;
    }
}
