package com.rouchuan;

import android.os.Build;
import android.view.View;

import rouchuan.customlayoutmanager.ViewPagerLayoutManager;

/**
 * Created by Dajavu on 12/7/16.
 * <p>
 * this layoutManager Require api21 to support elevation
 */

public class ElevateScaleLayoutManager extends ViewPagerLayoutManager {

    private static final float MIN_SCALE = 0.5f;

    private int itemSpace = 0;
    private float minScale;

    public ElevateScaleLayoutManager(int itemSpace) {
        this(itemSpace, false);
    }

    public ElevateScaleLayoutManager(int itemSpace, float minScale) {
        this(itemSpace, minScale, false);
    }

    public ElevateScaleLayoutManager(int itemSpace, boolean shouldReverseLayout) {
        this(itemSpace, MIN_SCALE, shouldReverseLayout);
    }

    public ElevateScaleLayoutManager(int itemSpace, float minScale, boolean shouldReverseLayout) {
        super(shouldReverseLayout);
        this.itemSpace = itemSpace;
        this.minScale = minScale;
    }

    @Override
    protected float setInterval() {
        return (mDecoratedChildWidth + itemSpace);
    }

    @Override
    protected void setUp() {

    }

    @Override
    protected void setItemViewProperty(View itemView, float targetOffset) {
        float scale = calculateScale((int) targetOffset + startLeft);
        float elevation = calculateElevation((int) targetOffset + startLeft);
        itemView.setScaleX(scale);
        itemView.setScaleY(scale);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            itemView.setElevation(elevation);
        }
    }

    /**
     * @param x start positon of the view you want scale
     * @return the scale rate of current scroll offset
     */
    private float calculateScale(int x) {
        float deltaX = Math.abs(x - (getHorizontalSpace() - mDecoratedChildWidth) / 2f);
        return -minScale * deltaX / (getHorizontalSpace() / 2f) + 1f;
    }

    private int calculateElevation(int x) {
        int deltaX = (int) Math.abs(x - (getHorizontalSpace() - mDecoratedChildWidth) / 2f);
        return Integer.MAX_VALUE - deltaX;
    }
}
