/*
 * Copyright (C) 2013 47 Degrees, LLC
 * http://47deg.com
 * hello@47deg.com
 *
 * Copyright 2012 Roman Nurik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fortysevendeg.swipelistview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.graphics.Rect;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Touch listener impl for the SwipeListView
 */
public class SwipeListViewTouchListener implements View.OnTouchListener {
    private int swipeMode = SwipeListView.SWIPE_MODE_BOTH;
    private boolean swipeClosesAllItemsWhenListMoves = true;

    private int cancelAnimationDuration;
    private int swipeFrontView = 0;
    private int swipeBackView = 0;

    private Rect rect = new Rect();

    // Cached ViewConfiguration and system-wide constant values
    private int slop;
    private int minFlingVelocity;
    private int maxFlingVelocity;
    private long configShortAnimationTime;
    private long animationTime;

    private float leftOffsetPer = 1f;
    private float rightOffsetPer = 1f;

    // Fixed properties
    private SwipeListView swipeListView;
    private int viewWidth = 1; // 1 and not 0 to prevent dividing by zero

    private List<PendingDismissData> pendingDismisses = new ArrayList<PendingDismissData>();
    private int dismissAnimationRefCount = 0;

    private float downX;
    private boolean swiping;
    private boolean swipingRight;
    private VelocityTracker velocityTracker;
    private int downPosition;
    private View parentView;
    private View frontView;
    private View backView;
    private boolean paused;

    private int swipeActionLeft = SwipeListView.SWIPE_ACTION_REVEAL;
    private int swipeActionRight = SwipeListView.SWIPE_ACTION_REVEAL;

    private List<Boolean> opened = new ArrayList<Boolean>();
    private List<Boolean> openedRight = new ArrayList<Boolean>();
    private boolean listViewMoving;
    private int oldSwipeActionRight;
    private int oldSwipeActionLeft;
    private boolean pullDirection;

