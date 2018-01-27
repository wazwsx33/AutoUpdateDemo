package com.example.ideapad.autoupdatedemo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;

public class DownloadService extends Service {
    private DownLoadTask downLoadTask;
    private String downloadUrl;
    private DownloadListener listener = new DownloadListener(){

        @Override
        public void onProgress(int progress) {
            getNotificationManager().notify(1, getNotification("下载中", progress));
        }

        @Override
        public void onSuccess(String path) {
            downLoadTask = null;
            //下载成功时关闭通知，并通知下载成功
            stopForeground(true);
            getNotificationManager().notify(1, getNotification("下载成功", -1));
            Toast.makeText(DownloadService.this, "下载成功", Toast.LENGTH_SHORT).show();

            //安装APK
            installAPK(DownloadService.this, path);

        }

        @Override
        public void onFailed() {
            downLoadTask = null;
            //下载成功时关闭通知，并通知下载失败
            stopForeground(true);
            getNotificationManager().notify(1, getNotification("下载失败", -1));
            Toast.makeText(DownloadService.this, "下载失败", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPaused() {
            downLoadTask = null;
            Toast.makeText(DownloadService.this, "下载暂停", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCanceled() {
            downLoadTask = null;
            stopForeground(true);
            Toast.makeText(DownloadService.this, "下载取消", Toast.LENGTH_SHORT).show();
        }
    };

    public DownloadService() {
    }

    private  DownloadBinder mBinder = new DownloadBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    class DownloadBinder extends Binder {
        public void startDownload(String url){
            if(downLoadTask == null){
                downloadUrl = url;
                downLoadTask = new DownLoadTask(listener);
                downLoadTask.execute(downloadUrl);
                startForeground(1, getNotification("下载中", 0));
                Toast.makeText(DownloadService.this, "下载中", Toast.LENGTH_SHORT).show();

            }
        }

        public void pauseDownload(){
            if (downLoadTask != null)
                downLoadTask.pauseDownload();
        }

        public void cancelDownload(){
            if (downLoadTask != null){
                downLoadTask.cancelDownload();
            }else {
                if (downloadUrl != null){
                    //删除文件并关闭通知
                    String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory ;

                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                        //如果手机有sd卡，路径为sd卡download文件夹
                        directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    else
                        //没有sd卡则路径为手机目录的download文件夹
                        directory = Environment.getRootDirectory().getPath();

                    File file = new File(directory + fileName);
                    if (file.exists())
                        file.delete();
                    getNotificationManager().cancel(1);
                    stopForeground(true);
                    Toast.makeText(DownloadService.this, "已取消", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * 获取通知管理器
     * @return
     */
    private NotificationManager getNotificationManager(){
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    /**]
     * 设置通知
     * @param title
     * @param progress
     * @return
     */
    private Notification getNotification(String title, int progress){
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(title);
        if (progress > 0){
            builder.setContentText(progress + "%");
            builder.setProgress(100, progress, false);
        }

        return builder.build();
    }

    /**
     * 安装APK
     * @param context
     * @param path
     */
    private void installAPK(Context context,String path) {
        File file = new File(path);
        if(file.exists()){
            openFile(file,context);
        }else{
            Toast.makeText(context,"安装失败",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 打开文件
     * @param var0
     * @param var1
     */
    public void openFile(File var0, Context var1) {
        Intent var2 = new Intent();
        var2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        var2.setAction(Intent.ACTION_VIEW);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            Uri uriForFile = FileProvider.getUriForFile(var1, var1.getApplicationContext().getPackageName() + ".provider", var0);
            var2.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            var2.setDataAndType(uriForFile, var1.getContentResolver().getType(uriForFile));
        }else{
            var2.setDataAndType(Uri.fromFile(var0), getMIMEType(var0));
        }
        try {
            var1.startActivity(var2);
        } catch (Exception var5) {
            var5.printStackTrace();
            Toast.makeText(var1, "没有找到打开此类文件的程序", Toast.LENGTH_SHORT).show();
        }
    }
    public String getMIMEType(File var0) {
        String var1 = "";
        String var2 = var0.getName();
        String var3 = var2.substring(var2.lastIndexOf(".") + 1, var2.length()).toLowerCase();
        var1 = MimeTypeMap.getSingleton().getMimeTypeFromExtension(var3);
        return var1;
    }

}
