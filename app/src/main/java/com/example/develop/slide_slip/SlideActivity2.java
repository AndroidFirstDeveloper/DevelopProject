package com.example.develop.slide_slip;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.develop.R;

public class SlideActivity2 extends Activity {
    private TouchRecyclerView recyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide2);
        initView();
    }

    private void initView() {
        recyclerView = findViewById(R.id.activity_slide_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(new SlideAdapter(this));
    }

    static class SlideAdapter extends RecyclerView.Adapter<SlideAdapter.SlideHolder> {
        private Context context;

        public SlideAdapter(Context context) {
            this.context = context;
        }


        @Override
        public SlideHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new SlideAdapter.SlideHolder(LayoutInflater.from(context).inflate(R.layout.slide_item_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(SlideHolder holder, final int position) {
            holder.slide_item_layout_content.setText("this is item" + position);
            holder.slide_item_layout_delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "删除" + position, Toast.LENGTH_SHORT).show();
                }
            });
            holder.slide_item_layout_top.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "置顶" + position, Toast.LENGTH_SHORT).show();
                }
            });
            holder.slide_item_layout_unread.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "标记为未读" + position, Toast.LENGTH_SHORT).show();
                }
            });
            holder.slide_item_layout_item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "item" + position, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return 30;
        }

        static class SlideHolder extends RecyclerView.ViewHolder {
            private LinearLayout slide_item_layout_item;
            private ImageView slide_item_layout_head;
            private TextView slide_item_layout_content;
            private TextView slide_item_layout_delete;
            private TextView slide_item_layout_unread;
            private TextView slide_item_layout_top;

            public SlideHolder(View itemView) {
                super(itemView);
                slide_item_layout_item = itemView.findViewById(R.id.slide_item_layout_item);
                slide_item_layout_head = itemView.findViewById(R.id.slide_item_layout_head);
                slide_item_layout_content = itemView.findViewById(R.id.slide_item_layout_content);
                slide_item_layout_delete = itemView.findViewById(R.id.slide_item_layout_delete);
                slide_item_layout_unread = itemView.findViewById(R.id.slide_item_layout_unread);
                slide_item_layout_top = itemView.findViewById(R.id.slide_item_layout_top);
            }
        }
    }
}
