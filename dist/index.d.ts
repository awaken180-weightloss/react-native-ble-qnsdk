import { ConnectionStatus, ConnectionStatusEmitter, DeviceInfoEvent, FinalMeasurementEvent, FinalMeasurementResponse, IYolandaUser, ScaleEventChangeEvent, ScaleStateChangeEvent, TemporaryMeasurementEvent, TemporaryMeasurementResponse, YolandaDeviceInfo, YolandaEventEmitter, YolandaEventTypeEnum } from "./types";
declare const BleQnsdk: any;
declare function buildYolandaUser(user: IYolandaUser): Promise<number>;
declare function startYolandaScan(): Promise<void>;
declare function stopYolandaScan(): Promise<void>;
declare function fetchConnectedDeviceInfo(): void;
declare function disconnectDevice(): Promise<void>;
export { BleQnsdk, buildYolandaUser, startYolandaScan, stopYolandaScan, fetchConnectedDeviceInfo, disconnectDevice, IYolandaUser, YolandaDeviceInfo, TemporaryMeasurementResponse, FinalMeasurementResponse, ConnectionStatus, TemporaryMeasurementEvent, FinalMeasurementEvent, ScaleEventChangeEvent, ScaleStateChangeEvent, DeviceInfoEvent, ConnectionStatusEmitter, YolandaEventEmitter, YolandaEventTypeEnum, };
