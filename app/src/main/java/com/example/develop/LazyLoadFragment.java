package com.example.develop;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class LazyLoadFragment extends Fragment {

    /**
     * 初始化数据必需是可见状态、视图构建完毕，数据自动加载过一次后就不进行再次自动加载（被动加载允许）视图销毁后再次创建的话才重新绑定数据
     * 该实现需要三个值判断：视图构建完毕否、页面可见否、数据加载过否、
     */
    private boolean isVisibleToUser = false;//视图是否可见
    private boolean viewRebuild = true;//页面是否为重建
    private boolean alreadyLoaded = false;//是否已经加载过数据

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        this.isVisibleToUser = isVisibleToUser;
        initData();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getContentView(), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewRebuild = true;
        findView(view);
        initListener();
        initData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void initData() {
        if (isVisibleToUser && viewRebuild && !alreadyLoaded) {
            alreadyLoaded = true;
            viewRebuild = false;
            firstLoad();
            return;
        }
        if (isVisibleToUser && viewRebuild) {
            viewRebuild = false;
            rebindData();
        }
    }

    protected abstract int getContentView();//添加布局文件

    protected abstract void findView(View container);//初始化view

    protected abstract void initListener();//设置监听

    protected abstract void firstLoad();//第一次加载数据

    protected abstract void rebindData();//重新绑定数据
}
