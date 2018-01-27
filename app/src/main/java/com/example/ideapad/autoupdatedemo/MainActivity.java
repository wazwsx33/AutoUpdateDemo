package com.example.ideapad.autoupdatedemo;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    Information information = new Information();
    boolean getVersionFromServer = false;
    int versionCode = -1;
    private DownloadService.DownloadBinder downloadBinder;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadBinder = (DownloadService.DownloadBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    /**
     * 异步消息处理
     */
    private Handler handler  = new Handler(){
        public void handleMessage(Message message){
            switch (message.what){
                case 1:
                    versionCompare();//比对方法
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()){
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        }

        Intent intent = new Intent(this, DownloadService.class);
        startService(intent); //启动服务
        bindService(intent, connection, BIND_AUTO_CREATE); //绑定服务

        //向服务器获取版本信息和下载地址
        sendRequestWithOkHttp();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 ){
                    for (int result : grantResults){
                        if (result != PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this, "必须同意所有权限", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                }
                break;
            default:
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

    /**
     * 获取当前版本号
     * @return
     * @throws Exception
     */
    private int getVersionName() throws Exception{
        PackageManager manager = getPackageManager();
        PackageInfo info = manager.getPackageInfo(getPackageName(), 0);

        return info.versionCode;
    }

    /**
     * 获取服务器版本信息
     */
    private void sendRequestWithOkHttp(){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        HttpUtil.sendOkHttpRequest("http://172.20.156.74/VersionUpdate.php", new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {

                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                boolean jsonFlag = parseJSONWithGSON(response.body().string());
                                Message message = new Message();
                                if (jsonFlag)
                                    message.what = 1; //服务器信息成功解析成JSON对象
                                else
                                    message.what = 2;
                                handler.sendMessage(message);
                            }
                        });

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }).start();
    }

    /**
     * 将服务器信息解析成对象
     * @param jsonData
     */
    private boolean parseJSONWithGSON(String jsonData) {
        Gson gson = new Gson();
        information = gson.fromJson(jsonData, Information.class);
        getVersionFromServer = true;
        Log.d("MainActivity", "code is " + information.getCode());
        Log.d("MainActivity", "address is " + information.getAddress());
        return true;
    }

    /**
     * 版本号比对并作出相应反应
     */
    private void versionCompare(){
        try {
            versionCode = getVersionName();
            if (versionCode < information.getCode()){
                Log.d("Judgement", String.valueOf(true));
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("存在更新，是否下载")
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                downloadBinder.startDownload(information.getAddress());
                            }
                        })
                        .setNegativeButton("否", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                builder.show();
            }else
                Log.d("Judgement", String.valueOf(false));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
