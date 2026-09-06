/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package uk.co.senab.photoview.gestures;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class FroyoGestureDetector extends CupcakeGestureDetector {

    private static final int INVALID_POINTER_ID = -1;

    private int mActivePointerId = INVALID_POINTER_ID;
    private int mActivePointerIndex;

    protected final ScaleGestureDetector mDetector;

    public FroyoGestureDetector(Context context) {
        super(context);
        ScaleGestureDetector.OnScaleGestureListener mScaleListener = new ScaleGestureDetector.OnScaleGestureListener() {

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();

                if (Float.isNaN(scaleFactor) || Float.isInfinite(scaleFactor))
                    return false;

                mListener.onScale(scaleFactor,
                        detector.getFocusX(), detector.getFocusY());
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                // NO-OP
            }
        };
        mDetector = new ScaleGestureDetector(context, mScaleListener);
    }

    @Override
    public boolean isScaling() {
        return mDetector.isInProgress();
    }

    @Override
    float getActiveX(MotionEvent event) {
        return getActiveCoordinate(event, true);
    }

    @Override
    float getActiveY(MotionEvent event) {
        return getActiveCoordinate(event, false);
    }

    private float getActiveCoordinate(MotionEvent event, boolean xAxis) {
        int pointerIndex = mActivePointerIndex;
        if (pointerIndex < 0 || pointerIndex >= event.getPointerCount()) {
            pointerIndex = 0;
        }
        return xAxis ? event.getX(pointerIndex) : event.getY(pointerIndex);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            mDetector.onTouchEvent(ev);
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mActivePointerId = ev.getPointerId(0);
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    int pointerIndex = ev.getActionIndex();
                    if (ev.getPointerId(pointerIndex) == mActivePointerId) {
                        int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                        mActivePointerId = ev.getPointerId(newPointerIndex);
                        mLastTouchX = ev.getX(newPointerIndex);
                        mLastTouchY = ev.getY(newPointerIndex);
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    mActivePointerId = INVALID_POINTER_ID;
                    break;
                default:
                    break;
            }
            mActivePointerIndex = mActivePointerId == INVALID_POINTER_ID
                    ? 0 : ev.findPointerIndex(mActivePointerId);
            return super.onTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            // A malformed final pointer event can arrive while the view is detaching.
            return true;
        }
    }
}
