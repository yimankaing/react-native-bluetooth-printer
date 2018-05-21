import React, {Component} from 'react';
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

export default class App extends React.Component {
  constructor(props) {
    super(props);
  }

  captureView = async () => {
    this.refs.viewShot.capture().then(uri => {
      let path = RNFS.ExternalDirectoryPath + '/invoice.jpg';
      RNFS.moveFile(uri, path)
        .then((success) => {
          console.log('FILE MOVED!');
        })
        .catch((err) => {
          console.log(err.message);
        });

    });
  };

  render() {
    let invoicePath = `${RNFS.ExternalDirectoryPath}/invoice.jpg`;
    return (
      <View style={styles.container}>
        <ViewShot ref="viewShot"
                  options={{format: "jpg", quality: 0.5}}>
          <Button title="Connect"
                  onPress={() => PrinterManager.connect()}/>
          <Button title="Print Text"
                  onPress={() => PrinterManager.printText("Hello")}/>
          <Button title="Print view shot"
                  onPress={() => PrinterManager.printInvoice(invoicePath)}/>
          <Button title="Disconnect"
                  onPress={() => PrinterManager.disconnect()}/>
          <Button title="Capture"
                  onPress={() => this.captureView()}/>
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
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});
