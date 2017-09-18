package rouchuan.customlayoutmanager;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

/**
 * Created by Dajavu on 12/7/16.
 */

public abstract class ViewPagerLayoutManager extends RecyclerView.LayoutManager {

    // Size of each items
    protected int mDecoratedChildWidth;
    protected int mDecoratedChildHeight;

    //Properties
    protected int startLeft;
    protected int startTop;
    protected float offset; //The delta of property which will change when scroll

    private boolean shouldReverseLayout;
    private int mPendingScrollPosition = NO_POSITION;
    private SavedState mPendingSavedState = null;

    protected float interval; //the interval of each item's offset

    /* package */ OnPageChangeListener onPageChangeListener;

    /**
     * Works the same way as {@link android.widget.AbsListView#setSmoothScrollbarEnabled(boolean)}.
     * see {@link android.widget.AbsListView#setSmoothScrollbarEnabled(boolean)}
     */
    private boolean mSmoothScrollbarEnabled = true;

    private boolean enableEndlessScroll = false;

    /**
     * @return the interval of each item's offset
     */
    protected abstract float setInterval();

    /**
     * You can set up your own properties here or change the exist properties like startLeft and startTop
     */
    protected abstract void setUp();

    protected abstract void setItemViewProperty(View itemView, float targetOffset);

    public ViewPagerLayoutManager() {
        this(false);
    }

    public ViewPagerLayoutManager(boolean shouldReverseLayout) {
        this.shouldReverseLayout = shouldReverseLayout;
        setAutoMeasureEnabled(true);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            offset = 0;
            return;
        }

        if (getChildCount() == 0) {
            View scrap = recycler.getViewForPosition(0);
            measureChildWithMargins(scrap, 0, 0);
            mDecoratedChildWidth = getDecoratedMeasuredWidth(scrap);
            mDecoratedChildHeight = getDecoratedMeasuredHeight(scrap);
            startLeft = (getHorizontalSpace() - mDecoratedChildWidth) / 2;
            startTop = (getVerticalSpace() - mDecoratedChildHeight) / 2;
            interval = setInterval();
            setUp();
        }

        if (mPendingSavedState != null) {
            shouldReverseLayout = mPendingSavedState.isReverseLayout;
            mPendingScrollPosition = mPendingSavedState.position;
        }

        if (mPendingScrollPosition != NO_POSITION) {
            offset = shouldReverseLayout ?
                    mPendingScrollPosition * -interval : mPendingScrollPosition * interval;
        }

        detachAndScrapAttachedViews(recycler);
        handleOutOfRange();
        layoutItems(recycler, state);

        if (!state.isPreLayout()) {
            mPendingScrollPosition = NO_POSITION;
        }

