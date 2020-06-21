package com.example.develop.slide_slip;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.Scroller;

/**
 * CZL20200610
 * 自定义View模仿qq侧滑删除
 */
public class SlideSlipView extends FrameLayout {

    /**
     * 为了更好地分析代码中的逻辑，下面一些概念在此声明：
     * 折叠状态：置顶、标记为未读、删除这部分隐藏起来的时候
     * 非折叠状态：置顶、标记为未读、删除这部分显示出来的时候
     * 原始内容：折叠状态下item可见内容部分
     * 隐藏内容：置顶、标记为未读、删除这部分构成的内容部分
     */

    private final String TAG = SlideSlipView.this.getClass().getSimpleName();
    private Scroller scroller;//辅助滚动工具
    private int mTouchSlop;
    private final float VELOCITY_SLOP = 600;//惯性滑动最小速度值
    private float mLastX;//记录上次触摸点x坐标
    private float mLastY;//记录上次触摸点y坐标
    //特殊触摸标记，含义：当前被点击的item之外是否还有其它item是展开的，true：是   false：否
    // 当为true的时候会进行‘拦截一切’的举动，也就是对事件只拦截但是什么滑动都不处理(这参考了qq的实现方式)
    private boolean specialTouch = false;
    //特殊触摸标记，含义：是否需要将当前view变成‘折叠状态’,true:是   false:否
    //当前view为'非折叠状态'时，手指点击‘原始内容’区域并立即抬起后将自动折叠view
    private boolean specialTouch2 = false;
    private boolean fold = true;//折叠标记,判断当前view是否是折叠状态，true:是  false：否
    private VelocityTracker velocityTracker;//速度模拟类
    private RecyclerView recyclerView;//如果在RV中使用当前view，需要处理-滑动冲突、多个item同时展开的问题，这会用到RV

    public SlideSlipView(Context context) {
        this(context, (AttributeSet) null);
    }

