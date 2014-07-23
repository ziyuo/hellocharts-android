package lecho.lib.hellocharts;

import lecho.lib.hellocharts.util.Utils;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

public class ChartCalculator {
	// TODO: use getters/setters instead of public members
	public static final int DEFAULT_COMMON_MARGIN_DP = 4;
	private Context mContext;
	private Chart mChart;
	private int margin;
	/**
	 * The current area (in pixels) for chart data, including mCoomonMargin. Labels are drawn outside this area.
	 */
	public Rect mContentRect = new Rect();
	public Rect mContentRectWithMargins = new Rect();
	/**
	 * This rectangle represents the currently visible chart values ranges. The currently visible chart X values are
	 * from this rectangle's left to its right. The currently visible chart Y values are from this rectangle's top to
	 * its bottom.
	 * <p>
	 * Note that this rectangle's top is actually the smaller Y value, and its bottom is the larger Y value. Since the
	 * chart is drawn onscreen in such a way that chart Y values increase towards the top of the screen (decreasing
	 * pixel Y positions), this rectangle's "top" is drawn above this rectangle's "bottom" value.
	 * 
	 */
	public RectF mCurrentViewport = new RectF();
	public RectF mMaximumViewport = new RectF();// Viewport for whole data ranges

	/**
	 * Constructor
	 */
	public ChartCalculator(Context context, Chart chart) {
		mContext = context;
		mChart = chart;
		margin = Utils.dp2px(mContext, DEFAULT_COMMON_MARGIN_DP);
	}

	/**
	 * Calculates available width and height. Should be called when chart dimensions or chart data change.
	 */
	public void calculateContentArea(int width, int height, int paddingLeft, int paddingTop, int paddingRight,
			int paddingBottom) {
		mContentRectWithMargins.set(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom);
		mContentRect.set(mContentRectWithMargins.left + margin, mContentRectWithMargins.top + margin,
				mContentRectWithMargins.right - margin, mContentRectWithMargins.bottom - margin);
	}

	public void setInternalMargin(int margin) {
		mContentRect.left = mContentRectWithMargins.left + margin;
		mContentRect.top = mContentRectWithMargins.top + margin;
		mContentRect.right = mContentRectWithMargins.right - margin;
		mContentRect.bottom = mContentRectWithMargins.bottom - margin;
	}

	public void setInternalMargin(int marginLeft, int marginTop, int marginRight, int marginBottom) {
		mContentRect.left = mContentRectWithMargins.left + marginLeft;
		mContentRect.top = mContentRectWithMargins.top + marginTop;
		mContentRect.right = mContentRectWithMargins.right - marginRight;
		mContentRect.bottom = mContentRectWithMargins.bottom - marginBottom;
	}

	public void setAxesMargin(int axisXMargin, int axisYMargin) {
		mContentRectWithMargins.bottom = mContentRectWithMargins.bottom - axisXMargin;
		mContentRectWithMargins.left = mContentRectWithMargins.left + axisYMargin;
		mContentRect.left = mContentRect.left + axisYMargin;
		mContentRect.bottom = mContentRect.bottom - axisXMargin;
	}

	public void calculateViewport() {
		final RectF boundaries = mChart.getData().getBoundaries();
		mMaximumViewport.set(boundaries.left, boundaries.bottom, boundaries.right, boundaries.top);
		// TODO: don't reset current viewport during animation if zoom is enabled
		mCurrentViewport.set(mMaximumViewport);
	}

	public void constrainViewport() {
		// TODO: avoid too much zoom
		mCurrentViewport.left = Math.max(mMaximumViewport.left, mCurrentViewport.left);
		mCurrentViewport.top = Math.max(mMaximumViewport.top, mCurrentViewport.top);
		mCurrentViewport.bottom = Math.max(Utils.nextUpF(mCurrentViewport.top),
				Math.min(mMaximumViewport.bottom, mCurrentViewport.bottom));
		mCurrentViewport.right = Math.max(Utils.nextUpF(mCurrentViewport.left),
				Math.min(mMaximumViewport.right, mCurrentViewport.right));
	}

	/**
	 * Sets the current viewport (defined by {@link #mCurrentViewport}) to the given X and Y positions. Note that the Y
	 * value represents the topmost pixel position, and thus the bottom of the {@link #mCurrentViewport} rectangle. For
	 * more details on why top and bottom are flipped, see {@link #mCurrentViewport}.
	 */
	public void setViewportBottomLeft(float x, float y) {
		/**
		 * Constrains within the scroll range. The scroll range is simply the viewport extremes (AXIS_X_MAX, etc.) minus
		 * the viewport size. For example, if the extrema were 0 and 10, and the viewport size was 2, the scroll range
		 * would be 0 to 8.
		 */

		final float curWidth = mCurrentViewport.width();
		final float curHeight = mCurrentViewport.height();
		x = Math.max(mMaximumViewport.left, Math.min(x, mMaximumViewport.right - curWidth));
		y = Math.max(mMaximumViewport.top + curHeight, Math.min(y, mMaximumViewport.bottom));
		mCurrentViewport.set(x, y - curHeight, x + curWidth, y);
	}

	public float calculateRawX(float valueX) {
		final float pixelOffset = (valueX - mCurrentViewport.left) * (mContentRect.width() / mCurrentViewport.width());
		return mContentRect.left + pixelOffset;
	}

	public float calculateRawY(float valueY) {
		final float pixelOffset = (valueY - mCurrentViewport.top) * (mContentRect.height() / mCurrentViewport.height());
		return mContentRect.bottom - pixelOffset;
	}

	/**
	 * Finds the chart point (i.e. within the chart's domain and range) represented by the given pixel coordinates, if
	 * that pixel is within the chart region described by {@link #mContentRect}. If the point is found, the "dest"
	 * argument is set to the point and this function returns true. Otherwise, this function returns false and "dest" is
	 * unchanged.
	 */
	public boolean rawPixelsToDataPoint(float x, float y, PointF dest) {
		if (!mContentRect.contains((int) x, (int) y)) {
			return false;
		}
		dest.set(mCurrentViewport.left + (x - mContentRect.left) * mCurrentViewport.width() / mContentRect.width(),
				mCurrentViewport.top + (y - mContentRect.bottom) * mCurrentViewport.height() / -mContentRect.height());
		return true;
	}

	/**
	 * Computes the current scrollable surface size, in pixels. For example, if the entire chart area is visible, this
	 * is simply the current size of {@link #mContentRect}. If the chart is zoomed in 200% in both directions, the
	 * returned size will be twice as large horizontally and vertically.
	 */
	public void computeScrollSurfaceSize(Point out) {
		out.set((int) (mMaximumViewport.width() * mContentRect.width() / mCurrentViewport.width()),
				(int) (mMaximumViewport.height() * mContentRect.height() / mCurrentViewport.height()));
	}

	public boolean isWithinContentRect(int x, int y) {
		if (x >= mContentRect.left && x <= mContentRect.right) {
			if (y >= mContentRect.top && y <= mContentRect.bottom) {
				return true;
			}
		}
		return false;
	}

}
