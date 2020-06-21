package com.example.develop.lazy_load;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.develop.R;

import java.lang.ref.WeakReference;

public class LazyLoadFragmentIml extends LazyLoadFragment {
    private final String TAG = LazyLoadFragmentIml.this.getClass().getSimpleName();

    private TextView fragment_lazyload_layout_tv1;
    private TextView fragment_lazyload_layout_tv;
    private MyHandler handler = new MyHandler(this);

    private int pageIndex;//缓存数据，只是视图被销毁的情况下这些变量保存的值是不变的
    private int counter;//缓存数据

    @Override
    protected int getContentView() {//设置布局文件
        return R.layout.fragment_lazyload_layout;
    }

    @Override
    protected void findView(View container) {//初始化View(绑定数据的逻辑最好不要放在这里面)
        fragment_lazyload_layout_tv1 = container.findViewById(R.id.fragment_lazyload_layout_tv1);
        fragment_lazyload_layout_tv = container.findViewById(R.id.fragment_lazyload_layout_tv);
    }

    @Override
    protected void initListener() {
    }

    @Override
    protected void firstLoad() {//首次加载数据
        Bundle bundle = getArguments();
        if (bundle != null) {
            pageIndex = bundle.getInt("pageIndex", -1);
        }
        Log.e(TAG, "firstLoad: pageIndex=" + pageIndex);
        new Thread(new Runnable() {//模拟网络加载数据
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    handler.sendEmptyMessage(0);//数据加载成功后更新View
                } catch (Exception e) {
                    Log.e(TAG, "run: ", e);//数据加载失败 关于数据加载失败后如何处理，这交给用户自己来做
                }
            }
        }).start();
    }

    @Override
    protected void rebindData() {//重新绑定数据，视图销毁并重建后会调用该方法
        Log.e(TAG, "rebindData: pageIndex=" + pageIndex);
        filling(0);
    }

    private void filling(int value) {//给视图view填充数据
        counter += value;
        fragment_lazyload_layout_tv1.setText(String.valueOf(counter));//加载次数
        fragment_lazyload_layout_tv.setText(String.valueOf(pageIndex));//当前页面序号
    }

    static class MyHandler extends Handler {

        WeakReference<LazyLoadFragmentIml> weakReference;

        MyHandler(LazyLoadFragmentIml fragment) {
            weakReference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    if (weakReference != null && weakReference.get() != null) {
                        weakReference.get().filling(1);
                    }
                    break;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
