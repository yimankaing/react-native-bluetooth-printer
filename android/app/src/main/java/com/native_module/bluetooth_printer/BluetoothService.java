
package com.native_module.bluetooth_printer;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class BluetoothService {
  private static final String TAG = "BluetoothService";
  private static final boolean D = true;
  public static final int MESSAGE_STATE_CHANGE = 1;
  public static final int MESSAGE_READ = 2;
  public static final int MESSAGE_WRITE = 3;
  public static final int MESSAGE_DEVICE_NAME = 4;
  public static final int MESSAGE_CONNECTION_LOST = 5;
  public static final int MESSAGE_UNABLE_CONNECT = 6;
  private static final String NAME = "BTPrinter";
  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
  private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
  private final Handler mHandler;
  private BluetoothService.AcceptThread mAcceptThread;
  private BluetoothService.ConnectThread mConnectThread;
  private BluetoothService.ConnectedThread mConnectedThread;
  private int mState = 0;
  public static final int STATE_NONE = 0;
  public static final int STATE_LISTEN = 1;
  public static final int STATE_CONNECTING = 2;
  public static final int STATE_CONNECTED = 3;

  public BluetoothService(Context context, Handler handler) {
    this.mHandler = handler;
  }

  public synchronized boolean isAvailable() {
    return this.mAdapter != null;
  }

  public synchronized boolean isBTopen() {
    return this.mAdapter.isEnabled();
  }

  public synchronized BluetoothDevice getDevByMac(String mac) {
    return this.mAdapter.getRemoteDevice(mac);
  }

  public synchronized BluetoothDevice getDevByName(String name) {
    BluetoothDevice tem_dev = null;
    Set pairedDevices = this.getPairedDev();
    if (pairedDevices.size() > 0) {
      Iterator var5 = pairedDevices.iterator();

      while (var5.hasNext()) {
        BluetoothDevice device = (BluetoothDevice) var5.next();
        if (device.getName().indexOf(name) != -1) {
          tem_dev = device;
          break;
        }
      }
    }

    return tem_dev;
  }

  public synchronized void sendMessage(String message, String charset) {
    if (message.length() > 0) {
      byte[] send;
      try {
        send = message.getBytes(charset);
      } catch (UnsupportedEncodingException var5) {
        send = message.getBytes();
      }

      this.write(send);
      byte[] tail = new byte[]{10, 13, 0};
      this.write(tail);
    }

  }

  public synchronized Set<BluetoothDevice> getPairedDev() {
    Set dev = null;
    dev = this.mAdapter.getBondedDevices();
    return dev;
  }

  public synchronized boolean cancelDiscovery() {
    return this.mAdapter.cancelDiscovery();
  }

  public synchronized boolean isDiscovering() {
    return this.mAdapter.isDiscovering();
  }

  public synchronized boolean startDiscovery() {
    return this.mAdapter.startDiscovery();
  }

  private synchronized void setState(int state) {
    this.mState = state;
    this.mHandler.obtainMessage(1, state, -1).sendToTarget();
  }

  public synchronized int getState() {
    return this.mState;
  }

  public synchronized void start() {
    if (this.mConnectThread != null) {
      this.mConnectThread.cancel();
      this.mConnectThread = null;
    }

    if (this.mConnectedThread != null) {
      this.mConnectedThread.cancel();
      this.mConnectedThread = null;
    }

    if (this.mAcceptThread == null) {
      this.mAcceptThread = new BluetoothService.AcceptThread();
      this.mAcceptThread.start();
    }

    this.setState(1);
  }

  public synchronized void connect(BluetoothDevice device) {
    Log.d("BluetoothService", "connect to: " + device);
    if (this.mState == 2 && this.mConnectThread != null) {
      this.mConnectThread.cancel();
      this.mConnectThread = null;
    }

    if (this.mConnectedThread != null) {
      this.mConnectedThread.cancel();
      this.mConnectedThread = null;
    }

    this.mConnectThread = new BluetoothService.ConnectThread(device);
    this.mConnectThread.start();
    this.setState(2);
  }

  public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
    Log.d("BluetoothService", "connected");
    if (this.mConnectThread != null) {
      this.mConnectThread.cancel();
      this.mConnectThread = null;
    }

    if (this.mConnectedThread != null) {
      this.mConnectedThread.cancel();
      this.mConnectedThread = null;
    }

    if (this.mAcceptThread != null) {
      this.mAcceptThread.cancel();
      this.mAcceptThread = null;
    }

    this.mConnectedThread = new BluetoothService.ConnectedThread(socket);
    this.mConnectedThread.start();
    Message msg = this.mHandler.obtainMessage(4);
    this.mHandler.sendMessage(msg);
    this.setState(3);
  }

  public synchronized void stop() {
    Log.d("BluetoothService", "stop");
    this.setState(0);
    if (this.mConnectThread != null) {
      this.mConnectThread.cancel();
      this.mConnectThread = null;
    }

    if (this.mConnectedThread != null) {
      this.mConnectedThread.cancel();
      this.mConnectedThread = null;
    }

    if (this.mAcceptThread != null) {
      this.mAcceptThread.cancel();
      this.mAcceptThread = null;
    }
  }

  public void write(byte[] out) {
    BluetoothService.ConnectedThread r;
    synchronized (this) {
      if (this.mState != 3) {
        return;
      }

      r = this.mConnectedThread;
    }

    r.write(out);
  }

  private void connectionFailed() {
    this.setState(1);
    Message msg = this.mHandler.obtainMessage(6);
    this.mHandler.sendMessage(msg);
  }

  private void connectionLost() {
    Message msg = this.mHandler.obtainMessage(5);
    this.mHandler.sendMessage(msg);
  }

  private class AcceptThread extends Thread {
    private final BluetoothServerSocket mmServerSocket;

    public AcceptThread() {
      BluetoothServerSocket tmp = null;

      try {
        tmp = BluetoothService.this.mAdapter.listenUsingRfcommWithServiceRecord("BTPrinter", BluetoothService.MY_UUID);
      } catch (IOException var4) {
        Log.e("BluetoothService", "listen() failed", var4);
      }

      this.mmServerSocket = tmp;
    }

    public void run() {
      Log.d("BluetoothService", "BEGIN mAcceptThread" + this);
      this.setName("AcceptThread");
      BluetoothSocket socket = null;

//      while (BluetoothService.this.mState != 3) {
      if (BluetoothService.this.mState != 3) {
        Log.d("AcceptThread线程运行", "正在运行......");

        try {
          if (this.mmServerSocket != null)
            socket = this.mmServerSocket.accept();
        } catch (IOException var6) {
          Log.e("BluetoothService", "accept() failed", var6);
//          break;
        }

        if (socket != null) {
          BluetoothService e = BluetoothService.this;
          synchronized (BluetoothService.this) {
            switch (BluetoothService.this.mState) {
              case 0:
              case 3:
                try {
                  socket.close();
                } catch (IOException var4) {
                  Log.e("BluetoothService", "Could not close unwanted socket", var4);
                }
                break;
              case 1:
              case 2:
                BluetoothService.this.connected(socket, socket.getRemoteDevice());
            }
          }
        }
      }

      Log.i("BluetoothService", "END mAcceptThread");
    }

    public void cancel() {
      Log.d("BluetoothService", "cancel " + this);

      try {
        if (this.mmServerSocket != null)
          this.mmServerSocket.close();
      } catch (IOException var2) {
        Log.e("BluetoothService", "close() of server failed", var2);
      }

    }
  }

  private class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;

    public ConnectThread(BluetoothDevice device) {
      this.mmDevice = device;
      BluetoothSocket tmp = null;

      try {
        tmp = device.createRfcommSocketToServiceRecord(BluetoothService.MY_UUID);
      } catch (IOException var5) {
        Log.e("BluetoothService", "create() failed", var5);
      }

      this.mmSocket = tmp;
    }

    public void run() {
      Log.i("BluetoothService", "BEGIN mConnectThread");
      this.setName("ConnectThread");
      BluetoothService.this.mAdapter.cancelDiscovery();

      try {
        this.mmSocket.connect();
      } catch (IOException var5) {
        BluetoothService.this.connectionFailed();

        try {
          this.mmSocket.close();
        } catch (IOException var3) {
          Log.e("BluetoothService", "unable to close() socket during connection failure", var3);
        }

        BluetoothService.this.start();
        return;
      }

      BluetoothService e = BluetoothService.this;
      synchronized (BluetoothService.this) {
        BluetoothService.this.mConnectThread = null;
      }

      BluetoothService.this.connected(this.mmSocket, this.mmDevice);
    }

    public void cancel() {
      try {
        this.mmSocket.close();
      } catch (IOException var2) {
        Log.e("BluetoothService", "close() of connect socket failed", var2);
      }

    }
  }

  private class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    public ConnectedThread(BluetoothSocket socket) {
      Log.d("BluetoothService", "create ConnectedThread");
      this.mmSocket = socket;
      InputStream tmpIn = null;
      OutputStream tmpOut = null;

      try {
        tmpIn = socket.getInputStream();
        tmpOut = socket.getOutputStream();
      } catch (IOException var6) {
        Log.e("BluetoothService", "temp sockets not created", var6);
      }

      this.mmInStream = tmpIn;
      this.mmOutStream = tmpOut;
    }

    public void run() {
      Log.d("ConnectedThread线程运行", "正在运行......");
      Log.i("BluetoothService", "BEGIN mConnectedThread");

      try {
        while (true) {
          byte[] e = new byte[256];
          int bytes = this.mmInStream.read(e);
          if (bytes <= 0) {
            Log.e("BluetoothService", "disconnected");
            BluetoothService.this.connectionLost();
            if (BluetoothService.this.mState != 0) {
              Log.e("BluetoothService", "disconnected");
              BluetoothService.this.start();
            }
            break;
          }

          BluetoothService.this.mHandler.obtainMessage(2, bytes, -1, e).sendToTarget();
        }
      } catch (IOException var3) {
        Log.e("BluetoothService", "disconnected", var3);
        BluetoothService.this.connectionLost();
        if (BluetoothService.this.mState != 0) {
          BluetoothService.this.start();
        }
      }

    }

    public void write(byte[] buffer) {
      try {
        this.mmOutStream.write(buffer);
        BluetoothService.this.mHandler.obtainMessage(3, -1, -1, buffer).sendToTarget();
      } catch (IOException var3) {
        Log.e("BluetoothService", "Exception during write", var3);
      }

    }

    public void cancel() {
      try {
        this.mmSocket.close();
      } catch (IOException var2) {
        Log.e("BluetoothService", "close() of connect socket failed", var2);
      }

    }
  }
}