    /**
     * Constructor
     *
     * @param swipeListView  SwipeListView
     * @param swipeFrontView front view Identifier
     * @param swipeBackView  back view Identifier
     */
    public SwipeListViewTouchListener(SwipeListView swipeListView, int swipeFrontView, int swipeBackView) {
        this.cancelAnimationDuration = swipeListView.getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);
        this.swipeFrontView = swipeFrontView;
        this.swipeBackView = swipeBackView;
        ViewConfiguration vc = ViewConfiguration.get(swipeListView.getContext());
        slop = vc.getScaledTouchSlop();
        minFlingVelocity = vc.getScaledMinimumFlingVelocity();
        maxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        configShortAnimationTime = swipeListView.getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);
        animationTime = configShortAnimationTime;
        this.swipeListView = swipeListView;
    }

    /**
     * Sets current item's parent view
     *
     * @param parentView Parent view
     */
    private void setParentView(View parentView) {
        this.parentView = parentView;
    }

    /**
     * Sets current item's front view
     *
     * @param frontView Front view
     */
    private void setFrontView(View frontView) {
        this.frontView = frontView;
        frontView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipeListView.onClickFrontView(downPosition);
            }
        });
    }

    /**
     * Set current item's back view
     *
     * @param backView
     */
    private void setBackView(View backView) {
        this.backView = backView;
        backView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipeListView.onClickBackView(downPosition);
            }
        });
    }

    /**
     * @return true if the list is in motion
     */
    public boolean isListViewMoving() {
        return listViewMoving;
    }

    /**
     * Sets animation time when the user drops the cell
     *
     * @param animationTime milliseconds
     */
    public void setAnimationTime(long animationTime) {
        if (animationTime > 0) {
            this.animationTime = animationTime;
        } else {
            this.animationTime = configShortAnimationTime;
        }
    }

    /**
     * Sets the right offset percentage
     *
     * @param rightOffsetPer OffsetPer
     */
    public void setRightOffsetPer(float rightOffsetPer) {
        this.rightOffsetPer = rightOffsetPer;
    }

    /**
     * Set the left offset percentage
     *
     * @param leftOffsetPer OffsetPer
     */
    public void setLeftOffsetPer(float leftOffsetPer) {
        this.leftOffsetPer = leftOffsetPer;
    }

    /**
     * Set if all item opened will be close when the user move ListView
     *
     * @param swipeClosesAllItemsWhenListMoves
     */
    public void setSwipeClosesAllItemsWhenListMoves(boolean swipeClosesAllItemsWhenListMoves) {
        this.swipeClosesAllItemsWhenListMoves = swipeClosesAllItemsWhenListMoves;
    }

    /**
     * Sets the swipe mode
     *
     * @param swipeMode
     */
    public void setSwipeMode(int swipeMode) {
        this.swipeMode = swipeMode;
    }

    /**
     * Check is swiping is enabled
     *
     * @return
     */
    protected boolean isSwipeEnabled() {
        return swipeMode != SwipeListView.SWIPE_MODE_NONE;
    }

    /**
     * Return action on left
     *
     * @return Action
     */
    public int getSwipeActionLeft() {
        return swipeActionLeft;
    }

    /**
     * Set action on left
     *
     * @param swipeActionLeft Action
     */
    public void setSwipeActionLeft(int swipeActionLeft) {
        this.swipeActionLeft = swipeActionLeft;
    }

    /**
     * Return action on right
     *
     * @return Action
     */
    public int getSwipeActionRight() {
        return swipeActionRight;
    }

    /**
     * Set action on right
     *
     * @param swipeActionRight Action
     */
    public void setSwipeActionRight(int swipeActionRight) {
        this.swipeActionRight = swipeActionRight;
    }

    /**
     * Adds new items when adapter is modified
     */
    public void resetItems() {
        if (swipeListView.getAdapter() != null) {
            int count = swipeListView.getAdapter().getCount();
            for (int i = opened.size(); i <= count; i++) {
                opened.add(false);
                openedRight.add(false);
            }
        }
    }

    /**
     * Open item
     *
     * @param position Position of list
     */
    protected void openAnimate(int position) {
        final View child = swipeListView.getChildAt(position - swipeListView.getFirstVisiblePosition()).findViewById(swipeFrontView);

        if (child != null) {
            openAnimate(child, position);
        }
    }

    /**
     * Close item
     *
     * @param position Position of list
     */
    protected void closeAnimate(int position) {
        final View child = swipeListView.getChildAt(position - swipeListView.getFirstVisiblePosition()).findViewById(swipeFrontView);

        if (child != null) {
            closeAnimate(child, position);
        }
    }

    /**
     * Dismiss an item.
     * @param position is the position of the item to delete.
     * @return 0 if the item is not visible. Otherwise return the height of the cell to dismiss.
     */
    protected int dismiss(int position) {
        opened.remove(position);
        int start = swipeListView.getFirstVisiblePosition();
        int end = swipeListView.getLastVisiblePosition();
        View view = swipeListView.getChildAt(position - start);
        ++dismissAnimationRefCount;
        if (position >= start && position <= end) {
            performDismiss(view, position, false);
            return view.getHeight();
        } else {
            pendingDismisses.add(new PendingDismissData(position, null));
            return 0;
        }
    }

    /**
     * Reset the state of front view when the it's recycled by ListView
     *
     * @param frontView view to re-draw
     */
    protected void reloadSwipeStateInView(View frontView, int position) {
        if (!opened.get(position)) {
            frontView.setTranslationX(0.0f);
        } else {
            if (openedRight.get(position)) {
                frontView.setTranslationX(swipeListView.getWidth());
            } else {
                frontView.setTranslationX(-swipeListView.getWidth());
            }
        }

    }

    /**
     * Open item
     *
     * @param view     affected view
     * @param position Position of list
     */
    private void openAnimate(View view, int position) {
        if (!opened.get(position)) {
            generateTranslateAnimate(view, true, false, position);
        }
    }

    /**
     * Close item
     *
     * @param view     affected view
     * @param position Position of list
     */
    private void closeAnimate(View view, int position) {
        if (opened.get(position)) {
            generateTranslateAnimate(view, true, false, position);
        }
    }

    /**
     * Create animation
     *
     * @param view      affected view
     * @param moveOut      If state should change. If "false" returns to the original position
     * @param moveToRight If moveOut is true, this parameter tells if move is to the right or left
     * @param position  Position of list
     */
    private void generateAnimation(final View view, final boolean moveOut, final boolean moveToRight, final int position) {
        if(SwipeListView.DEBUG){
            Log.d(SwipeListView.TAG, "moveOut: " + moveOut + " - moveToRight: " + moveToRight + " - position: " + position);
        }

        boolean toDismiss = !opened.get(downPosition)
                && (swipingRight && swipeActionRight == SwipeListView.SWIPE_ACTION_DISMISS
                || !swipingRight && swipeActionLeft == SwipeListView.SWIPE_ACTION_DISMISS);
        if (toDismiss) {
            generateDismissAnimate(parentView, moveOut, moveToRight, position);
        }
        else {
            generateTranslateAnimate(view, moveOut, moveToRight, position);
        }
    }

    /**
     * Create dismiss animation
     *
     * @param view      affected view
     * @param moveOut      If will change state. If is "false" returns to the original position
     * @param moveToRight If moveOut is true, this parameter tells if move is to the right or left
     * @param position  Position of list
     */
    private void generateDismissAnimate(final View view, final boolean moveOut, final boolean moveToRight, final int position) {
        int moveTo = 0;

        if (!opened.get(position) && moveOut) {
            moveTo = moveToRight ? viewWidth : -viewWidth;
        }

        int alpha = 1;
        if (moveOut) {
            ++dismissAnimationRefCount;
            alpha = 0;
        }

        view.animate()
                .translationX(moveTo)
                .alpha(alpha)
                .setDuration(animationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (moveOut) {
                            closeOpenedItems();
                            performDismiss(view, position, true);
                        }
                        resetCell();
                    }
                });

    }

    /**
     * Create translate animation
     *
     * @param view      affected view
     * @param moveOut      If will change state. If "false" returns to the original position
     * @param moveToRight If moveOut is true, this parameter tells if movement is toward right or left
     * @param position  list position
     */
    private void generateTranslateAnimate(final View view, final boolean moveOut, final boolean moveToRight, final int position) {
        int moveTo = 0;

        if (opened.get(position) && !moveOut) {
            moveTo = openedRight.get(position) ? viewWidth : -viewWidth;
        }
        else if (!opened.get(position) && moveOut) {
            moveTo = moveToRight ? viewWidth : -viewWidth;
        }

        view.animate()
                .translationX(moveTo)
                .setDuration(animationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        swipeListView.resetScrolling();
                        if (!opened.get(position) && moveOut) {
                            opened.set(position, true);
                            openedRight.set(position, moveToRight);
                            swipeListView.onOpened(position, moveToRight);
                        }
                        else if (opened.get(position) && moveOut){
                            opened.set(position, false);
                            swipeListView.onClosed(position, openedRight.get(position));
                            openedRight.set(position, false);
                        }
                        resetCell();
                    }
                });
    }

    private void resetCell() {
        if (downPosition != ListView.INVALID_POSITION) {
            frontView.setClickable(opened.get(downPosition));
            frontView.setLongClickable(opened.get(downPosition));
            frontView = null;
            backView = null;
            downPosition = ListView.INVALID_POSITION;
        }
    }

    /**
     * Set enabled
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        paused = !enabled;
    }

    /**
     * Return ScrollListener for ListView
     *
     * @return OnScrollListener
     */
    public AbsListView.OnScrollListener makeScrollListener() {
        return new AbsListView.OnScrollListener() {

            private boolean isFirstItem = false;
            private boolean isLastItem = false;

            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                if (swipeClosesAllItemsWhenListMoves && scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    closeOpenedItems();
                }
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    listViewMoving = true;
                    setEnabled(false);
                }
                if (scrollState != AbsListView.OnScrollListener.SCROLL_STATE_FLING && scrollState != SCROLL_STATE_TOUCH_SCROLL) {
                    listViewMoving = false;
                    downPosition = ListView.INVALID_POSITION;
                    swipeListView.resetScrolling();
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            setEnabled(true);
                        }
                    }, 500);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (isFirstItem) {
                    boolean onSecondItemList = firstVisibleItem == 1;
                    if (onSecondItemList) {
                        isFirstItem = false;
                    }
                } else {
                    boolean onFirstItemList = firstVisibleItem == 0;
                    if (onFirstItemList) {
                        isFirstItem = true;
                        swipeListView.onFirstListItem();
                    }
                }
                if (isLastItem) {
                    boolean onBeforeLastItemList = firstVisibleItem + visibleItemCount == totalItemCount - 1;
                    if (onBeforeLastItemList) {
                        isLastItem = false;
                    }
                } else {
                    boolean onLastItemList = firstVisibleItem + visibleItemCount >= totalItemCount;
                    if (onLastItemList) {
                        isLastItem = true;
                        swipeListView.onLastListItem();
                    }
                }
            }
        };
    }

    /**
     * Close all opened items
     */
    void closeOpenedItems() {
        if (opened != null) {
            int start = swipeListView.getFirstVisiblePosition();
            int end = swipeListView.getLastVisiblePosition();
            for (int i = start; i <= end; i++) {
                if (opened.get(i)) {
                    closeAnimate(swipeListView.getChildAt(i - start).findViewById(swipeFrontView), i);
                }
            }
        }

    }

    /**
     * @see OnTouchListener#onTouch(View, MotionEvent)
     */
    @TargetApi(VERSION_CODES.KITKAT)
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (!isSwipeEnabled()) {
            return false;
        }

        if (viewWidth < 2) {
            viewWidth = swipeListView.getWidth();
        }

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (paused && downPosition != ListView.INVALID_POSITION) {
                    return false;
                }

                int childCount = swipeListView.getChildCount();
                int[] listViewCoords = new int[2];
                swipeListView.getLocationOnScreen(listViewCoords);
                int x = (int) motionEvent.getRawX() - listViewCoords[0];
                int y = (int) motionEvent.getRawY() - listViewCoords[1];
                View child;
                for (int i = 0; i < childCount; i++) {
                    child = swipeListView.getChildAt(i);
                    child.getHitRect(rect);

                    int childPosition = swipeListView.getPositionForView(child);

                    // dont allow swiping if this is on the header or footer or IGNORE_ITEM_VIEW_TYPE or enabled is false on the adapter
                    boolean isEnabled = swipeListView.getAdapter().isEnabled(childPosition);
                    boolean isIgnored = swipeListView.getAdapter().getItemViewType(childPosition) >= 0;
                    boolean allowSwipe = isEnabled && isIgnored;

                    if (allowSwipe && rect.contains(x, y)) {
                        setParentView(child);
                        setFrontView(child.findViewById(swipeFrontView));

                        downX = motionEvent.getRawX();
                        downPosition = childPosition - swipeListView.getHeaderViewsCount();

                        frontView.setClickable(!opened.get(downPosition));
                        frontView.setLongClickable(!opened.get(downPosition));

                        velocityTracker = VelocityTracker.obtain();
                        velocityTracker.addMovement(motionEvent);
                        if (swipeBackView > 0) {
                            setBackView(child.findViewById(swipeBackView));
                        }
                        break;
                    }
                }
                view.onTouchEvent(motionEvent);
                return true;
            }
            case MotionEvent.ACTION_CANCEL: {
                if (velocityTracker == null || !swiping || downPosition == ListView.INVALID_POSITION) {
                    break;
                }
                frontView.animate().translationX(0).setDuration(cancelAnimationDuration);
                velocityTracker.recycle();
                velocityTracker = null;
                downX = 0;
                swiping = false;
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (velocityTracker == null || !swiping || downPosition == ListView.INVALID_POSITION) {
                    break;
                }

                float deltaX = motionEvent.getRawX() - downX;
                velocityTracker.addMovement(motionEvent);
                velocityTracker.computeCurrentVelocity(1000);
                float velocityX = velocityTracker.getXVelocity();
                float velocityXAbs = Math.abs(velocityX);
                if (!opened.get(downPosition)) {
                    if (swipeMode == SwipeListView.SWIPE_MODE_LEFT && velocityX > 0) {
                        velocityXAbs = 0;
                    }
                    if (swipeMode == SwipeListView.SWIPE_MODE_RIGHT && velocityX < 0) {
                        velocityXAbs = 0;
                    }
                }

                float velocityY = Math.abs(velocityTracker.getYVelocity());
                boolean moveOut = false;
                boolean moveToRight = false;
                boolean opened = this.opened.get(downPosition);
                if (minFlingVelocity <= velocityXAbs && velocityXAbs <= maxFlingVelocity && velocityY * 2 < velocityXAbs) {
                    moveToRight = velocityX > 0;

                    if (SwipeListView.DEBUG) {
                        Log.d(SwipeListView.TAG, "moveToRight: " + moveToRight + " - swipingRight: " + swipingRight);
                    }

                    // この時点でフリング動作は確定
                    if (!opened && moveToRight && (!swipingRight || swipeActionRight == SwipeListView.SWIPE_ACTION_NONE)) {
                        moveOut = false;
                    } else if (!opened && !moveToRight && (swipingRight || swipeActionLeft == SwipeListView.SWIPE_ACTION_NONE)) {
                        moveOut = false;
                    // 右に開いている状態で右にフリングは無視
                    } else if (opened && openedRight.get(downPosition) && moveToRight) {
                        moveOut = false;
                    // 左に開いている状態で左にフリングは無視
                    } else if (opened && !openedRight.get(downPosition) && !moveToRight) {
                        moveOut = false;
                    } else {
                        moveOut = true;
                    }
                } else if (Math.abs(deltaX) > viewWidth / 2) {
                    moveToRight = deltaX > 0;
                    // この時点でフリング動作は確定
                    if (!opened && moveToRight && swipeActionRight == SwipeListView.SWIPE_ACTION_NONE) {
                        moveOut = false;
                    } else if (!opened && !moveToRight && swipeActionLeft == SwipeListView.SWIPE_ACTION_NONE) {
                        moveOut = false;
                        // 右に開いている状態で右にフリングは無視
                    } else {
                        moveOut = true;
                    }
                }

                generateAnimation(frontView, moveOut, moveToRight, downPosition);

                velocityTracker.recycle();
                velocityTracker = null;
                downX = 0;
                swiping = false;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (velocityTracker == null || paused || downPosition == ListView.INVALID_POSITION) {
                    break;
                }

                velocityTracker.addMovement(motionEvent);
                velocityTracker.computeCurrentVelocity(1000);
                float velocityX = Math.abs(velocityTracker.getXVelocity());
                float velocityY = Math.abs(velocityTracker.getYVelocity());

                float deltaX = motionEvent.getRawX() - downX;
                float deltaXAbs = Math.abs(deltaX);

                int swipeMode = this.swipeMode;
                int changeSwipeMode = swipeListView.changeSwipeMode(downPosition);
                if (changeSwipeMode >= 0) {
                    swipeMode = changeSwipeMode;
                }

                if (swipeMode == SwipeListView.SWIPE_MODE_NONE) {
                    break;
                }

                if (swipeMode != SwipeListView.SWIPE_MODE_BOTH) {
                    if (opened.get(downPosition)) {
                        // 開いている方向に更にスワイプしても何もしない
                        if (swipeMode == SwipeListView.SWIPE_MODE_LEFT && deltaX < 0) {
                            break;
                        } else if (swipeMode == SwipeListView.SWIPE_MODE_RIGHT && deltaX > 0) {
                            break;
                        }
                    } else {
                        // 閉じている方向に更にスワイプしても何もしない
                        if (swipeMode == SwipeListView.SWIPE_MODE_LEFT && deltaX > 0) {
                            break;
                        } else if (swipeMode == SwipeListView.SWIPE_MODE_RIGHT && deltaX < 0) {
                            break;
                        }
                    }
                }

                if (deltaXAbs > slop && velocityY < velocityX) {
                    swiping = true;
                    swipingRight = (deltaX > 0);

                    if (SwipeListView.DEBUG) {
                        Log.d(SwipeListView.TAG, "deltaX: " + deltaX + " - swipingRight: " + swipingRight);
                    }

                    // notify close or open via listener
                    if (opened.get(downPosition)) {
                        swipeListView.onStartClose(downPosition, swipingRight);
                    } else {
                        swipeListView.onStartOpen(downPosition, swipingRight);
                    }

                    swipeListView.requestDisallowInterceptTouchEvent(true);
                    MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                            (motionEvent.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                    swipeListView.onTouchEvent(cancelEvent);
                }

                if (swiping && downPosition != ListView.INVALID_POSITION) {
                    if (opened.get(downPosition)) {
                        deltaX += openedRight.get(downPosition) ? viewWidth : -viewWidth;
                    }
                    if (pullDirection && !opened.get(downPosition)) {
                        deltaX *= openedRight.get(downPosition) ? rightOffsetPer : leftOffsetPer;
                    }

                    boolean toDismiss = !opened.get(downPosition)
                            && (swipingRight && swipeActionRight == SwipeListView.SWIPE_ACTION_DISMISS
                            || !swipingRight && swipeActionLeft == SwipeListView.SWIPE_ACTION_DISMISS);
                    move(deltaX, toDismiss);
                    return true;
                }
                break;
            }
        }
        return false;
    }

    private void setActionsTo(int action) {
        oldSwipeActionRight = swipeActionRight;
        oldSwipeActionLeft = swipeActionLeft;
        swipeActionRight = action;
        swipeActionLeft = action;
    }

    protected void returnOldActions() {
        swipeActionRight = oldSwipeActionRight;
        swipeActionLeft = oldSwipeActionLeft;
    }

    /**
     * Moves the view
     *
     * @param deltaX delta
     */
    public void move(float deltaX, boolean dismiss) {
        Log.d(SwipeListViewTouchListener.class.getSimpleName(), "move = " + deltaX);

        swipeListView.onMove(downPosition, deltaX);
        float posX = frontView.getX();
        if (opened.get(downPosition)) {
            posX += openedRight.get(downPosition) ? -viewWidth : viewWidth;
        }
        if (posX > 0 && !swipingRight) {
            if(SwipeListView.DEBUG){
                Log.d(SwipeListView.TAG, "change to right");
            }
            swipingRight = !swipingRight;
        }
        if (posX < 0 && swipingRight) {
            if(SwipeListView.DEBUG){
                Log.d(SwipeListView.TAG, "change to left");
            }
            swipingRight = !swipingRight;
        }

        if (dismiss) {
            frontView.setTranslationX(0);
            parentView.setTranslationX(deltaX);
            parentView.setAlpha(Math.max(0f, Math.min(1f,
                    1f - 2f * Math.abs(deltaX) / viewWidth)));
        } else {
            parentView.setTranslationX(0);
            parentView.setAlpha(1f);
            frontView.setTranslationX(deltaX);
        }
    }

    public void setPullDirection(boolean pullDirection) {
        this.pullDirection = pullDirection;
    }

    public boolean isPullDirection() {
        return pullDirection;
    }

    /**
     * Class that saves pending dismiss data
     */
    class PendingDismissData implements Comparable<PendingDismissData> {
        public int position;
        public View view;

        public PendingDismissData(int position, View view) {
            this.position = position;
            this.view = view;
        }

        @Override
        public int compareTo(PendingDismissData other) {
            // Sort by descending position
            return other.position - position;
        }
    }

    /**
     * Perform dismiss action
     *
     * @param dismissView     View
     * @param dismissPosition Position of list
     */
    protected void performDismiss(final View dismissView, final int dismissPosition, boolean doPendingDismiss) {
        enableDisableViewGroup((ViewGroup) dismissView, false);
        final ViewGroup.LayoutParams lp = dismissView.getLayoutParams();
        final int originalHeight = dismissView.getHeight();

        ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(animationTime);

        if (doPendingDismiss) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    --dismissAnimationRefCount;
                    if (dismissAnimationRefCount == 0) {
                        removePendingDismisses(originalHeight);
                    }
                }
            });
        }

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                enableDisableViewGroup((ViewGroup) dismissView, true);
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                lp.height = (Integer) valueAnimator.getAnimatedValue();
                dismissView.setLayoutParams(lp);
            }
        });

        pendingDismisses.add(new PendingDismissData(dismissPosition, dismissView));
        animator.start();
    }

    /**
     * Remove all pending dismisses.
     */
    protected void resetPendingDismisses() {
        pendingDismisses.clear();
    }

    /**
     * Will call {@link #removePendingDismisses(int)} in animationTime + 100 ms.
     * @param originalHeight will be used to rest the cells height.
     */
    protected void handlerPendingDismisses(final int originalHeight) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                removePendingDismisses(originalHeight);
            }
        }, animationTime + 100);
    }

    /**
     * Will delete all pending dismisses.
     * Will call callback onDismiss for all pending dismisses.
     * Will reset all cell height to originalHeight.
     * @param originalHeight is the height of the cell before animation.
     */
    private void removePendingDismisses(int originalHeight) {
        // No active animations, process all pending dismisses.
        // Sort by descending position
        Collections.sort(pendingDismisses);

        int[] dismissPositions = new int[pendingDismisses.size()];
        for (int i = pendingDismisses.size() - 1; i >= 0; i--) {
            dismissPositions[i] = pendingDismisses.get(i).position;
        }
        swipeListView.onDismiss(dismissPositions);

        ViewGroup.LayoutParams lp;
        for (PendingDismissData pendingDismiss : pendingDismisses) {
            // Reset view presentation
            if (pendingDismiss.view != null) {
                pendingDismiss.view.setAlpha(1f);
                pendingDismiss.view.setTranslationX(0);
                lp = pendingDismiss.view.getLayoutParams();
                lp.height = originalHeight;
                pendingDismiss.view.setLayoutParams(lp);
            }
        }

        resetPendingDismisses();

    }

    public static void enableDisableViewGroup(ViewGroup viewGroup, boolean enabled) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = viewGroup.getChildAt(i);
            view.setEnabled(enabled);
            if (view instanceof ViewGroup) {
                enableDisableViewGroup((ViewGroup) view, enabled);
            }
        }
    }

}