        mPendingSavedState = null;
    }

    private float getProperty(int position) {
        return !shouldReverseLayout ? position * interval : position * -interval;
    }

    @Override
    public View findViewByPosition(int position) {
        final int childCount = getChildCount();
        if (childCount == 0) {
            return null;
        }
        final int firstChild = getPosition(getChildAt(0));
        final int viewPosition = position - firstChild;
        if (viewPosition >= 0 && viewPosition < childCount) {
            final View child = getChildAt(viewPosition);
            if (getPosition(child) == position) {
                return child; // in pre-layout, this may not match
            }
        }
        // fallback to traversal. This might be necessary in pre-layout.
        return super.findViewByPosition(position);
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        removeAllViews();
        offset = 0;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState();
        savedState.position = getCurrentPositionInternal();
        savedState.isReverseLayout = shouldReverseLayout;
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            mPendingSavedState = new SavedState((SavedState) state);
            requestLayout();
        }
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void scrollToPosition(int position) {
        mPendingScrollPosition = position;
        offset = shouldReverseLayout ? position * -interval : position * interval;
        requestLayout();
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return ViewPagerLayoutManager.this.computeScrollVectorForPosition(targetPosition);
            }
        };
        linearSmoothScroller.setTargetPosition(position);
        startSmoothScroll(linearSmoothScroller);
    }

    public PointF computeScrollVectorForPosition(int targetPosition) {
        if (getChildCount() == 0) {
            return null;
        }
        final int firstChildPos = getPosition(getChildAt(0));
        final float direction = targetPosition < firstChildPos == !shouldReverseLayout ?
                -1 / getDistanceRatio() : 1 / getDistanceRatio();
        return new PointF(direction, 0);
    }

    @Override
    public int computeHorizontalScrollOffset(RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }

        if (!mSmoothScrollbarEnabled) {
            return !shouldReverseLayout ?
                    getCurrentPositionInternal() : getItemCount() - getCurrentPositionInternal() - 1;
        }

        return !shouldReverseLayout ? (int) offset : (int) (Math.abs(getMinOffset()) + offset);
    }

    @Override
    public int computeHorizontalScrollExtent(RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }

        if (!mSmoothScrollbarEnabled) {
            return 1;
        }

        return !shouldReverseLayout ?
                (int) (getMaxOffset() / getItemCount()) : (int) Math.abs(getMinOffset() / getItemCount());
    }

    @Override
    public int computeHorizontalScrollRange(RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }

        if (!mSmoothScrollbarEnabled) {
            return getItemCount();
        }

        return !shouldReverseLayout ? (int) getMaxOffset() : (int) Math.abs(getMinOffset());
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0 || dx == 0) {
            return 0;
        }

        int willScroll = dx;

        float realDx = dx / getDistanceRatio();
        float targetOffset = offset + realDx;

        //handle the boundary
        if (!enableEndlessScroll && targetOffset < getMinOffset()) {
            willScroll = 0;
        } else if (!enableEndlessScroll && targetOffset > getMaxOffset()) {
            willScroll = (int) ((getMaxOffset() - offset) * getDistanceRatio());
        }

        realDx = willScroll / getDistanceRatio();

        offset += realDx;

        //re-calculate the rotate x,y of each items
        for (int i = 0; i < getChildCount(); i++) {
            View scrap = getChildAt(i);
            float delta = propertyChangeWhenScroll(scrap) - realDx;
            layoutScrap(scrap, delta);
        }

        layoutItems(recycler, state);

        return willScroll;
    }

    private void layoutItems(RecyclerView.Recycler recycler,
                             RecyclerView.State state) {
        if (state.isPreLayout()) return;

        //remove the views which out of range
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            int position = getPosition(view);
            if (removeCondition(getProperty(position) - offset)) {
                removeAndRecycleView(view, recycler);
            }
        }

        final int currentPos = getCurrentPositionInternal();
        final float curOffset = getProperty(currentPos) - offset;

        int start = (int) (currentPos - Math.abs(((curOffset - minRemoveOffset()) / interval))) - 1;
        int end = (int) (currentPos + Math.abs(((curOffset - maxRemoveOffset()) / interval))) + 1;

        if (start < 0 && !enableEndlessScroll) start = 0;
        final int itemCount = getItemCount();
        if (end > itemCount && !enableEndlessScroll) end = itemCount;

        for (int i = start; i < end; i++) {
            if (!removeCondition(getProperty(i) - offset)) {
                int realIndex = i;
                if (i >= itemCount) {
                    realIndex %= itemCount;
                } else if (i < 0) {
                    int delta = (-realIndex) % itemCount;
                    if (delta == 0) delta = itemCount;
                    realIndex = itemCount - delta;
                }
                if (findViewByPosition(i) == null) {
                    View scrap = recycler.getViewForPosition(realIndex);
                    measureChildWithMargins(scrap, 0, 0);
                    addView(scrap);
                    resetViewProperty(scrap);
                    float targetOffset = getProperty(i) - offset;
                    layoutScrap(scrap, targetOffset);
                }
            }
        }

        if (enableEndlessScroll) {
            if (getCurrentPositionInternal() == 0) {
                removeAndRecycleAllViews(recycler);
                internalScrollToPosition(itemCount, recycler, state);
            } else if (getCurrentPositionInternal() == itemCount + 1) {
                removeAndRecycleAllViews(recycler);
                internalScrollToPosition(1, recycler, state);
            }
        }
    }

    private void internalScrollToPosition(int position, RecyclerView.Recycler recycler, RecyclerView.State state) {
        offset = shouldReverseLayout ? position * -interval : position * interval;
        layoutItems(recycler, state);
    }

    private boolean removeCondition(float targetOffset) {
        return targetOffset > maxRemoveOffset() || targetOffset < minRemoveOffset();
    }

    private void handleOutOfRange() {
        if (offset < getMinOffset()) {
            offset = getMinOffset();
        }
        if (offset > getMaxOffset()) {
            offset = getMaxOffset();
        }
    }

    private void resetViewProperty(View v) {
        v.setRotation(0);
        v.setRotationY(0);
        v.setRotationX(0);
        v.setScaleX(1f);
        v.setScaleY(1f);
        v.setAlpha(1f);
    }

    private float getMaxOffset() {
        return !shouldReverseLayout ?
                (enableEndlessScroll ? (getItemCount() + 1) : (getItemCount() - 1)) * interval : 0;
    }

    private float getMinOffset() {
        return !shouldReverseLayout ?
                0 : -(enableEndlessScroll ? (getItemCount() + 1) : (getItemCount() - 1)) * interval;
    }

    private void layoutScrap(View scrap, float targetOffset) {
        int left = calItemLeftPosition(targetOffset);
        int top = calItemTopPosition(targetOffset);
        layoutDecorated(scrap, startLeft + left, startTop + top,
                startLeft + left + mDecoratedChildWidth, startTop + top + mDecoratedChildHeight);
        setItemViewProperty(scrap, targetOffset);
    }

    protected int calItemLeftPosition(float targetOffset) {
        return (int) targetOffset;
    }

    protected int calItemTopPosition(float targetOffset) {
        return 0;
    }

    protected int getHorizontalSpace() {
        return getWidth() - getPaddingRight() - getPaddingLeft();
    }

    protected int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    protected float maxRemoveOffset() {
        return getHorizontalSpace() - startLeft;
    }

    protected float minRemoveOffset() {
        return -mDecoratedChildWidth - getPaddingLeft() - startLeft;
    }

    protected float propertyChangeWhenScroll(View itemView) {
        return itemView.getLeft() - startLeft;
    }

    protected float getDistanceRatio() {
        return 1f;
    }

    public int getCurrentPosition() {
        int position = getCurrentPositionInternal();
        if (enableEndlessScroll && position > getItemCount()) return position - getItemCount();
        else if (enableEndlessScroll && position < 0) return position + getItemCount();
        return position;
    }

    private int getCurrentPositionInternal() {
        return Math.round(Math.abs(offset) / interval);
    }

    public int getOffsetCenterView() {
        return (int) ((getCurrentPositionInternal() * (!shouldReverseLayout ?
                interval : -interval) - offset) * getDistanceRatio());
    }

    public void setOnPageChangeListener(OnPageChangeListener onPageChangeListener) {
        this.onPageChangeListener = onPageChangeListener;
    }

    public void setEnableEndlessScroll(boolean enable) {
        enableEndlessScroll = enable;
    }

    /**
     * When smooth scrollbar is enabled, the position and size of the scrollbar thumb is computed
     * based on the number of visible pixels in the visible items. This however assumes that all
     * list items have similar or equal widths or heights (depending on list orientation).
     * If you use a list in which items have different dimensions, the scrollbar will change
     * appearance as the user scrolls through the list. To avoid this issue,  you need to disable
     * this property.
     * <p>
     * When smooth scrollbar is disabled, the position and size of the scrollbar thumb is based
     * solely on the number of items in the adapter and the position of the visible items inside
     * the adapter. This provides a stable scrollbar as the user navigates through a list of items
     * with varying widths / heights.
     *
     * @param enabled Whether or not to enable smooth scrollbar.
     * @see #setSmoothScrollbarEnabled(boolean)
     */
    public void setSmoothScrollbarEnabled(boolean enabled) {
        mSmoothScrollbarEnabled = enabled;
    }

    public void setStackFromEnd(boolean stackFromEnd) {
        shouldReverseLayout = stackFromEnd;
        requestLayout();
    }

    public boolean getStackFromEnd() {
        return shouldReverseLayout;
    }

    /**
     * Returns the current state of the smooth scrollbar feature. It is enabled by default.
     *
     * @return True if smooth scrollbar is enabled, false otherwise.
     * @see #setSmoothScrollbarEnabled(boolean)
     */
    public boolean isSmoothScrollbarEnabled() {
        return mSmoothScrollbarEnabled;
    }

    private static class SavedState implements Parcelable {
        int position;
        boolean isReverseLayout;

        SavedState() {

        }

        SavedState(Parcel in) {
            position = in.readInt();
            isReverseLayout = in.readInt() == 1;
        }

        public SavedState(SavedState other) {
            position = other.position;
            isReverseLayout = other.isReverseLayout;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(position);
            dest.writeInt(isReverseLayout ? 1 : 0);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public interface OnPageChangeListener {
        void onPageSelected(int position);

        void onPageScrollStateChanged(int state);
    }
}
