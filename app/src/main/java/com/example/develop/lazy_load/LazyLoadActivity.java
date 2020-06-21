package com.example.develop.lazy_load;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.example.develop.R;

public class LazyLoadActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lazy_load);
        initView();
    }

    private void initView() {
        ViewPager viewPager = findViewById(R.id.activity_lazy_load_vp);
        viewPager.setAdapter(new LazyAdapter(getSupportFragmentManager()));
    }


    static class LazyAdapter extends FragmentPagerAdapter {

        public LazyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment;
            switch (position) {
                case 0:
                    fragment = new LazyLoadFragmentIml();
                    break;
                case 1:
                    fragment = new LazyLoadFragmentIml();
                    break;
                case 2:
                    fragment = new LazyLoadFragmentIml();
                    break;
                case 3:
                    fragment = new LazyLoadFragmentIml();
                    break;
                case 4:
                    fragment = new LazyLoadFragmentIml();
                    break;
                case 5:
                    fragment = new LazyLoadFragmentIml();
                    break;
                default:
                    fragment = null;
            }
            if (fragment != null) {
                Bundle bundle = new Bundle();
                bundle.putInt("pageIndex", position + 1);
                fragment.setArguments(bundle);
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return 6;
        }
    }
}
