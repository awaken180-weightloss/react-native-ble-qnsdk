//
//  BleBleQnsdk.swift
//  react-native-ble-qnsdk
//
//  Created by Jeffrey Drakos on 4/10/20.
//

import React // for RCTEventEmitter
import Foundation
import CoreBluetooth

@objc(BleQnsdk)
public class BleQnsdk: RCTEventEmitter  {
    var bleApi: QNBleApi!
    var user: QNUser!
    var device: QNBleDevice!
    
    override public func supportedEvents() -> [String]! {
        return [EventEmitterState.uploadProgress.rawValue]
    }
    
    override init() {
        super.init()
        bleApi = QNBleApi.shared()

        let podBundle = Bundle(for: BleQnsdk.self)

        if let fileURL = podBundle.url(forResource: "Awaken202401", withExtension: "qn") {
            let filePath = fileURL.path
            bleApi.initSdk("Awaken202401", firstDataFile: filePath, callback: { error in
                // Handle the callback
                self.bleApi.discoveryListener = self
                self.bleApi.connectionChangeListener = self
                self.bleApi.dataListener = self
            })
        } else {
            print("Failed to locate the resource file in the pod bundle.")
            // Handle the error appropriately
        }
    }
    
    
    @objc func buildUser(_ birthday: String, height: Int, gender: String, id: String, unit: Int, athleteType: Int, resolver resolve: @escaping RCTPromiseResolveBlock,
                         rejecter reject: @escaping RCTPromiseRejectBlock) {
        let birthdayDateFormatter = DateFormatter()
        birthdayDateFormatter.dateFormat = "yyyy/MM/dd"
        let date = birthdayDateFormatter.date(from: birthday)
        
        self.user = bleApi.buildUser(id, height: Int32(height), gender: gender, birthday: date, callback: { error in
            if (error != nil) {
                self.sendEvent(
                    withName: EventEmitterState.uploadProgress.rawValue,
                    body: [
                        "type": EventEmitterState.connectionStatus.rawValue,
                        "value": [
                            "status": ConnectionStatusState.error.rawValue,
                            "error": error,
                            "description": ConnectionStatusDescription.buildUserErrorDescription.rawValue
                        ]
                    ]
                )
            }
            
        })
        
        self.user.athleteType = athleteType == 1 ? YLAthleteType.sport :YLAthleteType.default
        let config = bleApi.getConfig()
        
        config?.unit = unit == 0 ? QNUnit.KG : QNUnit.LB
        resolve(true)
    }
    
    @objc
    func onStartDiscovery(_ resolve: @escaping RCTPromiseResolveBlock,
                          rejecter reject: @escaping RCTPromiseRejectBlock) {
        bleApi.startBleDeviceDiscovery { error in
            if let error = error {
                let errorMessage = "Failed to start the scan method, reason: \(error)"
                let errorCode = "BLE_DISCOVERY_ERROR"
                reject(errorCode, errorMessage, error)
                return
            }
            
            resolve(true)
        }
    }
    
    @objc
    func onStopDiscovery(resolve: @escaping RCTPromiseResolveBlock,
                         rejecter reject: @escaping RCTPromiseRejectBlock) {
        bleApi.stopBleDeviceDiscorvery { error in
            if let error = error {
                let errorMessage = "Failed to stop the scan method, reason: \(error)"
                let errorCode = "BLE_STOP_DISCOVERY_ERROR"
                reject(errorCode, errorMessage, error)
                return
            }
            
            resolve(true)
        }
    }
    
    @objc
    func fetchConnectedDeviceInfo() {
        sendConnectedDeviceInfo()
    }
    
    func getDeviceInfo(device: QNBleDevice!) -> [String : Any] {
        return [
            "id": device.mac,
            "name": device.name,
            "modeId": device.modeId,
            "bluetoothName": device.bluetoothName,
            "deviceType": device.deviceType,
            "maxUserNum": device.maxUserNum,
            "registeredUserNum": device.registeredUserNum,
            "firmwareVer": device.firmwareVer,
            "hardwareVer": device.hardwareVer,
            "softwareVer": device.softwareVer
        ]
    }
    
