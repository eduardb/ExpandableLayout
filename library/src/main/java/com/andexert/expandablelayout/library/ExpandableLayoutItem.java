/***********************************************************************************
 * The MIT License (MIT)

 * Copyright (c) 2014 Robin Chutaux

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ***********************************************************************************/
package com.andexert.expandablelayout.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.RelativeLayout;

public class ExpandableLayoutItem extends RelativeLayout
{
    private Boolean isAnimationRunning = false;
    private Boolean isOpened = false;
    private Integer duration;
    private RelativeLayout contentRelativeLayout;
    private RelativeLayout headerRelativeLayout;
    private Boolean closeByUser = true;

    public ExpandableLayoutItem(Context context)
    {
        super(context);
    }

    public ExpandableLayoutItem(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

    public ExpandableLayoutItem(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(final Context context, AttributeSet attrs)
    {
        final View rootView = View.inflate(context, R.layout.view_expandable, this);
        headerRelativeLayout = (RelativeLayout) rootView.findViewById(R.id.view_expandable_headerlayout);
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ExpandableLayout);
        final int headerID = typedArray.getResourceId(R.styleable.ExpandableLayout_headerLayout, -1);
        final int contentID = typedArray.getResourceId(R.styleable.ExpandableLayout_contentLayout, -1);
        contentRelativeLayout = (RelativeLayout) rootView.findViewById(R.id.view_expandable_contentLayout);

        if (headerID == -1 || contentID == -1)
            throw new IllegalArgumentException("HeaderLayout and ContentLayout cannot be null!");

        duration = typedArray.getInt(R.styleable.ExpandableLayout_duration, getContext().getResources().getInteger(android.R.integer.config_shortAnimTime));
        final View headerView = View.inflate(context, headerID, null);
        headerView.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        headerRelativeLayout.addView(headerView);
        setTag(ExpandableLayoutItem.class.getName());
        final View contentView = View.inflate(context, contentID, null);
        contentView.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        contentRelativeLayout.addView(contentView);
        contentRelativeLayout.setVisibility(GONE);

        headerRelativeLayout.setOnTouchListener(new OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (isOpened() && event.getAction() == MotionEvent.ACTION_UP)
                {
                    hide();
                    closeByUser = true;
                }

                return isOpened() && event.getAction() == MotionEvent.ACTION_DOWN;
            }
        });
        typedArray.recycle();
    }

    private void expand(final View v)
    {
        isOpened = true;
        v.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        final int startTopMargin = - v.getMeasuredHeight();
        final int targetTopMargin = 0;
        ((LayoutParams) v.getLayoutParams()).topMargin = startTopMargin;
        v.setVisibility(VISIBLE);

        Animation animation = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t)
            {
                if (interpolatedTime == 1)
                    isOpened = true;
                ((LayoutParams) v.getLayoutParams()).topMargin = (int) (startTopMargin
                        + (targetTopMargin - startTopMargin) * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        animation.setDuration(duration);
        v.startAnimation(animation);
    }

    private void collapse(final View v)
    {
        isOpened = false;
        final int startTopMargin = 0;
        final int targetTopMargin = - v.getMeasuredHeight();
        ((LayoutParams) v.getLayoutParams()).topMargin = startTopMargin;

        Animation animation = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if(interpolatedTime == 1)
                {
                    v.setVisibility(View.GONE);
                    isOpened = false;
                }
                else{
                    ((LayoutParams) v.getLayoutParams()).topMargin = (int) (startTopMargin
                            + (targetTopMargin - startTopMargin) * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        animation.setDuration(duration);
        v.startAnimation(animation);
    }

    public void hideNow()
    {
        ((LayoutParams) contentRelativeLayout.getLayoutParams()).topMargin = - contentRelativeLayout.getMeasuredHeight();
        contentRelativeLayout.invalidate();
        contentRelativeLayout.setVisibility(View.GONE);
        isOpened = false;
    }

    public void showNow()
    {
        if (!this.isOpened())
        {
            contentRelativeLayout.setVisibility(VISIBLE);
            this.isOpened = true;
            ((LayoutParams) contentRelativeLayout.getLayoutParams()).topMargin = 0;
            contentRelativeLayout.invalidate();
        }
    }

    public Boolean isOpened()
    {
        return isOpened;
    }

    public void show()
    {
        if (!isAnimationRunning)
        {
            expand(contentRelativeLayout);
            isAnimationRunning = true;
            new Handler().postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    isAnimationRunning = false;
                }
            }, duration);
        }
    }

    public RelativeLayout getHeaderRelativeLayout()
    {
        return headerRelativeLayout;
    }

    public RelativeLayout getContentRelativeLayout()
    {
        return contentRelativeLayout;
    }

    public void hide()
    {
        if (!isAnimationRunning)
        {
            collapse(contentRelativeLayout);
            isAnimationRunning = true;
            new Handler().postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    isAnimationRunning = false;
                }
            }, duration);
        }
        closeByUser = false;
    }

    public Boolean getCloseByUser()
    {
        return closeByUser;
    }
}
