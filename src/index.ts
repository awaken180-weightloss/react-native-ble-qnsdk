import {
  ConnectionStatus,
  ConnectionStatusEmitter,
  DeviceInfoEvent,
  FinalMeasurementEvent,
  FinalMeasurementResponse,
  IYolandaUser,
  ScaleEventChangeEvent,
  ScaleStateChangeEvent,
  TemporaryMeasurementEvent,
  TemporaryMeasurementResponse,
  YolandaDeviceInfo,
  YolandaEventEmitter,
  YolandaEventTypeEnum,
} from "./types";
import { NativeModules, Platform } from "react-native";

const LINKING_ERROR =
  `The package 'react-native-ble-qnsdk' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: "" }) +
  "- You rebuilt the app after installing the package\n" +
  "- You are not using Expo Go\n";

const BleQnsdk = NativeModules.BleQnsdk
  ? NativeModules.BleQnsdk
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

function buildYolandaUser(user: IYolandaUser): Promise<number> {
  return BleQnsdk.buildUser(
    user.birthday,
    user.height,
    user.gender,
    user.id,
    user.unit,
    user.athleteType
  );
}

function startYolandaScan(): Promise<void> {
  return BleQnsdk.onStartDiscovery();
}

function stopYolandaScan(): Promise<void> {
  return BleQnsdk.onStopDiscovery();
}

function fetchConnectedDeviceInfo(): void {
  return BleQnsdk.fetchConnectedDeviceInfo();
}

function disconnectDevice(): Promise<void> {
  return BleQnsdk.disconnectDevice();
}

export {
  BleQnsdk,
  buildYolandaUser,
  startYolandaScan,
  stopYolandaScan,
  fetchConnectedDeviceInfo,
  disconnectDevice,
  IYolandaUser,
  YolandaDeviceInfo,
  TemporaryMeasurementResponse,
  FinalMeasurementResponse,
  ConnectionStatus,
  TemporaryMeasurementEvent,
  FinalMeasurementEvent,
  ScaleEventChangeEvent,
  ScaleStateChangeEvent,
  DeviceInfoEvent,
  ConnectionStatusEmitter,
  YolandaEventEmitter,
  YolandaEventTypeEnum,
};
