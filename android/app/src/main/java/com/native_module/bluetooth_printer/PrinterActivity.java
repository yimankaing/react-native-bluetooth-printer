package com.native_module.bluetooth_printer;

import android.annotation.SuppressLint;
import android.content.Intent;

import com.native_module.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import android.util.Log;

public class PrinterActivity extends Activity {
  Button btnSearch;
  Button btnPrintTest;
  Button btnSend;
  Button btnDis;
  EditText edtContext;
  EditText edtPrint;
  private static final int REQUEST_ENABLE_BT = 2;
  BluetoothService mService = null;
  BluetoothDevice con_dev = null;
  private static final int REQUEST_CONNECT_DEVICE = 1;
  private static final String ENCODING = "GBK";
  private static final int D58MMWIDTH = 384;
  private static final int D80MMWIDTH = 576;

  /*bluetooth adapter availability*/
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    mService = new BluetoothService(this, mHandler);

    if (!mService.isAvailable()) {
      Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
      finish();
    }
  }

  /*Bluetooth communication*/
  @Override
  public void onStart() {
    super.onStart();
    /*turn on bluetooth*/
    if (!mService.isBTopen()) {
      Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }
    try {
      btnPrintTest = (Button) this.findViewById(R.id.btnPrint);
      btnPrintTest.setOnClickListener(new ClickEvent());
      btnSearch = (Button) this.findViewById(R.id.btnSearch);
      btnSearch.setOnClickListener(new ClickEvent());
      btnSend = (Button) this.findViewById(R.id.btnSend);
      btnSend.setOnClickListener(new ClickEvent());
      btnDis = (Button) this.findViewById(R.id.btnDis);
      btnDis.setOnClickListener(new ClickEvent());
      edtContext = (EditText) findViewById(R.id.txt_content);
      btnDis.setEnabled(true);
      btnSend.setEnabled(false);
      btnPrintTest.setEnabled(false);
    } catch (Exception ex) {
      Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  /*Disconnect*/
  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mService != null)
      mService.stop();
    mService = null;
  }

  /*Event on buttons*/
  class ClickEvent implements View.OnClickListener {
    public void onClick(View v) {
      if (v == btnSearch) {
        Intent serverIntent = new Intent(PrinterActivity.this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
      } else if (v == btnSend) {
        String msg = edtContext.getText().toString();
        if (msg.length() > 0) {
          mService.sendMessage(msg + "\n", ENCODING);
        }
      } else if (v == btnDis) {
        if (mService != null)
          mService.stop();
      } else if (v == btnPrintTest) {
        String msg = "";
        printImage();

        byte[] cmd = new byte[3];
        cmd[0] = 0x1b;
        cmd[1] = 0x21;
        cmd[2] |= 0x10;
        mService.write(cmd);
        mService.sendMessage("Congratulations!\n", ENCODING);
        cmd[2] &= 0xEF;
        mService.write(cmd);
        msg = "  You have successfully created communications between your device and our bluetooth printer.\n\n";

        mService.sendMessage(msg, "GBK");
      }
    }
  }

  @SuppressLint("HandlerLeak")
  private final Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case BluetoothService.MESSAGE_STATE_CHANGE:
          switch (msg.arg1) {
            case BluetoothService.STATE_CONNECTED:
              Toast.makeText(getApplicationContext(), "Device is connected", Toast.LENGTH_SHORT).show();
              btnDis.setEnabled(true);
              btnSend.setEnabled(true);
              btnPrintTest.setEnabled(true);
              break;
            case BluetoothService.STATE_CONNECTING:
              Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
              break;
            case BluetoothService.STATE_LISTEN:
            case BluetoothService.STATE_NONE:
              break;
          }
          break;
        case BluetoothService.MESSAGE_CONNECTION_LOST:
          Toast.makeText(getApplicationContext(), "Device is disconnected",
                  Toast.LENGTH_SHORT).show();
          btnSend.setEnabled(false);
          btnPrintTest.setEnabled(false);
          break;
        case BluetoothService.MESSAGE_UNABLE_CONNECT:
          Toast.makeText(getApplicationContext(), "Unable to connect device", Toast.LENGTH_SHORT).show();
          break;
      }
    }
  };

  /*Bluetooth communication is reached*/
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_ENABLE_BT:
        if (resultCode == Activity.RESULT_OK) {
          Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_LONG).show();
        } else {
          finish();
        }
        break;
      case REQUEST_CONNECT_DEVICE:
        if (resultCode == Activity.RESULT_OK) {
          String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
          con_dev = mService.getDevByMac(address);

          mService.connect(con_dev);
        }
        break;
    }
  }

  /*Image printing*/
  public void printImage() {
    byte[] sendData = null;

    Bitmap bm = BitmapFactory.decodeResource(this.getResources(), R.drawable.android_logo);
    int height = D58MMWIDTH * bm.getHeight() / bm.getWidth();
    bm = Bitmap.createScaledBitmap(bm, D58MMWIDTH, height, false);
    sendData = PrintPicture.POS_PrintBMP(bm, D58MMWIDTH, 0);
    mService.write(sendData);
  }
}
