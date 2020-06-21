package com.example.develop;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initRecyclerView();
    }

    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.activity_main_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        InnerAdapter adapter = new InnerAdapter(getDataList(), this, this);
        recyclerView.setAdapter(adapter);
    }

    private List<ActivityModel> getDataList() {
        List<ActivityModel> list = new ArrayList<>();
        list.add(new ActivityModel("懒加载", "LazyLoadActivity", "com.example.develop.lazy_load.LazyLoadActivity"));
        list.add(new ActivityModel("侧滑删除", "SlideActivity2", "com.example.develop.slide_slip.SlideActivity2"));
        return list;
    }

    private final static class InnerAdapter extends RecyclerView.Adapter<InnerAdapter.InnerHolder> {
        private final String TAG = InnerAdapter.class.getSimpleName();
        private List<ActivityModel> list;
        private Context context;
        private final WeakReference<MainActivity> weakReference;

        public InnerAdapter(List<ActivityModel> list, Context context, MainActivity activity) {
            this.list = list;
            this.context = context;
            weakReference = new WeakReference<>(activity);
        }


        @Override
        public InnerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new InnerHolder(LayoutInflater.from(context).inflate(R.layout.activity_item_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(InnerHolder holder, final int position) {
            holder.textView1.setText(list.get(position).getTitle());
            holder.textView2.setText(list.get(position).getContent());
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String msg = "onClick: ----------------------item=" + list.get(position).getContent();
//                    LogcatHelper2.getInstance().input2File(context.getApplicationContext(), msg);
                    try {
                            Intent intent = new Intent(context, Class.forName(list.get(position).getPackageName()));
                            context.startActivity(intent);
                    } catch (ClassNotFoundException e) {
                        Log.e(TAG, "onClick: ", e);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return list == null ? 0 : list.size();
        }

        private final static class InnerHolder extends RecyclerView.ViewHolder {

            private TextView textView1;
            private TextView textView2;

            public InnerHolder(View itemView) {
                super(itemView);
                textView1 = itemView.findViewById(R.id.activity_item_layout_title);
                textView2 = itemView.findViewById(R.id.activity_item_layout_content);
            }
        }
    }


    private final static class ActivityModel {
        private String title;
        private String content;
        private String packageName;

        public ActivityModel(String title, String content, String packageName) {
            this.title = title;
            this.content = content;
            this.packageName = packageName;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }
    }
}
