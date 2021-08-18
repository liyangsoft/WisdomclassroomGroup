package com.example.wisdomclassroomgroup.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.blankj.utilcode.util.ToastUtils;
import com.example.wisdomclassroomgroup.eventbus.EventCenter;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServer extends Service {
    private ServerSocket serverSocket;
    private Socket socket;

    @Override
    public void onCreate() {
        super.onCreate();
        startServer();
    }

    public void startServer() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(8888);
                    Log.i("tcp", "服务器等待连接中");
                    while (true) {
                        socket = serverSocket.accept();
                        Log.i("tcp", "客户端连接上来了");
                        InputStream inputStream = socket.getInputStream();
                        byte[] buffer = new byte[1024];
                        int len = -1;

                        while ((len = inputStream.read(buffer)) != -1) {
                            String data = new String(buffer, 0, len);
                            Log.i("tcp", "收到客户端的数据-----------------------------:" + data);

                            EventBus.getDefault().post(new EventCenter<>(1, data));
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();

                } finally {
                   /* try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    socket = null;
                    serverSocket = null;*/
                }
            }
        }).start();

    }

    public void sendTcpMessage(final String msg) {
        if (socket != null && socket.isConnected()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        socket.getOutputStream().write(msg.getBytes());
                        socket.getOutputStream().flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
