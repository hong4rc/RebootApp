package kiat.anhhong.reboot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;

import kiat.anhhong.reboot.butterknife.ButterKnife;


public class MainActivity extends Activity {

    private static String[] SHUTDOWN_BROADCAST() {
        return new String[]{
                // we announce the device is going down so apps that listen for
                // this broadcast can do whatever
                "am broadcast android.intent.action.ACTION_SHUTDOWN",
                // we tell the file system to write any data buffered in memory out to disk
                "sync",
                // we also instruct the kernel to drop clean caches, as well as
                // reclaimable slab objects like dentries and inodes
                "echo 3 > /proc/sys/vm/drop_caches",
                // and sync buffered data as before
                "sync",
        };
    }

    private static final String SHUTDOWN = "svc power shutdown";
    private static final String REBOOT_CMD = "svc power reboot";
    private static final String REBOOT_SOFT_REBOOT_CMD = "setprop ctl.restart zygote";
    private static final String REBOOT_RECOVERY_CMD = "reboot recovery";
    private static final String REBOOT_BOOTLOADER_CMD = "svc power reboot bootloader";
    private static final String[] REBOOT_SAFE_MODE
            = new String[]{"setprop persist.sys.safemode 1", REBOOT_SOFT_REBOOT_CMD};
    private static final String PLAY_STORE_MY_APPS
            = "https://facebook.com.halohong1997";

    private static final int RUNNABLE_DELAY_MS = 1000;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        HandlerThread mHandlerThread = new HandlerThread("BackgroundThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        AsyncTaskCompat.executeParallel(new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                return Shell.SU.available();
            }

            @Override
            protected void onPostExecute(Boolean rootNotAvailable) {

                if (!rootNotAvailable) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("Error");
                    dialog.setMessage(R.string.root_status_no);
                    dialog.setCancelable(false);
                    dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    });
                    dialog.show();
                }
            }
        });
    }

    private void runCmd(long timeout, final String... cmd) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Shell.SU.run(cmd);
                mHandler.removeCallbacks(this);
            }
        }, timeout);
    }

    public void onAboutClick(View view) {
        String url = PLAY_STORE_MY_APPS;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    public void onShutdownClick(View view) {
        runCmd(0, SHUTDOWN);
    }

    public void onRebootClick(View view) {
        runCmd(0, REBOOT_CMD);
    }

    public void onSoftRebootClick(View view) {
        runCmd(0, SHUTDOWN_BROADCAST());
        runCmd(RUNNABLE_DELAY_MS, REBOOT_SOFT_REBOOT_CMD);
    }

    public void onRebootRecoveryClick(View view) {
        runCmd(0, SHUTDOWN_BROADCAST());
        runCmd(RUNNABLE_DELAY_MS, REBOOT_RECOVERY_CMD);
    }

    public void onRebootBootloaderClick(View view) {
        runCmd(0, REBOOT_BOOTLOADER_CMD);
    }

    public void onRebootSafeModeClick(View view) {
        runCmd(0, SHUTDOWN_BROADCAST());
        runCmd(RUNNABLE_DELAY_MS, REBOOT_SAFE_MODE);
    }
}