    func getConnectedDeviceInfo() -> [String : Any] {
        if (self.device == nil) {
            return [:]
        }
        
        return getDeviceInfo(device: self.device)
    }
    
    func onTryConnect() {
        connectToDevice()
    }
    
    func connectToDevice() {
        onStopDiscovery(
            resolve: { _ in
                self.startConnection()
            },
            rejecter: { (_, _, _) in
                self.startConnection()
            }
        )
    }
    
    func startConnection() {
        bleApi.connect(self.device, user: self.user, callback: { error in
            if let error = error {
                self.handleConnectionError(error)
            }
        })
    }
    
    func sendConnectedDeviceInfo() {
        self.sendEvent(
            withName: EventEmitterState.uploadProgress.rawValue,
            body: [
                "type": EventEmitterState.deviceInfo.rawValue,
                "value": getConnectedDeviceInfo()
            ]
        )
    }
    
    
    func handleConnectionError(_ error: Error) {
        self.sendEvent(
            withName: EventEmitterState.uploadProgress.rawValue,
            body: [
                "type": EventEmitterState.connectionStatus.rawValue,
                "value": [
                    "status": ConnectionStatusState.error.rawValue,
                    "error": error,
                    "description": ConnectionStatusDescription.connectionErrorDescription.rawValue
                ]
            ]
        )
    }
    
    func convertPoundsToGrams(_ weight: Double) -> Double {
        var finalWeight = weight * 1000
        
        // In order to have the same value for lb's in the app, convert from lb's to grams
        if bleApi.getConfig().unit == QNUnit.LB {
            let pounds = bleApi.convertWeight(withTargetUnit: weight, unit: QNUnit.LB)
            let convertedWeight = 453.59237 * pounds
            finalWeight = convertedWeight
        }
        
        return finalWeight
    }
    
}

extension BleQnsdk: QNBleDeviceDiscoveryListener {
    public func onStartScan() {
        self.sendEvent(
            withName: EventEmitterState.uploadProgress.rawValue,
            body: [
                "type": EventEmitterState.logging.rawValue,
                "value": [
                    "status": LoggingState.startScan.rawValue
                ]
            ]
        )
    }
    
    public func onDeviceDiscover(_ device: QNBleDevice!) {
        self.device = device
        self.onTryConnect()
        self.sendEvent(
            withName: EventEmitterState.uploadProgress.rawValue,
            body: [
                "type": EventEmitterState.logging.rawValue,
                "value": [
                    "status": LoggingState.deviceDiscovered.rawValue,
                    "device":  getDeviceInfo(device: device)
                ]
            ]
        )
    }
}

extension BleQnsdk: QNBleConnectionChangeListener {
    public func onDisconnected(_ device: QNBleDevice!) {
        
        self.sendEvent(
            withName: EventEmitterState.uploadProgress.rawValue,
            body: [
                "type": EventEmitterState.connectionStatus.rawValue,
                "value": [
                    "status": ConnectionStatusState.onDisconnected.rawValue
                ]
            ]
        )
        
    }
    public func onConnecting(_ device: QNBleDevice!) {
        
        self.sendEvent(
            withName: EventEmitterState.uploadProgress.rawValue,
            body: [
                "type": EventEmitterState.connectionStatus.rawValue,
                "value": [
                    "status": ConnectionStatusState.onConnecting.rawValue
                ]
            ]
        )
    }
    
    public func onConnected(_ device: QNBleDevice!) {
        self.device = device
        
        self.sendEvent(
            withName: EventEmitterState.uploadProgress.rawValue,
            body: [
                "type": EventEmitterState.connectionStatus.rawValue,
                "value": [
                    "status": ConnectionStatusState.onConnected.rawValue,
                    "device": getDeviceInfo(device: device)
                ]
            ]
        )
        
        self.sendConnectedDeviceInfo()
    }
    
    public func onServiceSearchComplete(_ device: QNBleDevice!) {
        self.sendEvent(
            withName: EventEmitterState.uploadProgress.rawValue,
            body: [
                "type": EventEmitterState.connectionStatus.rawValue,
                "value": [
                    "status": ConnectionStatusState.onServiceSearchComplete.rawValue
                ]
            ]
        )
    }
    
