package com.example.ideapad.autoupdatedemo;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.ProgressBar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by IdeaPad on 2018/1/25.
 */

public class DownLoadTask extends AsyncTask<String, Integer, Integer> {
    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;

    private DownloadListener listener;
    private boolean isCanceled = false;
    private boolean isPaused = false;
    private int lastProgress;

    private String directory;
    private String fileName;

    public DownLoadTask(DownloadListener listener){
        this.listener = listener;
    }

    @Override
    protected Integer doInBackground(String... params) {
        InputStream is = null;
        RandomAccessFile savedFile = null;
        File file = null;

        try{
            int progress = 0; //进度
            long downloadedLength = 0; //记录已下载长度
            String downloadUrl = params[0];
            fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));

            directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file = new File(directory + fileName);
            if (file.exists())
                file.delete();

            //获取待下载文件长度
            long contentLength = getContentLength(downloadUrl);

            if (contentLength == 0)
                return TYPE_FAILED;
/*            else if (contentLength == downloadedLength)
                //下载完成
                return TYPE_SUCCESS;*/

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
/*                    //断点下载，指定从哪个字节开始下载
                    .addHeader("RANGE", "byte=" + downloadedLength + "-")*/
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if (response != null){
                is = response.body().byteStream();
                savedFile = new RandomAccessFile(file, "rw");
                //savedFile.seek(downloadedLength); //跳过已下载字节
                byte[] b = new byte[1024];
                int total = 0;
                int len;

                while ((len = is.read(b)) != -1){
                    if (isCanceled)
                        return TYPE_CANCELED;
                    else if (isPaused)
                        return TYPE_PAUSED;
                    else {
                        total += len;
                        savedFile.write(b, 0, len);
                        //计算下载百分比
                        progress = (int) ((total + downloadedLength) * 100 / contentLength);
                        publishProgress(progress);
                    }
                }
            }

            response.body().close();
            return TYPE_SUCCESS;

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if (is != null)
                    is.close();
                if (savedFile != null)
                    savedFile.close();
                if (isCanceled && file != null)
                    file.delete();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if (progress > lastProgress){
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }

    @Override
    protected void onPostExecute(Integer status) {
        switch (status){
            case TYPE_SUCCESS:
                listener.onSuccess(directory  + fileName);
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
                break;
            default:
                break;
        }
    }

    public void pauseDownload(){
        isPaused = true;
    }

    public void cancelDownload(){
        isCanceled = true;
    }

    /**
     * 计算待下载文件长度
     * @param downloadUrl
     * @return
     * @throws IOException
     */
    private long getContentLength(String downloadUrl) throws IOException{
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()){
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }
}
