package com.example.ideapad.autoupdatedemo;

/**
 * Created by IdeaPad on 2018/1/25.
 */

interface DownloadListener {
    void onProgress(int progress);
    void onSuccess(String path);
    void onFailed();
    void onPaused();
    void onCanceled();
}