    public func onDisconnecting(_ device: QNBleDevice!) {
        self.sendEvent(
            withName: EventEmitterState.uploadProgress.rawValue,
            body: [
                "type": EventEmitterState.connectionStatus.rawValue,
                "value": [
                    "status": ConnectionStatusState.onDisconnecting.rawValue
                ]
            ]
        )
    }
    
    public func onConnectError(_ device: QNBleDevice!, error: Error!) {
        self.sendEvent(
            withName: EventEmitterState.uploadProgress.rawValue,
            body: [
                "type": EventEmitterState.connectionStatus.rawValue,
                "value": [
                    "status": ConnectionStatusState.error.rawValue,
                    "error": error
                ]
            ]
        )
    }
    
}

extension BleQnsdk: QNScaleDataListener {
    public func onGetUnsteadyWeight(_ device: QNBleDevice!, weight: Double) {
        let finalWeight = convertPoundsToGrams(weight)
        
        
        self.sendEvent(
            withName: EventEmitterState.uploadProgress.rawValue,
            body: [
                "type": EventEmitterState.temporaryMeasurementReceived.rawValue,
                "value": [
                    "weight": finalWeight
                ]
            ]
        )
    }
    
    public func filterResponse(_ scaleData: [QNScaleItemData]) -> [String:Any]? {
        var response = [String:Any]()
        for item in scaleData {
            
            if (item.name == "BMR") {
                response["basalMetabolicRate"] = item.value
            }
            if (item.name == "visceral fat") {
                response["visceralFatTanita"] = item.value
            }
            if (item.name == "weight") {
                response["weight"] = convertPoundsToGrams(item.value)
            }
            if (item.name == "lean body weight") {
                response["fatFreeMass"] = (item.value * 1000)
            }
            if (item.name == "body fat rate") {
                response["bodyFat"] = (item.value * 1000)
            }
            if (item.name == "body water rate") {
                response["waterPercentage"] = (item.value * 1000)
            }
            if (item.name == "muscle rate") {
                response["skeletalMuscleRatio"] = (item.value * 1000)
            }
            if (item.name == "muscle mass") {
                response["muscleMass"] = (item.value * 1000)
            }
            
        }
        
        return response
    }
    
    public func onGetScaleData(_ device: QNBleDevice!, data scaleData: QNScaleData!) {
        let data = self.filterResponse(scaleData.getAllItem())
        
        self.sendEvent(
            withName: EventEmitterState.uploadProgress.rawValue,
            body: [
                "type": EventEmitterState.finalMeasurementReceived.rawValue,
                "value": data
            ]
        )
        
    }
    
    public func onGetStoredScale(_ device: QNBleDevice!, data storedDataList: [QNScaleStoreData]!) {
        self.sendEvent(
            withName: EventEmitterState.uploadProgress.rawValue,
            body: [
                "type": EventEmitterState.logging.rawValue,
                "value": [
                    "status": LoggingState.onGetStoredScale.rawValue
                ]
            ]
        )   
    }
    
    public func onGetElectric(_ electric: UInt, device: QNBleDevice!) {
        self.sendEvent(
            withName: EventEmitterState.uploadProgress.rawValue,
            body: [
                "type": EventEmitterState.logging.rawValue,
                "value": [
                    "status": LoggingState.onGetElectric.rawValue
                ]
            ]
        )
    }
    
    public func onScaleStateChange(_ device: QNBleDevice!, scaleState state: QNScaleState) {
        self.sendEvent(
            withName: EventEmitterState.uploadProgress.rawValue,
            body: [
                "type": EventEmitterState.scaleStateChange.rawValue,
                "value": state.rawValue
            ]
        )
    }
    
    public func onScaleEventChange(_ device: QNBleDevice!, scaleEvent: QNScaleEvent) {
        self.sendEvent(
            withName: EventEmitterState.uploadProgress.rawValue,
            body: [
                "type": EventEmitterState.scaleEventChange.rawValue,
                "value": scaleEvent
            ]
        )
    }
}