    public SlideSlipView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideSlipView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        scroller = new Scroller(context);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        setClipToPadding(false);//设置该属性后，该view设置的padding部分可以随内容一起滑动
    }

    /**
     * 重写布局方法，支持子view设置margin、padding等属性
     * 子view必须设置tag属性，否者直接抛出异常
     * 可见子view设置的tag取值范围0-100；隐藏子view设置tag取值范围100-Integer最大值。
     * 这里的子view是指直接子view，可见子view是指正常情况下可以看见的子view，隐藏子view是指正常情况下不可见的子view，当滑动以后才能看到的view
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int childCount = getChildCount();
        final int mPaddingLeft = Math.max(0, getPaddingLeft());
        final int mPaddingRight = Math.max(0, getPaddingRight());
        final int mPaddingTop = Math.max(0, getPaddingTop());
        int widthSum = mPaddingLeft;//可见内容部分初始左边界位置
        int widthSum2 = getMeasuredWidth();//隐藏内容部分初始左边界位置
        int heightSum = 0;
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            int tag;
            try {
                tag = Integer.valueOf((String) childView.getTag());//这里设计通过tag区分可见子view跟隐藏子view，所以使用该控件必需设置子view的tag
            } catch (Exception e) {
                throw new IllegalArgumentException("SlideSlipView的子View需要设置tag，显示View的tag 0-100，隐藏View的tag 101-Integer.MAX_VALUE");
            }

            heightSum = mPaddingTop + getMargin(2, childView);
            if (tag <= 100 && tag >= 0) {//可见子view,tag 0-100
                widthSum += getMargin(0, childView);//获取childView左边界的mleft位置
                if (widthSum >= (getMeasuredWidth() - mPaddingRight)) {//当可见子View累计宽度大于可用宽度时，不再显示剩余‘可见子view’
                    continue;
                }
                int rightPosition = Math.min(getMeasuredWidth() - mPaddingRight, widthSum + childView.getMeasuredWidth());//当子view的右边界大于
                childView.layout(widthSum, heightSum, rightPosition, heightSum + childView.getMeasuredHeight());
                widthSum += childView.getMeasuredWidth();//计算子view的横向位置
                widthSum += getMargin(1, childView);//计算子view的横向位置
            } else if (tag > 100) {//隐藏子view, tag 100-Integer.MAX_VALUE
                Log.e(TAG, "onLayout: currentViewWidth=" + getMeasuredWidth() + "\tchildWidth=" + childView.getMeasuredWidth() + "\ttag=" + tag);
                widthSum2 += getMargin(0, childView);//隐藏子view有多少就布局多少个
                childView.layout(widthSum2, heightSum, widthSum2 + childView.getMeasuredWidth(), heightSum + childView.getMeasuredHeight());
                widthSum2 += childView.getMeasuredWidth();//计算子view的横向位置
                widthSum2 += getMargin(1, childView);//计算子view的横向位置
            }
        }
    }

    /**
     * 侧滑关闭策略：
     * 1、点击的item是‘展开’状态，则将item折叠（包括当前item及可能存在的其它item，因为RV如果多个手指同时滑动多个item时，都会进行侧滑，这跟qq不一样，是个小bug）
     * 具体实现方案：在onInterceptTouchEvent方法的ACTION_UP事件,并更新状态,同时拦截该事件（不允许调用item的onClick方法）
     * 2、被点击的item是‘折叠’状态，如果其它item有‘展开’则‘折叠’其它item（在actiondown的时候就），如果其他item都是‘折叠’则正常处理点击事件
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercepted;
        float currentX = ev.getX();
        float currentY = ev.getY();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (existExpandChildren()) {//这行代码作用：当有其它item展开的时候，点击当前item关闭其它展开的item，并将该事件停止下传
//                    Log.e(TAG, "---------------------------onInterceptTouchEvent: 特殊事件ACTION_DOWN");
                    if (getRecyclerView() != null) {//xz
                        getRecyclerView().requestDisallowInterceptTouchEvent(true);//这个方法一旦调用，除非再调用一次，否则该view永远无法拦截事件
                    }
                    specialTouch = true;//特殊事件标记，当是true的时候拦截所有的事件，但是不对事件做处理（即rv不进行滚动、slideview不进行任何滑动响应）
                }
                if (!isFold()) {//如果当前item是‘展开’状态
                    specialTouch = false;//如果当前item也是‘非折叠’状态的话，停止‘拦截一切’的举动
                    if (needFold(ev)) {
                        specialTouch2 = true;
                        intercepted = true;
                        break;
                    }
                }
                if (specialTouch) {//把特殊事件标记放这里是为了让第二个标记的判断也能走一遍
                    intercepted = true;
                    break;
                }
                //当既没有其它item展开，点击的也不是‘原始内容’时，那么将触摸事件分发给slideslipview的子view
                mLastX = currentX;
                mLastY = currentY;
                intercepted = false;
                if (getRecyclerView() != null) {//xz
                    getRecyclerView().requestDisallowInterceptTouchEvent(true);
                }
            }
            break;
            case MotionEvent.ACTION_MOVE:
                if (specialTouch) {//特殊事件、特殊处理
//                    Log.e(TAG, "---------------------------onInterceptTouchEvent: 特殊事件ACTION_MOVE");
                    intercepted = true;
                    break;
                }
                //当最开始点击的是‘隐藏内容’时会执行到这里(点击‘隐藏内容’后slideview需要判断后续的动作，如果是滑动的话那么对事件进行拦截)
                if (Math.abs(currentX - mLastX) > Math.abs(currentY - mLastY)) {//横向滑动
                    mLastX = currentX;
                    mLastY = currentY;
                    intercepted = true;//拦截事件
                    if (getRecyclerView() != null) {//xz
                        getRecyclerView().requestDisallowInterceptTouchEvent(true);//如果是横向滑动的话，禁止RV拦截接下来的事件，否则RV会拦截接下来的事件，造成滑动冲突
                    }
                } else if (Math.abs(currentY - mLastY) > Math.abs(currentX - mLastX)) {//竖直滑动
                    intercepted = false;//不拦截事件
                    if (getRecyclerView() != null) {//xz
                        getRecyclerView().requestDisallowInterceptTouchEvent(false);//允许RV拦截滑动事件，RV一旦拦截事件的话接下来所有的事件都会交给RV处理
                    }
                } else {
                    intercepted = false;
                    if (getRecyclerView() != null) {//xz
                        getRecyclerView().requestDisallowInterceptTouchEvent(false);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                intercepted = false;//不对up事件进行拦截，因为一旦拦截的话，那么子view的点击（包括长按）功能就不管用了
                break;
            default:
                intercepted = false;
        }
//        Log.e(TAG, "onInterceptTouchEvent: intercept=" + intercepted);
        return intercepted;
    }

    /**
     * 逻辑方法
     * 这个方法用来判断当手指点击‘非折叠状态’下‘原始内容’区域时是否需要自动折叠
     * true：自动折叠    false：不折叠
     */
    private boolean needFold(MotionEvent ev) {
        boolean result = false;
        for (int i = 0; i < getChildCount(); i++) {
            View childView = getChildAt(i);
            int tag = Integer.valueOf((String) childView.getTag());
            if (!(tag >= 0 && tag <= 100)) {//这里的判断是过滤掉‘隐藏内容’view
                continue;
            }
            Rect rect = new Rect();
            childView.getGlobalVisibleRect(rect);
//          Log.e(TAG, "onInterceptTouchEvent: x=" + ev.getX() + "\ty=" + ev.getY() + "\tleft=" + rect.left + "\ttop=" + rect.top + "\tright=" + rect.right + "\tbottom=" + rect.bottom);
            Rect rect1 = new Rect(rect.left, 0, rect.right, rect.bottom - rect.top);
            if (rect1.contains(Math.round(ev.getX()), Math.round(ev.getY()))) {//判断点击区域是否在‘原始内容’部分内
//              Log.e(TAG, "onInterceptTouchEvent: 点击第一个子view");
                result = true;//特殊事件标记，表示手指按下部分是否属于‘原始内容’,true：是   false：不是
                if (getRecyclerView() != null) {//xz
                    getRecyclerView().requestDisallowInterceptTouchEvent(true);
                }
                break;
            }
        }
        return result;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (specialTouch) {//特殊事件-是为了处理点击折叠状态的item，关闭展开状态的item后RV不处理触摸事件（也就是不滑动）
            boolean result;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.e(TAG, "---------------------------onTouchEvent: 特殊事件ACTION_DOWN");
                    result = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.e(TAG, "---------------------------onTouchEvent: 特殊事件ACTION_MOVE");
                    result = true;
                    break;
                case MotionEvent.ACTION_UP: {
                    Log.e(TAG, "---------------------------onTouchEvent: 特殊事件ACTION_UP");
                    specialTouch = false;
                    getRecyclerView().requestDisallowInterceptTouchEvent(false);
                    result = true;
                }
                break;
                case MotionEvent.ACTION_CANCEL: {
                    Log.e(TAG, "---------------------------onTouchEvent: 特殊事件ACTION_CANCEL");
                    specialTouch = false;
                    getRecyclerView().requestDisallowInterceptTouchEvent(false);
                    result = true;
                }
                break;
                default:
                    specialTouch = false;
                    getRecyclerView().requestDisallowInterceptTouchEvent(false);
                    result = false;
            }
            return result;
        }

        boolean result = false;//为了配合特殊事件2
        float currentX = event.getX();
        float currentY = event.getY();
        addVelocityTracker(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mLastX = currentX;
                mLastY = currentY;
                result = true;//为了配合特殊事件2
            }
            break;
            case MotionEvent.ACTION_MOVE:
                if (specialTouch2 && Math.abs(currentX - mLastX) > mTouchSlop && Math.abs(currentX - mLastX) > Math.abs(currentY - mLastY)) {
                    //这里的条件语句目的只有一个，判断 “从用户手指点击‘原始内容’部分到手指离开” 这是一个点击事件还是滑动事件
                    //当手指移动范围较大的时候看成滑动事件，否者看成点击事件；（当手指按下触摸屏幕的时候，保持不动，对用户来说他认为没有移动手指所以没有移动，
                    // 但是对系统来说即使再微小的移动也能捕获，而且确实当你手指从按下那一刻就在不停的移动，只是人很难察觉到）
                    //至于为什么要区分事件，是因为对不同事件需要做不同处理，点击事件:手指抬起时需要对展开的item折叠 ，滑动事件:手指抬起时需要对item进行自动滑动（折叠起来还是展开）
                    //而作为区分的标记就是specialTouch2，这个值最终会在action_up时用到，所以这里的设计当时也是耗费了一些时间才想到的。
                    specialTouch2 = false;//特殊事件2，一旦开始移动的话就不再算作特殊事件，也就是将特殊事件看做失效
                }
                //当用户手指滑动slideview的时候，让内容滑动起来
                if (Math.abs(currentX - mLastX) > Math.abs(currentY - mLastY)) {//注意点：这里没有使用mTouchSlop进行判断，是因为使用mTouchSlop会让滑动不流畅
                    if (currentX - mLastX > 0) {
                        //向右滑动
                        if (getScrollX() <= 0) {
                            //停止滑动，因为已经滑动到边界
                        } else {
                            scrollBy(-Math.min(getScrollX(), Math.round(currentX - mLastX)), 0);
                        }
                    } else {
                        //向左滑动
                        int childCount = getChildCount();
                        int rightBorder = getChildAt(childCount - 1).getRight();
                        Log.e(TAG, "onTouchEvent: rightBorder=" + rightBorder);
//                        if (getScrollX() >= rightBorder - getChildAt(0).getRight()) {
                        if (getScrollX() >= (rightBorder - getMeasuredWidth())) {//当滚动距离大于‘隐藏子view’区域（间距+宽度）宽度的时候停止滑动
                            //停止滑动，因为已经滑动到边界
                        } else {//当滚动距离小于‘隐藏子view’区域的宽度时，继续进行滑动（这里进行了滑动判断，防止手指移动距离大于内容可滑动距离）
//                            scrollBy(Math.min(rightBorder - getChildAt(0).getRight() - getScrollX(), Math.round(mLastX - currentX)), 0);
                            scrollBy(Math.min(rightBorder - getMeasuredWidth() - getScrollX(), Math.round(mLastX - currentX)), 0);
                        }
                    }
                    mLastX = currentX;
                    mLastY = currentY;
                }
                break;
            case MotionEvent.ACTION_UP: {
                if (specialTouch2) {
                    //如果用户点击的是‘原始内容’，折叠item
                    Log.e(TAG, "---------------------------------onTouchEvent: 特殊事件2");
                    setFold(true);
                    smoothScrollToStart();
                    if (getRecyclerView() != null) {
                        getRecyclerView().requestDisallowInterceptTouchEvent(false);
                    }
                    break;
                }
                //如果用户滑动的item，手指离开的时候让item自动滑动（这里面有用到惯性滑动）
                int childCount = getChildCount();
                int rightBorder = getChildAt(childCount - 1).getRight();
                float x_velocity = getXVelocity();
                if (x_velocity > VELOCITY_SLOP) {//向右滑动（惯性滑动）
                    Log.e(TAG, "---------------------------------onTouchEvent: 快速向右滑动");
                    setFold(true);
                    scroller.startScroll(getScrollX(), 0, -getScrollX(), 0, 200);
                    invalidate();//遗忘点，这句代码如果不加的话导致view不会惯性滑动
                } else if (x_velocity < -VELOCITY_SLOP) {//左滑动(惯性滑动)
                    Log.e(TAG, "---------------------------------onTouchEvent: 快速向左滑动");
                    setFold(false);
//                    scroller.startScroll(getScrollX(), 0, rightBorder - getChildAt(0).getRight() - getScrollX(), 0, 200);
                    scroller.startScroll(getScrollX(), 0, rightBorder - getMeasuredWidth() - getScrollX(), 0, 200);
                    invalidate();//遗忘点，这句代码如果不加的话导致view不会惯性滑动
                } else {//根据滑动距离判断是折叠还是展开
                    Log.e(TAG, "---------------------------------onTouchEvent: 自由滑动");
//                    if (getScrollX() > (rightBorder - getChildAt(0).getRight()) / 2) {
                    if (getScrollX() > (rightBorder - getMeasuredWidth()) / 2) {//当滑动距离超过‘隐藏内容’一半宽度时，手指离开屏幕后隐藏内容剩余部分自动展开
                        setFold(false);
//                        scroller.startScroll(getScrollX(), 0, rightBorder - getChildAt(0).getRight() - getScrollX(), 0, 500);
                        scroller.startScroll(getScrollX(), 0, rightBorder - getMeasuredWidth() - getScrollX(), 0, 500);
                        invalidate();
                    } else {//当滑动距离小于‘隐藏内容’一半宽度时，手指离开屏幕后隐藏内容已展开部分自动折叠
                        setFold(true);
                        scroller.startScroll(getScrollX(), 0, -getScrollX(), 0, 500);
                        invalidate();
                    }
                }
                recycleVelocityTracker();
                releaseRecyclerView();//XZ
            }
            break;
        }
        return super.onTouchEvent(event) || result;
    }

    /**
     * 工具方法(View自带方法，所有view都有该方法)
     * 页面每次重绘都会调用该方法，默认是空实现，一般都是跟scroller配合使用
     */
    @Override
    public void computeScroll() {
        super.computeScroll();
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            postInvalidate();
        }
    }

    /**
     * 工具方法
     * 获取惯性滑动的竖直速度
     */
    private void addVelocityTracker(MotionEvent event) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);
    }

    /**
     * 工具方法
     * 获取惯性滑动的横向速度
     */
    private float getXVelocity() {
        if (velocityTracker != null) {
            velocityTracker.computeCurrentVelocity(1000);
            return velocityTracker.getXVelocity();
        }
        return 0;
    }

    /**
     * 工具方法
     * VelocityTracker常规使用
     */
    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();//出错点，这里不需要再添加clear代码，否者会报错
        }
        velocityTracker = null;
    }

    /**
     * 工具方法
     * 获取该view所在列表的recyclerview对象
     */
    private RecyclerView getRecyclerView() {
        if (recyclerView != null)
            return recyclerView;
        ViewParent viewParent = this;
        while (!(viewParent.getParent() instanceof RecyclerView)) {
            viewParent = viewParent.getParent();
            if (viewParent == null) {
                Log.e(TAG, "getRecyclerView: 没有找到recyclerview");
                break;
            }
        }
        recyclerView = (viewParent == null ? null : (RecyclerView) viewParent.getParent());
        if (recyclerView != null) {
            Log.e(TAG, "getRecyclerView: 找到了recyclerview");
        }
        return recyclerView;
    }

    private void releaseRecyclerView() {
        recyclerView = null;
    }

    /**
     * view折叠状态
     * 默认为true
     * 当删除、置顶部分隐藏的时候是折叠状态，显示的时候是非折叠状态
     */
    public boolean isFold() {
        return fold;
    }

    public void setFold(boolean fold) {
        this.fold = fold;
    }

    /**
     * 工具方法
     * 遍历RV可见范围内是否有其它item是非折叠状态
     */
    private boolean existExpandChildren() {
        boolean result = false;
        if (getRecyclerView() != null) {
            int childCount = getRecyclerView().getChildCount();
            for (int i = 0; i < childCount; i++) {
                View itemView = getRecyclerView().getChildAt(i);
                SlideSlipView slideSlipView = getExpandSlideSlipView(itemView);
                if (slideSlipView != null) {
                    slideSlipView.smoothScrollToStart();
                    slideSlipView.setFold(true);
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * 工具方法
     * scroller滑动常规使用
     */
    private void smoothScrollToStart() {
        scroller.startScroll(getScrollX(), 0, -getScrollX(), 0, 500);
        postInvalidate();
    }

    /**
     * 工具方法
     * 递归获取slideslipview对象
     */
    private SlideSlipView getExpandSlideSlipView(View parentView) {
        if (parentView == null) {
            return null;
        }
        if (parentView == this) {
            return null;
        }
        if (parentView instanceof SlideSlipView) {
            if (((SlideSlipView) parentView).isFold()) {
                return null;
            } else {
                return (SlideSlipView) parentView;
            }
        }
        if (!(parentView instanceof ViewGroup)) {
            return null;
        }
        final int childCount = ((ViewGroup) parentView).getChildCount();
        for (int i = 0; i < childCount; i++) {
            SlideSlipView slipView = getExpandSlideSlipView(((ViewGroup) parentView).getChildAt(i));
            if (slipView != null) {
                if (slipView.isFold()) {
                    return null;
                } else {
                    return slipView;
                }
            }
        }
        return null;
    }

    /**
     * 工具方法
     * 获取子view的间距
     */
    private int getMargin(int type, View childView) {
        ViewGroup.LayoutParams layoutParams = childView.getLayoutParams();
        if (!(layoutParams instanceof MarginLayoutParams)) {
            return 0;
        }
        int result;
        switch (type) {
            case 0:
                result = Math.max(0, ((MarginLayoutParams) layoutParams).leftMargin);
                break;
            case 1:
                result = Math.max(0, ((MarginLayoutParams) layoutParams).rightMargin);
                break;
            case 2:
                result = Math.max(0, ((MarginLayoutParams) layoutParams).topMargin);
                break;
            default:
                result = 0;
        }
        return result;
    }
}
