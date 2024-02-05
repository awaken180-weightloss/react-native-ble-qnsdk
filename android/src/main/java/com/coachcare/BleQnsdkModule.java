package com.coachcare;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.IllegalViewOperationException;

import com.qn.device.listener.QNScaleDataListener;
import com.qn.device.listener.QNResultCallback;
import com.qn.device.out.QNBleApi;
import com.qn.device.constant.CheckStatus;
import com.qn.device.constant.QNIndicator;
import com.qn.device.constant.UserGoal;
import com.qn.device.constant.UserShape;
import com.qn.device.listener.QNBleDeviceDiscoveryListener;
import com.qn.device.out.QNBleBroadcastDevice;
import com.qn.device.out.QNBleDevice;
import com.qn.device.out.QNBleKitchenDevice;
import com.qn.device.out.QNConfig;
import com.qn.device.out.QNScaleData;
import com.qn.device.out.QNScaleItemData;
import com.qn.device.out.QNScaleStoreData;
import com.qn.device.out.QNUser;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.qn.device.listener.QNBleConnectionChangeListener;

public class BleQnsdkModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private final ReactApplicationContext reactContext;
    private QNBleApi mQNBleApi;

    private QNBleDevice connectedDevice;
    public User mUser = new User();
    public static final String FORMAT_SHORT = "yyyy-MM-dd";

    private Handler backgroundHandler;

    public BleQnsdkModule(ReactApplicationContext reactContext) {
        super(reactContext);

        reactContext.addLifecycleEventListener(this);
        this.reactContext = reactContext;

        HandlerThread handlerThread = new HandlerThread("BleQnsdkModuleInit");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());

        // Move initialization to the background thread
        backgroundHandler.post(() -> {
            this.initializeListeners();
        });
    }

    public void initializeListeners() {
        mQNBleApi = QNBleApi.getInstance(reactContext);
        initSDK();
        this.setConfig();

        this.setDiscoveryListener();
        this.setConnectionListener();
        this.setDataListener();
    }

    public void setConfig() {
        QNConfig mQnConfig = mQNBleApi.getConfig();
        mQnConfig.setNotCheckGPS(true);
        mQnConfig.setAllowDuplicates(false);
        mQnConfig.setDuration(0);
        mQnConfig.setOnlyScreenOn(false);

        mQnConfig.save(new QNResultCallback() {
            @Override
            public void onResult(int i, String s) {
                Log.d("Yolanda setConfig", "initData:" + s);
            }
        });
    }

    public void initSDK() {
            String encryptPath = "file:///android_asset/Awaken202401.qn";
            mQNBleApi.initSdk("Awaken202401", encryptPath, new QNResultCallback() {
            @Override
            public void onResult(int code, String msg) {
                Log.d("Yolanda Scale", "Initialization file\n" + msg);
                Log.d("Yolanda Scale", "Initialization code\n" + code);
            }
        });

    }

    @Override
    public void onHostResume() {
        Log.w("Yolanda Scale", "on onHostResume");
    }

    @Override
    public void onHostPause() {
        Log.w("Yolanda Scale", "on onHostPause");
    }

    @Override
    public void onHostDestroy() {
        Log.w("Yolanda Scale", "on onHostDestroy");
    }

    private QNUser createQNUser() {
        UserShape userShape;
        switch (mUser.getChoseShape()) {
            case 1:
                userShape = UserShape.SHAPE_SLIM;
                break;
            case 2:
                userShape = UserShape.SHAPE_NORMAL;
                break;
            case 3:
                userShape = UserShape.SHAPE_STRONG;
                break;
            case 4:
                userShape = UserShape.SHAPE_PLIM;
                break;
            default:
                userShape = UserShape.SHAPE_NONE;
                break;
        }

        UserGoal userGoal;
        switch (mUser.getChoseGoal()) {
            case 1:
                userGoal = UserGoal.GOAL_LOSE_FAT;
                break;
            case 2:
                userGoal = UserGoal.GOAL_STAY_HEALTH;
                break;
            case 3:
                userGoal = UserGoal.GOAL_GAIN_MUSCLE;
                break;
            case 4:
                userGoal = UserGoal.POWER_OFTEN_EXERCISE;
                break;
            case 5:
                userGoal = UserGoal.POWER_LITTLE_EXERCISE;
                break;
            case 6:
                userGoal = UserGoal.POWER_OFTEN_RUN;
                break;
            default:
                userGoal = UserGoal.GOAL_NONE;
                break;
        }

        return mQNBleApi.buildUser(mUser.getUserId(),
                mUser.getHeight(), mUser.getGender(), mUser.getBirthDay(), mUser.getAthleteType(),
                userShape, userGoal, mUser.getClothesWeight(), new QNResultCallback() {
                    @Override
                    public void onResult(int code, String msg) {
                        Log.d("Yolanda ConnectActivity", "Response:" + msg);
                    }
                });
    }

    @Override
    public String getName() {
        return "BleQnsdk";
    }

    @ReactMethod
    public void buildUser(String birthday, int height, String gender, String id, int unit, int athleteType,
            Promise promise) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_SHORT); // here set the pattern as you date in string was
                                                                       // containing like date/month/year
            Date formattedBirthday = sdf.parse(birthday);

            this.mUser.setAthleteType(athleteType);
            this.mUser.setBirthDay(formattedBirthday);
            this.mUser.setGender(gender);
            this.mUser.setHeight(height);
            this.mUser.setUserId(id);

            this.createQNUser();

            QNConfig mQnConfig = mQNBleApi.getConfig();
            mQnConfig.setUnit(unit);
            promise.resolve("Yolanda build user success");
        } catch (IllegalViewOperationException | ParseException e) {
            setBleStatusWithError(e, "Build user error");
            promise.reject("Yolanda build user reject", e);
        }
    }

    public WritableMap getDeviceInfo(QNBleDevice device) {
        WritableMap infoMap = Arguments.createMap();
        infoMap.putString("id", device.getMac());
        infoMap.putString("name", device.getName());
        infoMap.putString("modeId", device.getModeId());
        infoMap.putString("bluetoothName", device.getBluetoothName());
        infoMap.putInt("deviceType", device.getDeviceType());
        infoMap.putInt("maxUserNum", device.getMaxUserNum());
        infoMap.putInt("registeredUserNum", device.getRegisteredUserNum());
        infoMap.putInt("firmwareVer", device.getFirmwareVer());
        infoMap.putInt("hardwareVer", device.getHardwareVer());
        infoMap.putInt("softwareVer", device.getSoftwareVer());
        return infoMap;
    }

    public WritableMap getConnectedDeviceInfo() {
        if (connectedDevice == null) {
            return Arguments.createMap(); // Return an empty map if device is null
        }
        return getDeviceInfo(connectedDevice);
    }

    private void sendConnectedDeviceInfo() {
        WritableMap params = Arguments.createMap();

        params.putString("type", "deviceInfo");
        params.putMap("value", getConnectedDeviceInfo());
        sendEventToJS("uploadProgress", params);
    }

    private void connectToDevice(QNBleDevice device) {
        mQNBleApi.connectDevice(device, createQNUser(), new QNResultCallback() {
            @Override
            public void onResult(int code, String msg) {
                connectedDevice = device;
                sendConnectedDeviceInfo();
            }
        });
    }

    private void setBleStatus(String bleStatus) {
        WritableMap params = Arguments.createMap();
        WritableMap valueMap = Arguments.createMap();
        valueMap.putString("status", bleStatus);

        params.putString("type", "connectionStatus");
        params.putMap("value", valueMap);

        sendEventToJS("uploadProgress", params);
    }

    private void setBleStatusWithError(Exception error, String description) {
        WritableMap params = Arguments.createMap();
        WritableMap valueMap = Arguments.createMap();
        valueMap.putString("status", "error");
        valueMap.putString("error", String.valueOf(error));
        valueMap.putString("description", description);

        params.putString("type", "connectionStatus");
        params.putMap("value", valueMap);
        sendEventToJS("uploadProgress", params);
    }

    private void setBleStatusWithError(int error, String description) {
        WritableMap params = Arguments.createMap();
        WritableMap valueMap = Arguments.createMap();
        valueMap.putString("status", "error");
        valueMap.putString("error", String.valueOf(error));
        valueMap.putString("description", description);

        params.putString("type", "connectionStatus");
        params.putMap("value", valueMap);
        sendEventToJS("uploadProgress", params);
    }

    private void setConnectionListener() {
        mQNBleApi.setBleConnectionChangeListener(new QNBleConnectionChangeListener() {
            @Override
            public void onConnecting(QNBleDevice device) {
                setBleStatus("onConnecting");
            }

            @Override
            public void onConnected(QNBleDevice device) {
                setBleStatus("onConnected");
            }

            @Override
            public void onServiceSearchComplete(QNBleDevice device) {
                setBleStatus("onServiceSearchComplete");
            }

            @Override
            public void onStartInteracting(QNBleDevice qnBleDevice) {
                setBleStatus("onStartInteracting");
            }

            @Override
            public void onDisconnecting(QNBleDevice device) {
                setBleStatus("onDisconnecting");
            }

            @Override
            public void onDisconnected(QNBleDevice device) {
                setBleStatus("disconnected");
            }

            @Override
            public void onConnectError(QNBleDevice device, int errorCode) {
                setBleStatusWithError(errorCode, "onConnectError");
            }
        });
    }

    public void sendEventToJS(String eventName, WritableMap params) {
        this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    public void setDataListener() {

        mQNBleApi.setDataListener(new QNScaleDataListener() {
            public double convertWeight(Double weight) {
                try {
                    double finalWeight = weight * 1000;
                    int unit = mQNBleApi.getConfig().getUnit();

                    if (unit != 0) {
                        String convertedWeightAsString = mQNBleApi.convertWeightWithTargetUnit(weight, unit);
                        double pounds = Float.valueOf(convertedWeightAsString.split("lb")[0]);
                        double convertedWeight = 453.59237 * pounds;
                        finalWeight = convertedWeight;
                    }

                    return finalWeight;
                } catch (IllegalViewOperationException e) {
                    return weight;
                }
            };

            @Override
            public void onGetUnsteadyWeight(QNBleDevice device, double weight) {
                double finalWeight = convertWeight(weight);

                WritableMap valueMap = Arguments.createMap();
                WritableMap params = Arguments.createMap();

                valueMap.putDouble("weight", finalWeight);

                params.putString("type", "temporaryMeasurementReceived");
                params.putMap("value", valueMap);

                sendEventToJS("uploadProgress", params);
            }

            @Override
            public void onGetScaleData(QNBleDevice device, QNScaleData data) {
                WritableMap params = Arguments.createMap();
                WritableMap valueMap = Arguments.createMap();

                QNScaleItemData value = data.getItem(QNIndicator.TYPE_BMR);
                if (value != null) {
                    valueMap.putDouble("basalMetabolicRate", value.getValue());
                }

                value = data.getItem(QNIndicator.TYPE_VISFAT);
                if (value != null) {
                    valueMap.putDouble("visceralFatTanita", value.getValue());
                }

                value = data.getItem(QNIndicator.TYPE_WEIGHT);
                if (value != null) {
                    double finalWeight = convertWeight(value.getValue());

                    valueMap.putDouble("weight", finalWeight);
                }

                value = data.getItem(QNIndicator.TYPE_LBM);
                if (value != null) {
                    valueMap.putDouble("fatFreeMass", value.getValue() * 1000);
                }

                value = data.getItem(QNIndicator.TYPE_BODYFAT);
                if (value != null) {
                    valueMap.putDouble("bodyFat", value.getValue() * 1000);
                }

                value = data.getItem(QNIndicator.TYPE_WATER);
                if (value != null) {
                    valueMap.putDouble("waterPercentage", value.getValue() * 1000);
                }

                // muscle mass Muscle mass Kg
                value = data.getItem(QNIndicator.TYPE_MUSCLE_MASS);
                if (value != null) {
                    double finalWeight = convertWeight(value.getValue());
                    valueMap.putDouble("muscleMass", finalWeight);
                }

                // Skeletal muscle rate %
                value = data.getItem(QNIndicator.TYPE_MUSCLE);
                if (value != null) {
                    valueMap.putDouble("skeletalMuscleRatio", value.getValue() * 1000);
                }

                value = data.getItem(QNIndicator.TYPE_BONE);
                if (value != null) {
                    double finalWeight = convertWeight(value.getValue());
                    valueMap.putDouble("boneWeight", finalWeight);
                }

                params.putString("type", "finalMeasurementReceived");
                params.putMap("value", valueMap);

                sendEventToJS("uploadProgress", params);
            }

            @Override
            public void onGetStoredScale(QNBleDevice device, List<QNScaleStoreData> storedDataList) {
                Log.d("Yolanda onGetStordScale", String.valueOf(storedDataList));
            }

            @Override
            public void onGetElectric(QNBleDevice device, int electric) {
                Log.d("Yolanda onGetElectric ", String.valueOf(electric));
            }

            @Override
            public void onScaleStateChange(QNBleDevice device, int status) {
                WritableMap params = Arguments.createMap();

                params.putString("type", "scaleStateChange");
                params.putInt("value", status);
                sendEventToJS("uploadProgress", params);
            }

            @Override
            public void onScaleEventChange(QNBleDevice qnBleDevice, int i) {
                WritableMap params = Arguments.createMap();

                params.putString("type", "onScaleEventChange");
                params.putInt("value", i);

                sendEventToJS("uploadProgress", params);
            }

            @Override
            public void readSnComplete(QNBleDevice qnBleDevice, String s) {
                Log.d("Yolanda readSnComplete", String.valueOf(qnBleDevice));
            }

        });
    }

    public void setDiscoveryListener() {
        mQNBleApi.setBleDeviceDiscoveryListener(new QNBleDeviceDiscoveryListener() {
            @Override
            public void onDeviceDiscover(QNBleDevice device) {
                connectToDevice(device);
            }

            @Override
            public void onStartScan() {
                Log.d("Yolanda onStartScan", "start");
            }

            @Override
            public void onStopScan() {
                mQNBleApi.stopBleDeviceDiscovery(new QNResultCallback() {
                    @Override
                    public void onResult(int code, String msg) {
                    }
                });
            }

            @Override
            public void onScanFail(int code) {
                setBleStatusWithError(code, "onScanFail");
            }

            @Override
            public void onBroadcastDeviceDiscover(QNBleBroadcastDevice qnBleBroadcastDevice) {
                Log.d("Yolanda onBrodDiscover", String.valueOf(qnBleBroadcastDevice));

            }

            @Override
            public void onKitchenDeviceDiscover(QNBleKitchenDevice qnBleKitchenDevice) {
                Log.d("Yolanda onKitchDiscover", String.valueOf(qnBleKitchenDevice));
            }
        });
    }

    @ReactMethod
    public void onStartDiscovery(final Promise promise) {
        Handler mHandler = new Handler();
        mHandler.post(new Runnable() {

            @Override
            public void run() {

                mQNBleApi.startBleDeviceDiscovery(new QNResultCallback() {
                    @Override
                    public void onResult(int code, String msg) {
                        if (code == CheckStatus.OK.getCode()) {
                            promise.resolve(code);
                        } else {
                            setBleStatusWithError(code, "startBleDeviceDiscovery");
                            promise.reject("startBleDeviceDiscovery", String.valueOf(code));
                        }
                    }
                });
            }
        });
    }

    @ReactMethod
    public void onStopDiscovery(final Promise promise) {
        mQNBleApi.stopBleDeviceDiscovery(new QNResultCallback() {
            @Override
            public void onResult(int code, String msg) {
                if (code == CheckStatus.OK.getCode()) {
                    promise.resolve(code);
                } else {
                    promise.reject("onStopDiscovery", String.valueOf(code));
                }
            }
        });
    }

    @ReactMethod
    public void fetchConnectedDeviceInfo() {
        sendConnectedDeviceInfo();
    }
}
