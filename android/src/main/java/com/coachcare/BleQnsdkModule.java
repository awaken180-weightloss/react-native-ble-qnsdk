package com.coachcare;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.qn.device.listener.QNBleConnectionChangeListener;

public class BleQnsdkModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private final ReactApplicationContext reactContext;
    private QNBleApi mQNBleApi;
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
        //    mQNBleApi = QNBleApi.getInstance(reactContext);
        //    initSDK();
        this.initializeListeners();
       });
    }

    public void initializeListeners() {
        Log.d("Yolanda Scale", "initializeListeners \n");

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
        String encryptPath = "file:///android_asset/Lexington202208.qn";
        mQNBleApi.initSdk("Lexington202004", encryptPath, new QNResultCallback() {
            @Override
            public void onResult(int code, String msg) {
                Log.d("Yolanda Scale", "Initialization file\n" + msg);
                Log.d("Yolanda Scale", "Initialization code\n" + code);
            }
        });

    }

    @Override
    public void onHostResume() {
        // this.initializeListeners();
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
    public void buildUser(String name, String birthday, int height, String gender, String id, int unit, int athleteType,
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
            Log.d("Yolanda Scale", "buildUser file\n");
            promise.resolve("build user success");
        } catch (IllegalViewOperationException | ParseException e) {
            Log.d("Yolanda CATCH ERROR", String.valueOf(e));
            setBleStatusWithError(e, "Build user error");
            promise.reject("build user reject", e);
        }
    }

    private void setBleStatus(String bleStatus, String deviceId) {
        WritableMap params = Arguments.createMap();
        WritableMap item = Arguments.createMap();
        params.putString("status", bleStatus);
        params.putString("deviceId", deviceId);
        item.putMap("connectionStatus", params);
        sendEventToJS("uploadProgress", item);
    }

    private void setBleStatus(String bleStatus) {
        WritableMap params = Arguments.createMap();
        WritableMap item = Arguments.createMap();
        params.putString("status", bleStatus);
        item.putMap("connectionStatus", params);
        sendEventToJS("uploadProgress", item);
    }

    private void setBleStatusWithError(Exception error, String description) {
        WritableMap params = Arguments.createMap();
        WritableMap item = Arguments.createMap();
        params.putString("status", "error");
        params.putString("error", String.valueOf(error));
        params.putString("description", description);
        item.putMap("connectionStatus", params);
        sendEventToJS("uploadProgress", item);
    }

    private void setBleStatusWithError(int error, String description) {
        WritableMap params = Arguments.createMap();
        WritableMap item = Arguments.createMap();
        params.putString("status", "error");
        params.putString("error", String.valueOf(error));
        params.putString("description", description);
        item.putMap("connectionStatus", params);
        sendEventToJS("uploadProgress", item);
    }

    private void setConnectionListener() {
        mQNBleApi.setBleConnectionChangeListener(new QNBleConnectionChangeListener() {
            @Override
            public void onConnecting(QNBleDevice device) {
                setBleStatus("onConnecting");
            }

            @Override
            public void onConnected(QNBleDevice device) {
                setBleStatus("onConnected", device.getModeId());
            }

            @Override
            public void onServiceSearchComplete(QNBleDevice device) {
                setBleStatus("onServiceSearchComplete");
            }

            @Override
            public void onStartInteracting(QNBleDevice qnBleDevice) {
                Log.w("Yolanda Scale", "on onStartInteracting");
                setBleStatus("onStartInteracting");
            }

            @Override
            public void onDisconnecting(QNBleDevice device) {
                setBleStatus("onDisconnecting");
            }

            @Override
            public void onDisconnected(QNBleDevice device) {
                WritableMap params = Arguments.createMap();
                params.putString("scaleStateChange", "0");
                sendEventToJS("uploadProgress", params);

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
            }

            ;

            @Override
            public void onGetUnsteadyWeight(QNBleDevice device, double weight) {
                double finalWeight = convertWeight(weight);

                WritableMap measurement = Arguments.createMap();
                WritableMap params = Arguments.createMap();
                measurement.putDouble("weight", finalWeight);
                params.putMap("measurement", measurement);
                sendEventToJS("uploadProgress", params);
            }

            @Override
            public void onGetScaleData(QNBleDevice device, QNScaleData data) {
                WritableMap params = Arguments.createMap();

                QNScaleItemData value = data.getItem(QNIndicator.TYPE_BMR);
                if (value != null) {
                    params.putDouble("basalMetabolicRate", value.getValue());
                }

                value = data.getItem(QNIndicator.TYPE_VISFAT);
                if (value != null) {
                    params.putDouble("visceralFatTanita", value.getValue());
                }

                value = data.getItem(QNIndicator.TYPE_WEIGHT);
                if (value != null) {
                    double finalWeight = convertWeight(value.getValue());

                    params.putDouble("weight", finalWeight);
                }

                value = data.getItem(QNIndicator.TYPE_LBM);
                if (value != null) {
                    params.putDouble("fatFreeMass", value.getValue() * 1000);
                }

                value = data.getItem(QNIndicator.TYPE_BODYFAT);
                if (value != null) {
                    params.putDouble("bodyFat", value.getValue() * 1000);
                }

                value = data.getItem(QNIndicator.TYPE_WATER);
                if (value != null) {
                    params.putDouble("waterPercentage", value.getValue() * 1000);
                }

                // muscle mass Muscle mass Kg
                value = data.getItem(QNIndicator.TYPE_MUSCLE_MASS);
                if (value != null) {
                    double finalWeight = convertWeight(value.getValue());
                    params.putDouble("muscleMass", finalWeight);
                }

                // Skeletal muscle rate %
                value = data.getItem(QNIndicator.TYPE_MUSCLE);
                if (value != null) {
                    params.putDouble("skeletalMuscleRatio", value.getValue() * 1000);
                }

                value = data.getItem(QNIndicator.TYPE_BONE);
                if (value != null) {
                    double finalWeight = convertWeight(value.getValue());
                    params.putDouble("boneWeight", finalWeight);
                }

                WritableMap measurement = Arguments.createMap();
                measurement.putMap("measurement", params);

                sendEventToJS("uploadProgress", measurement);
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
                params.putString("scaleStateChange", String.valueOf(status));
                sendEventToJS("uploadProgress", params);
            }

            @Override
            public void onScaleEventChange(QNBleDevice qnBleDevice, int i) {
                Log.d("Yolanda onEventChange", String.valueOf(qnBleDevice));
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
                mQNBleApi.connectDevice(device, createQNUser(), new QNResultCallback() {
                    @Override
                    public void onResult(int code, String msg) {
                        Log.d("Yolanda onDevDiscover", String.valueOf(device));
                    }
                });

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
                        Log.d("Yolanda onStopScan c", String.valueOf(code));
                        Log.d("Yolanda onStopScan m", String.valueOf(msg));
                        if (code == CheckStatus.OK.getCode()) {
                        }
                    }
                });
            }

            @Override
            public void onScanFail(int code) {
                Log.d("Yolanda onScanFail", String.valueOf(code));
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
    public void onStartDiscovery(String name, final Promise promise) {
        Handler mHandler = new Handler();
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                Log.d("Yolanda onStartDscovery", String.valueOf(name));
                mQNBleApi.startBleDeviceDiscovery(new QNResultCallback() {
                    @Override
                    public void onResult(int code, String msg) {
                        Log.d("Yolanda onResult code ", String.valueOf(code));
                        Log.d("Yolanda onResult mesg", String.valueOf(msg));
                        if (code == CheckStatus.OK.getCode()) {
                            promise.resolve(true);
                        }
                        if (code != CheckStatus.OK.getCode()) {
                            setBleStatusWithError(code, "startBleDeviceDiscovery");
                        }
                    }
                });
            }
        });
    }

    @ReactMethod
    public void stopScan(final Callback callback) {
        mQNBleApi.stopBleDeviceDiscovery(new QNResultCallback() {
            @Override
            public void onResult(int code, String msg) {
                Log.d("Yolanda stopScan c", String.valueOf(code));
                Log.d("Yolanda stopScan m", String.valueOf(msg));
                if (code == CheckStatus.OK.getCode()) {
                    callback.invoke("stopScan: ");
                }
            }
        });

    }

    @ReactMethod
    public void onStopDiscovery() {
        mQNBleApi.stopBleDeviceDiscovery(new QNResultCallback() {
            @Override
            public void onResult(int code, String msg) {

            }
        });

    }
}
