package org.nestordeveloper.controlparrotdrone;

// Mirar codigo http://developer.parrot.com/docs/SDK3/?java#discover-the-drones
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.parrot.arsdk.ARSDK;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARControllerException;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener;
import com.parrot.arsdk.arcontroller.ARDeviceControllerStreamListener;
import com.parrot.arsdk.arcontroller.ARFeatureCommon;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceNetService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryException;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;

import java.util.List;

public class ControlParrotDrone extends AppCompatActivity implements ARDiscoveryServicesDevicesListUpdatedReceiverDelegate, ARDeviceControllerListener, ARDeviceControllerStreamListener {

    private static final String TAG = "Info ";
    // Descubriendo drones conectados
    private ARDiscoveryService mArdiscoveryService;
    private ServiceConnection mArdiscoveryServiceConnection;
    private ARDiscoveryServicesDevicesListUpdatedReceiver mArdiscoveryServicesDevicesListUpdatedReceiver;
    private ARDeviceController deviceController;
    private ARDiscoveryDevice device;
    private List<ARDiscoveryDeviceService> deviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_parrot_drone);

        ARSDK.loadSDKLibs();
        initDiscoveryService();
        startDiscovery();
        registerReceivers();

        if(deviceList!=null){
            ARDiscoveryDeviceService service = deviceList.get(0); //Comprobar si el elemento 0 tiene algo

            if(service!=null) {
                device = createDiscoveryDevice(service); // mirar que device tiene algo
                if (device != null) {
                    try {
                        deviceController = new ARDeviceController(device);
                    } catch (ARControllerException e) {
                        e.printStackTrace();
                    }

                    // your class should implement ARDeviceControllerListener
                    // Listen to the commands received from the drone (example of the battery level received)
                    // Listen to the states changes:
                    deviceController.addListener(this);

                    // Listen to the video stream received from the drone
                    // your class should implement ARDeviceControllerStreamListener
                    deviceController.addStreamListener(this);

            /*
                Finally, starts the device controller (after that call, the callback you set in
                ARCONTROLLER_Device_AddStateChangedCallback should be called).
            */
                    ARCONTROLLER_ERROR_ENUM error = deviceController.start();

                    // Cleanup when done:
                    // ARCONTROLLER_ERROR_ENUM error = deviceController.stop();

                    // VOY POR TAKE OFF. LANDING... http://developer.parrot.com/docs/SDK3/?java#taking-off

                }
            }
        }

    }

    // Descubriendo drones conectados
    private void initDiscoveryService()
    {
        // create the service connection
        if (mArdiscoveryServiceConnection == null)
        {
            mArdiscoveryServiceConnection = new ServiceConnection()
            {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service)
                {
                    mArdiscoveryService = ((ARDiscoveryService.LocalBinder) service).getService();

                    startDiscovery();
                }

                @Override
                public void onServiceDisconnected(ComponentName name)
                {
                    mArdiscoveryService = null;
                }
            };
        }

        if (mArdiscoveryService == null)
        {
            // if the discovery service doesn't exists, bind to it
            Intent i = new Intent(getApplicationContext(), ARDiscoveryService.class);
            getApplicationContext().bindService(i, mArdiscoveryServiceConnection, Context.BIND_AUTO_CREATE);
        }
        else
        {
            // if the discovery service already exists, start discovery
            startDiscovery();
        }
    }

    private void startDiscovery()
    {
        if (mArdiscoveryService != null)
        {
            mArdiscoveryService.start();
        }
    }

    // your class should implement ARDiscoveryServicesDevicesListUpdatedReceiverDelegate
    private void registerReceivers()
    {
        mArdiscoveryServicesDevicesListUpdatedReceiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(this);
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.registerReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver, new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));
    }

    @Override
    public void onServicesDevicesListUpdated()
    {
        Log.d(TAG, "onServicesDevicesListUpdated ...");

        if (mArdiscoveryService != null)
        {
            deviceList = mArdiscoveryService.getDeviceServicesArray();

            // Do what you want with the device list
            for(ARDiscoveryDeviceService aux:deviceList)
                Log.i("DEVICES",aux.toString());
        }
    }

    private ARDiscoveryDevice createDiscoveryDevice(ARDiscoveryDeviceService service)
    {
        ARDiscoveryDevice device = null;
        if ((service != null) &&
                (ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_ARDRONE.equals(ARDiscoveryService.getProductFromProductID(service.getProductID()))))
        {
            try
            {
                device = new ARDiscoveryDevice();

                ARDiscoveryDeviceNetService netDeviceService = (ARDiscoveryDeviceNetService) service.getDevice();

                device.initWifi(ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_ARDRONE, netDeviceService.getName(), netDeviceService.getIp(), netDeviceService.getPort());
            }
            catch (ARDiscoveryException e)
            {
                e.printStackTrace();
                Log.e(TAG, "Error: " + e.getError());
            }
        }

        return device;
    }

    private void unregisterReceivers()
    {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());

        localBroadcastMgr.unregisterReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver);
    }

    private void closeServices()
    {
        Log.d(TAG, "closeServices ...");

        if (mArdiscoveryService != null)
        {
            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    mArdiscoveryService.stop();

                    getApplicationContext().unbindService(mArdiscoveryServiceConnection);
                    mArdiscoveryService = null;
                }
            }).start();
        }
    }

    @Override
    // called when the state of the device controller has changed
    public void onStateChanged (ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARCONTROLLER_ERROR_ENUM error)
    {
        switch (newState)
        {
            case ARCONTROLLER_DEVICE_STATE_RUNNING:
                break;
            case ARCONTROLLER_DEVICE_STATE_STOPPED:
                break;
            case ARCONTROLLER_DEVICE_STATE_STARTING:
                break;
            case ARCONTROLLER_DEVICE_STATE_STOPPING:
                break;

            default:
                break;
        }
    }

    @Override
    public void onExtensionStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARDISCOVERY_PRODUCT_ENUM product, String name, ARCONTROLLER_ERROR_ENUM error) {

    }

    @Override
    // called when a command has been received from the drone
    public void onCommandReceived(ARDeviceController deviceController, ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey, ARControllerDictionary elementDictionary)
    {
        if (elementDictionary != null)
        {
            // if the command received is a battery state changed
            if (commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED)
            {
                ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);

                if (args != null)
                {
                    Integer batValue = (Integer) args.get(ARFeatureCommon.ARCONTROLLER_DICTIONARY_KEY_COMMON_COMMONSTATE_BATTERYSTATECHANGED_PERCENT);

                    // do what you want with the battery level
                }
            }
        }
        else
        {
            Log.e(TAG, "elementDictionary is null");
        }
    }

    @Override
    public ARCONTROLLER_ERROR_ENUM configureDecoder(ARDeviceController deviceController, final ARControllerCodec codec) {
        // configure your decoder
        // return ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK if display went well
        // otherwise, return ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_ERROR. In that case,
        // configDecoderCallback will be called again
        return null;
    }

    @Override
    public ARCONTROLLER_ERROR_ENUM onFrameReceived(ARDeviceController deviceController, final ARFrame frame) {
        // display the frame
        // return ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_OK if display went well
        // otherwise, return ARCONTROLLER_ERROR_ENUM.ARCONTROLLER_ERROR. In that case,
        // configDecoderCallback will be called again
        return null;
    }

    @Override
    public void onFrameTimeout(ARDeviceController deviceController) {

    }
}
