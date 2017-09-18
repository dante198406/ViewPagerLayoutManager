package com.rouchuan;

import android.view.View;

import rouchuan.customlayoutmanager.ViewPagerLayoutManager;

/**
 * Created by Dajavu on 12/7/16.
 */

public class CircleLayoutManager extends ViewPagerLayoutManager {

    private static int INTERVAL_ANGLE = 30;// The default interval angle between each items
    private static float DISTANCE_RATIO = 10f; // Finger swipe distance divide item rotate angle

    private int mRadius;

    public CircleLayoutManager() {
        super();
    }

    public CircleLayoutManager(boolean shouldReverseLayout) {
        super(shouldReverseLayout);
    }

    @Override
    protected float setInterval() {
        return INTERVAL_ANGLE;
    }

    @Override
    protected void setUp() {
        mRadius = mDecoratedChildHeight;
    }

    @Override
    protected float maxRemoveOffset() {
        return 90;
    }

    @Override
    protected float minRemoveOffset() {
        return -90;
    }

    @Override
    protected int calItemLeftPosition(float targetOffset) {
        return (int) (mRadius * Math.cos(Math.toRadians(90 - targetOffset)));
    }

    @Override
    protected int calItemTopPosition(float targetOffset) {
        return (int) (mRadius - mRadius * Math.sin(Math.toRadians(90 - targetOffset)));
    }

    @Override
    protected void setItemViewProperty(View itemView, float targetOffset) {
        itemView.setRotation(targetOffset);
    }

    @Override
    protected float propertyChangeWhenScroll(View itemView) {
        return itemView.getRotation();
    }

    @Override
    protected float getDistanceRatio() {
        return DISTANCE_RATIO;
    }
}
