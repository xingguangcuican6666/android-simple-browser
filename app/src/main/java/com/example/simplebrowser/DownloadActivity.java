package com.example.simplebrowser;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DownloadActivity extends AppCompatActivity {
    private long downloadId = -1L;
    private TextView statusView;
    private Handler handler;
    private Runnable poll;
    private BroadcastReceiver completeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        downloadId = getIntent().getLongExtra("downloadId", -1L);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (getResources().getDisplayMetrics().density * 16);
        root.setPadding(pad, pad, pad, pad);
        statusView = new TextView(this);
        statusView.setText(downloadId > 0 ? "正在下载..." : "无下载任务");
        statusView.setTextSize(16);
        root.addView(statusView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);

        handler = new Handler(Looper.getMainLooper());
        poll = new Runnable() {
            @Override public void run() {
                updateProgress();
                handler.postDelayed(this, 1000);
            }
        };
        if (downloadId > 0) handler.post(poll);

        completeReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    updateProgress();
                    statusView.setText("下载完成，已保存到 Downloads");
                    handler.removeCallbacks(poll);
                    handler.postDelayed(() -> finish(), 1500);
                }
            }
        };
        registerReceiver(completeReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void updateProgress() {
        try {
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Query q = new DownloadManager.Query();
            q.setFilterById(downloadId);
            Cursor c = dm.query(q);
            if (c != null) {
                if (c.moveToFirst()) {
                    int titleIdx = c.getColumnIndex(DownloadManager.COLUMN_TITLE);
                    int totalIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                    int sofarIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    int statIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    String title = titleIdx >= 0 ? c.getString(titleIdx) : "";
                    long total = totalIdx >= 0 ? c.getLong(totalIdx) : -1;
                    long sofar = sofarIdx >= 0 ? c.getLong(sofarIdx) : -1;
                    int st = statIdx >= 0 ? c.getInt(statIdx) : 0;
                    String stStr = (st == DownloadManager.STATUS_RUNNING ? "下载中" : st == DownloadManager.STATUS_SUCCESSFUL ? "已完成" : st == DownloadManager.STATUS_FAILED ? "失败" : "等待");
                    String prog = (total > 0 && sofar >= 0) ? (int)(sofar * 100f / total) + "%" : "-";
                    statusView.setText((title.isEmpty()?"下载":title) + " - " + stStr + " (" + prog + ")");
                }
                c.close();
            }
        } catch (Throwable ignored) { }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (completeReceiver != null) try { unregisterReceiver(completeReceiver); } catch (Throwable ignored) {}
        if (handler != null && poll != null) handler.removeCallbacks(poll);
    }
}
