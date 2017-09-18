package com.rouchuan;

import android.view.View;

import rouchuan.customlayoutmanager.ViewPagerLayoutManager;

/**
 * Created by Dajavu on 12/7/16.
 */

public class ScaleLayoutManager extends ViewPagerLayoutManager {

    private static final float SCALE_RATE = 1.2f;
    private int itemSpace = 0;

    public ScaleLayoutManager(int itemSpace) {
        this(itemSpace, false);
    }

    public ScaleLayoutManager(int itemSpace, boolean shouldReverseLayout) {
        super(shouldReverseLayout);
        this.itemSpace = itemSpace;
    }

    @Override
    protected float setInterval() {
        return (int) (mDecoratedChildWidth * ((SCALE_RATE - 1f) / 2f + 1) + itemSpace);
    }

    @Override
    protected void setUp() {

    }

    @Override
    protected void setItemViewProperty(View itemView, float targetOffset) {
        float scale = calculateScale((int) targetOffset + startLeft);
        itemView.setScaleX(scale);
        itemView.setScaleY(scale);
    }

    /**
     * @param x start positon of the view you want scale
     * @return the scale rate of current scroll offset
     */
    private float calculateScale(int x) {
        int deltaX = Math.abs(x - (getHorizontalSpace() - mDecoratedChildWidth) / 2);
        float diff = 0f;
        if ((mDecoratedChildWidth - deltaX) > 0) diff = mDecoratedChildWidth - deltaX;
        return (SCALE_RATE - 1f) / mDecoratedChildWidth * diff + 1;
    }
}
