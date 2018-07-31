package com.wh.weexupdate;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE = 100;
    //错误码
    protected static final int NET_ERROR = 1001;

    protected static final int NET_KEEP = 1002;

    private TextView       tv_version;
    private ProgressDialog progressDialog;

    Handler mHandler = new Handler(){
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case NET_ERROR:
                    Toast.makeText(getApplicationContext(), "网络错误！", Toast.LENGTH_SHORT).show();
                    enterHome();
                    break;
                case NET_KEEP:
                    Toast.makeText(getApplicationContext(), "服务器正在维护！", Toast.LENGTH_SHORT).show();
                    enterHome();
                    break;

                default:
                    break;
            }

        };
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_version = (TextView) findViewById(R.id.splash_tv_version);
        // 设置版本名
        initVersionName();
        //版本更新
        updateVersion();
    }

    private void updateVersion() {
        final String url = "http://192.168.39.50:8080/update.txt";
        //1.访问服务器，获取最新版本信息
        new Thread(new Runnable() {

            @Override
            public void run() {
                //01. 定义okhttp
                //              OkHttpClient okHttpClient_get = new OkHttpClient();
                //设置连接超时时间
                OkHttpClient okHttpClient_get = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).build();
                //02.请求体
                Request request = new Request.Builder()
                        .get()//get请求方式
                        .url(url)//网址
                        .build();

                //03.执行okhttp
                Response response = null;
                try {
                    try {
                        response = okHttpClient_get.newCall(request).execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    /**
                     * 打印数据,注意一次请求，一次响应，读取一次就没了，要注释掉
                     */
                    //                  System.out.println(response.body().string());
                    String json = response.body().string();
                    //001.解析
                    //{"remoteVesion":"2","desc":"1.添加了摇一摇的功能\n2.优化了用户的体验","apkUrl":"http://192.168.39.50:8080/safenet.apk"}
                    JSONObject jsonObject = new JSONObject(json);

                    int remoteVesion = jsonObject.getInt("remoteVesion");
                    String desc = jsonObject.getString("desc");
                    String url = jsonObject.getString("apkUrl");

                    final UpdateInfo updateInfo = new UpdateInfo();
                    updateInfo.remoteVesion = remoteVesion;
                    updateInfo.desc = desc;
                    updateInfo.apkUrl = url;

                    //2.判断最新版本与本地是否一致
                    int localVersion = PackageUtil.getVersionCode(getApplicationContext());
                    if (remoteVesion > localVersion) {
                        //3.弹出提示dialog
                        //不能在子线程中更新UI
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                showUpdateDialog(updateInfo);
                            }
                        });

                    }else{
                        //跳转到主页
                        enterHome();
                    }
                } catch (IOException e) {
                    Message message = mHandler.obtainMessage();
                    message.what = NET_ERROR;
                    message.sendToTarget();

                    e.printStackTrace();
                } catch (JSONException e) {
                    Message message = mHandler.obtainMessage();
                    message.what = NET_KEEP;
                    message.sendToTarget();

                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();

    }
    //进入主页
    protected void enterHome() {
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        }, 2000);

    }

    private void showUpdateDialog(final UpdateInfo info) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("版本更新提示");
        builder.setCancelable(false);//点击旁边不能消失
        builder.setMessage(info.desc);

        //4.是否升级
        builder.setPositiveButton("立刻升级", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                downApk(info);
            }
        });
        builder.setNegativeButton("稍后再说", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //回到主页
                enterHome();

            }
        });
        builder.show();
    }

    protected void downApk(UpdateInfo info) {
        //5.下载最新APK
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.show();

            new Thread(new DownTask(info.apkUrl)).start();
        }else{
            //回到主页
            Toast.makeText(this, "没有SD卡！", Toast.LENGTH_SHORT).show();
            enterHome();
        }

    }

    class DownTask implements Runnable{

        String mUrl;

        public DownTask(String apkUrl) {
            this.mUrl = apkUrl;
        }

        @Override
        public void run() {
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                Response response = HttpUtil.get(mUrl);
                is = response.body().byteStream();

                long contentLength = response.body().contentLength();
                //01.设置进度条最大值
                progressDialog.setMax((int) contentLength);
                //路径：mnt/sdcard/
                File file = new File(Environment.getExternalStorageDirectory(), "safe.apk");
                fos = new FileOutputStream(file);

                byte[] buffer = new byte[1024];

                int len = -1;

                int progress = 0;
                while((len = is.read(buffer)) != -1){
                    fos.write(buffer, 0, len);

                    //                  SystemClock.sleep(10);
                    progress += len;
                    //02.设置进度
                    progressDialog.setProgress(progress);
                }

                fos.flush();
                //03.进度条消失
                progressDialog.dismiss();
                //6.提示用户安装
                installApk(file);
            } catch (IOException e) {
                Message message = mHandler.obtainMessage();
                message.what = NET_ERROR;
                message.sendToTarget();
                e.printStackTrace();
            } finally{
                closeIo(fos);
                closeIo(is);
                //              closeIo2(fos,is);
            }
        }

    }

    public void closeIo(Closeable io){
        if (io != null) {
            try {
                io.close();
                io = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void installApk(File file) {
        //       <intent-filter>
        //         <action android:name="android.intent.action.VIEW" />
        //         <category android:name="android.intent.category.DEFAULT" />
        //         <data android:scheme="content" />
        //         <data android:scheme="file" />
        //         <data android:mimeType="application/vnd.android.package-archive" />
        //     </intent-filter>
        //7.安装APK
        Intent intent = new Intent();
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setAction("android.intent.action.VIEW");
        //获取文件的uri
        Uri data = Uri.fromFile(file);
        intent.setDataAndType(data, "application/vnd.android.package-archive");
        //      startActivity(intent);
        startActivityForResult(intent, REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            //系统默认的用户行为只有两种：yes no
            switch (resultCode) {
                case Activity.RESULT_OK:
                    //***被覆盖安装了，所以不会出日志
                    Log.d(TAG, "RESULT_OK");
                    break;
                case Activity.RESULT_CANCELED:
                    Log.d(TAG, "RESULT_CANCELED");
                    //进入到主页
                    enterHome();
                    break;

                default:
                    break;
            }

        }
    }

    public void closeIo2(Closeable... io){
        for (int i = 0; i < io.length; i++) {
            if(io[i] != null){
                try {
                    io[i].close();
                    io[i] = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void initVersionName() {

        // PackageManager pm = this.getPackageManager();
        // /**
        // * flags:标志位，0标志包的基本信息
        // */
        // try {
        // PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
        // String versionName = info.versionName;
        //
        // tv_version.setText(versionName);
        // } catch (NameNotFoundException e) {
        // e.printStackTrace();
        // }

        String versionName = PackageUtil.getVersionName(this);
        tv_version.setText(versionName);

    }
}
