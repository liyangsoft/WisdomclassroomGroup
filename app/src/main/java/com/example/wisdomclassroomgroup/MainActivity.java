package com.example.wisdomclassroomgroup;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.BusUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.example.wisdomclassroomgroup.entity.HandShakeBean;
import com.example.wisdomclassroomgroup.eventbus.EventCenter;
import com.example.wisdomclassroomgroup.service.MediaReaderService;
import com.example.wisdomclassroomgroup.service.TcpServer;
import com.xuhao.didi.core.iocore.interfaces.IPulseSendable;
import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.core.pojo.OriginalData;
import com.xuhao.didi.socket.client.impl.client.action.ActionDispatcher;
import com.xuhao.didi.socket.client.sdk.OkSocket;
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter;
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager;
import com.xuhao.didi.socket.client.sdk.client.connection.NoneReconnect;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClient;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClientPool;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IServerManager;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IServerShutdown;
import com.xuhao.didi.socket.server.action.ServerActionAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final int RC_SCREEN_PERM = 124;
    private int REQUEST_MEDIA_PROJECTION = 18;
    private MediaProjectionManager mediaProjectionManager;
    private MyApplication myApplication;
    private IServerManager mServerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        myApplication = (MyApplication) getApplication();
        checPermiss();
        initSocket();

    }



    @Subscribe(threadMode = ThreadMode.MAIN)
    public void EventMess(EventCenter center) {

        if (!AppUtils.isAppForeground()){
            ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);

            /**获得当前运行的task(任务)*/
            List<ActivityManager.RunningTaskInfo> taskInfoList = activityManager.getRunningTasks(100);
            for (ActivityManager.RunningTaskInfo taskInfo : taskInfoList) {
                /**找到本应用的 task，并将它切换到前台*/
                if (taskInfo.topActivity.getPackageName().equals(this.getPackageName())) {
                    activityManager.moveTaskToFront(taskInfo.id, 0);
                    break;
                }
            }
        }


        String data = (String) center.getData();
        //data 1.开启   0.不开
        if (data.equals("1") && !ActivityUtils.isActivityExistsInStack(WatchContect.class)) {

            Intent intent = new Intent(this, WatchContect.class);
            Bundle bundle = new Bundle();
//            bundle.putString("Address", "172.31.98.211");
            bundle.putString("Address", "192.168.3.220");
            intent.putExtras(bundle);
            startActivity(intent);
        }

    }

    private void initSocket() {
        startService(new Intent(getApplicationContext(), TcpServer.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    //获取屏幕权限
    @AfterPermissionGranted(RC_SCREEN_PERM)
    private void checPermiss() {
        String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {
            requestCapturePermission();
        } else {
            EasyPermissions.requestPermissions(this, "需要访问存储权限",
                    RC_SCREEN_PERM, perms);
        }
    }

    private void requestCapturePermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            //5.0 之后才允许使用屏幕截图
            mediaProjectionManager = (MediaProjectionManager) this.getSystemService(
                    Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),
                    REQUEST_MEDIA_PROJECTION);
        } else {
            Toast.makeText(this, "系统版本低于5.0!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
                if (mediaProjection == null) {

                    return;
                }

                myApplication.mediaProjection = mediaProjection;
                start();
                MediaReaderService.ServerStatus serverStatus = myApplication.getServerStatus();
                serverStatus = MediaReaderService.ServerStatus.STARTED;
            }
        }
    }

    /**
     * 开启
     */
    private void start() {
        Intent intent = new Intent(this, MediaReaderService.class);
        intent.putExtra("CMD", 1);
        startService(intent);
    }


    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            ToastUtils.showLong("没有相关权限，打开应用程序设置界面修改应用程序权限");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


}