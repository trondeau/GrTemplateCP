package org.gnuradio.grtemplatecp;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;

import org.gnuradio.controlport.BaseTypes;
import org.gnuradio.grcontrolport.RPCConnection;
import org.gnuradio.grcontrolport.RPCConnectionThrift;

import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static class SetKnobsHandler extends Handler {
        private RPCConnection mConnection;
        public SetKnobsHandler(RPCConnection conn) {
            super();
            mConnection = conn;
        }

        public void handleMessage(Message m) {
            Bundle b = m.getData();
            if(b != null) {
                HashMap<String, RPCConnection.KnobInfo> k =
                        (HashMap<String, RPCConnection.KnobInfo>) b.getSerializable("Knobs");

                Log.d("MainActivity", "Set Knobs: " + k);
                if((k != null) && (!k.isEmpty())) {
                    mConnection.setKnobs(k);
                }
            }
        }
    }

    public class RunNetworkThread implements Runnable {

        private RPCConnection mConnection;
        private String mHost;
        private Integer mPort;
        private Boolean mConnected;
        private Handler mHandler;

        RunNetworkThread(String host, Integer port) {
            this.mHost = host;
            this.mPort = port;
            this.mConnected = false;
        }

        public void run() {
            if(!mConnected) {
                mConnection = new RPCConnectionThrift(mHost, mPort);
                mConnected = true;
            }

            Looper.prepare();
            mHandler = new SetKnobsHandler(mConnection);
            Looper.loop();
        }

        public RPCConnection getConnection() {
            if(mConnection == null) {
                throw new IllegalStateException("connection not established");
            }
            return mConnection;
        }

        public Handler getHandler() {
            return mHandler;
        }
    }

    private void postSetKnobMessage(HashMap<String, RPCConnection.KnobInfo> knobs) {
        Handler h = mControlPortThread.getHandler();
        Bundle b = new Bundle();
        b.putSerializable("Knobs", knobs);
        Message m = h.obtainMessage();
        m.setData(b);
        h.sendMessage(m);
    }

    public RunNetworkThread mControlPortThread;
    private SeekBar mAmpSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        SetTMP(getCacheDir().getAbsolutePath());
        FgInit();
        FgStart();

        // Make the ControlPort connection in the network thread
        mControlPortThread = new RunNetworkThread("localhost", 65001);
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(mControlPortThread);

        // Wait here until we have confirmation that the connection succeeded.
        while(true) {
            try {
                mControlPortThread.getConnection();
                break;
            } catch (IllegalStateException e0) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Initialize the knob to a value of 0.5
        final String mult_knob_name = "multiply_const_ff0::coefficient";
        RPCConnection.KnobInfo k =
                new RPCConnection.KnobInfo(mult_knob_name, 0.5,
                        BaseTypes.DOUBLE);

        HashMap<String, RPCConnection.KnobInfo> map = new HashMap<>();
        map.put(mult_knob_name, k);
        postSetKnobMessage(map);

        // Create the seekBar to move update the amplitude
        mAmpSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mAmpSeekBar.setMax(100);      // max value -> 1.0
        mAmpSeekBar.setProgress(50);  // match 0.5 starting value
        mAmpSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Double amp = progress / 100.0; // rescale by 100
                RPCConnection.KnobInfo _k =
                        new RPCConnection.KnobInfo(mult_knob_name, amp,
                                BaseTypes.DOUBLE);
                HashMap<String, RPCConnection.KnobInfo> _map = new HashMap<>();
                _map.put(mult_knob_name, _k);
                postSetKnobMessage(_map);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    public native void SetTMP(String tmpname);
    public native void FgInit();
    public native void FgStart();

    static {
        System.loadLibrary("fg");
    }
}
