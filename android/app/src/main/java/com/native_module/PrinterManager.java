package com.native_module;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.native_module.bluetooth_printer.BluetoothService;
import com.native_module.bluetooth_printer.DeviceListActivity;
import com.native_module.bluetooth_printer.PrintPicture;
import com.native_module.bluetooth_printer.PrinterActivity;


public class PrinterManager extends ReactContextBaseJavaModule {

  private static final int REQUEST_ENABLE_BT = 2;
  public static BluetoothService mService = null;
  private static BluetoothDevice con_dev = null;
  private static final int REQUEST_CONNECT_DEVICE = 1;
  private static final String ENCODING = "GBK";
  private static final int D58MMWIDTH = 384;
  private static final int D80MMWIDTH = 576;

  public PrinterManager(ReactApplicationContext reactContext) {
    super(reactContext);
    // Add the listener for `onActivityResult`
    reactContext.addActivityEventListener(mActivityEventListener);
  }


  @Override
  public String getName() {
    return "PrinterManager";
  }

  private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
      ReactApplicationContext context = getReactApplicationContext();
      try {

        switch (requestCode) {
          case REQUEST_ENABLE_BT:
            if (resultCode == Activity.RESULT_OK) {
              Toast.makeText(context, Constants.bt_on, Toast.LENGTH_LONG).show();
              startDeviceListActivity(context, getCurrentActivity());
            }
            break;
          case REQUEST_CONNECT_DEVICE:
            if (resultCode == Activity.RESULT_OK) {
              String address = intent.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
              con_dev = mService.getDevByMac(address);
              mService.connect(con_dev);
            }
            break;
        }
      } catch (Exception ex) {
        Toast.makeText(context, ex.getMessage(), Toast.LENGTH_SHORT).show();
      }
    }
  };

  @ReactMethod
  public void connect() {
    Activity currentActivity = getCurrentActivity();
    ReactApplicationContext context = getReactApplicationContext();

    try {
      //has adapter
      if (mService.isAvailable()) {
        if (currentActivity != null) {
          //state=off
          if (!mService.isBTopen()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            currentActivity.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
          }
          //state=on
          else {
            startDeviceListActivity(context, currentActivity);
          }
        }
      } else {
        Toast.makeText(context, Constants.no_BT_adapter, Toast.LENGTH_LONG).show();
      }
    } catch (Exception ex) {
      Toast.makeText(context, ex.getMessage(), Toast.LENGTH_SHORT).show();
    }
  }

  private void startDeviceListActivity(ReactApplicationContext context, Activity currentActivity) {
    Intent serverIntent = new Intent(context, DeviceListActivity.class);
    currentActivity.startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    currentActivity.overridePendingTransition(R.anim.fadein, R.anim.fadeout);
  }

  /*GO TO PRINTER ACTIVITY*/
  @ReactMethod
  void printerActivity() {
    ReactApplicationContext context = getReactApplicationContext();
    Intent intent = new Intent(context, PrinterActivity.class);
    context.startActivity(intent);
  }

  /*DISCONNECT*/
  @ReactMethod
  void disconnect() {
    if (mService != null)
      mService.stop();
  }

  /*IMAGE*/
  @ReactMethod
  void printImageFromDrawable(ReactApplicationContext context, int image) {

    if (mService.getState() == BluetoothService.STATE_CONNECTED) {
      byte[] sendData = null;

      Bitmap bm = BitmapFactory.decodeResource(context.getResources(), image);
      if (bm != null) {
        int height = D58MMWIDTH * bm.getHeight() / bm.getWidth();
        bm = Bitmap.createScaledBitmap(bm, D58MMWIDTH, height, false);
        sendData = PrintPicture.POS_PrintBMP(bm, D58MMWIDTH, 0);
        mService.write(sendData);
      } else {
        Toast.makeText(context, Constants.no_file, Toast.LENGTH_SHORT).show();
      }
    } else {
      Toast.makeText(context, Constants.notConnected, Toast.LENGTH_SHORT).show();
    }
  }

  @ReactMethod
  private void printImageFromFilePath(ReactApplicationContext context, String path) {

    byte[] sendData = null;

    Bitmap bm = BitmapFactory.decodeFile(path);
    if (bm != null) {
      int height = D80MMWIDTH * bm.getHeight() / bm.getWidth();
      bm = Bitmap.createScaledBitmap(bm, D80MMWIDTH, height, false);
      sendData = PrintPicture.POS_PrintBMP(bm, D80MMWIDTH, 0);
      mService.write(sendData);
    } else {
      Toast.makeText(context, Constants.no_file, Toast.LENGTH_SHORT).show();
    }
  }

  @ReactMethod
  void printImage(String imagePath, Callback callback) {
    ReactApplicationContext context = getReactApplicationContext();

    if (mService.getState() == BluetoothService.STATE_CONNECTED) {
      printImageFromFilePath(context, imagePath);
      callback.invoke("connected");
    } else {
      Toast.makeText(context, Constants.notConnected, Toast.LENGTH_SHORT).show();
      connect();
      callback.invoke("disconnected");
    }
  }

  /*TEXT*/
  @ReactMethod
  void printText(String message) {
    mService.sendMessage(message + "\n", ENCODING);
  }

}