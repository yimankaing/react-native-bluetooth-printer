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
