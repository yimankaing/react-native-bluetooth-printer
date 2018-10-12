# react_native_bluetooth_printer (Android, iOS = future)
## {
- Android project
- Turn on bluetooth
- Connect paired devices
- Scan for available devices
- Capture component using [react-native-view-shot](https://github.com/gre/react-native-view-shot)
- Moving file using [react-native-fs](https://github.com/itinance/react-native-fs)
- Print Text and Image
## }

> I'm sorry this is not a react-native package. You have to copy some files and modified or add some line to native project. 

## <b>Android project</b>

const <b>androidApp</b> = android/app/src/main/java/com/native-module;<br>
const <b>resources</b> = android/app/src/main/res;

### FILE TO BE ADDED:
- androidApp/bluetooth_printer/ (3 files)
    - BluetoothService.java
    - DeviceListActivity.java
    - PrintPicture.java
- androidApp/Constants.java  (I use Khmer language)
- androidApp/PrinterManager.java
- androidApp/PrinterPackage.java
- resources/values/* (3 files)
- resources/layout/ (2 files)
    - device_list.xml
    - device_name.xml
- resources/anim/* (2 files)

### FILE TO BE MODIFIED:
- <b>AndroidManifest.xml</b>
    - permissions
    ```
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    ```
    - device list activity
    ```
    <application ...>
    <activity
            android:name=".bluetooth_printer.DeviceListActivity"
            android:theme="@style/DeviceListTheme" />
    </application>
    ```
- <b>MainApplication.java</b>
  ```
  @Override
        protected List<ReactPackage> getPackages() {
            return Arrays.<ReactPackage>asList(
                    new MainReactPackage(),
                    ...
                    new PrinterPackage()
            );
        }
  ```
- <b>MainActivity.java</b>
  
  ```
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    ...
    super.onCreate(savedInstanceState);
    /*INIT BLUETOOTH SERVICE*/
    PrinterManager.mService = new BluetoothService(getApplicationContext(), mHandler);
    ...
  }
  
  @Override
  public void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
  }

  /*MESSAGE HANDLER BLUETOOTH STATE*/
  @SuppressLint("HandlerLeak")
  private final Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case BluetoothService.MESSAGE_STATE_CHANGE:
          switch (msg.arg1) {
            case BluetoothService.STATE_CONNECTED:
              Toast.makeText(getApplicationContext(), Constants.connected, Toast.LENGTH_SHORT).show();
              break;
            case BluetoothService.STATE_CONNECTING:
              Toast.makeText(getApplicationContext(), Constants.connecting, Toast.LENGTH_SHORT).show();
              break;
            case BluetoothService.STATE_LISTEN:
            case BluetoothService.STATE_NONE:
              break;
          }
          break;
        case BluetoothService.MESSAGE_CONNECTION_LOST:
          Toast.makeText(getApplicationContext(), Constants.disconnected, Toast.LENGTH_SHORT).show();
          break;
        case BluetoothService.MESSAGE_UNABLE_CONNECT:
          Toast.makeText(getApplicationContext(), Constants.unable_to_connect, Toast.LENGTH_SHORT).show();
          break;
      }
    }
  };

  @Override
  public void onStart() {
    super.onStart();
  }

  /*Destroy*/
  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (PrinterManager.mService != null)
      PrinterManager.mService.stop();
    PrinterManager.mService = null;
  }
  ```

  #### 

## <b>React native project</b>

```
import React, { Component } from 'react';
import {
  Platform,
  StyleSheet,
  Text,
  View,
  Button,
  NativeModules,
} from 'react-native';
import ViewShot from "react-native-view-shot";
import RNFS from "react-native-fs";

const PrinterManager = NativeModules.PrinterManager;
const imageType = "png";
const imagePath = `${RNFS.ExternalDirectoryPath}/image.${imageType}`;


export default class App extends React.Component {
  constructor(props) {
    super(props);
  }

  captureView = async () => {
    this.refs.viewShot.capture().then(uri => {
      RNFS.moveFile(uri, imagePath)
        .then((success) => {
          console.log('FILE MOVED!');
          //this.printViewShot(imagePath)
        })
        .catch((err) => {
          console.log(err.message);
        });

    });
  };

  printViewShot = (imagePath) => {
    PrinterManager.printImage(imagePath, (res) => {
      console.log(res)
      if (res === 'connected') {
        //do something
      } else {
        //do something
      }
    });
  };

  render() {
    return (
      <View style={styles.container}>
        <ViewShot ref="viewShot"
          options={{ format: "jpg", quality: 0.9 }}>
          <Button title="Connect"
            onPress={() => PrinterManager.connect()} />
          <Button title="Print Text"
            onPress={() => PrinterManager.printText("Hello")} />
          <Button title="Print view shot"
            onPress={() => this.printViewShot(imagePath)} />
          <Button title="Disconnect"
            onPress={() => PrinterManager.disconnect()} />
          <Button title="Capture"
            onPress={() => this.captureView()} />
        </ViewShot>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
});


```