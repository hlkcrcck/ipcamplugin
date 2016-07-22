package com.edimax.edilife.ipcam.page;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class LiveView extends SurfaceView implements SurfaceHolder.Callback {

	// Screen
	private int ViewWidth = 0;
	private int ViewHeight = 0;

	// Bitmap
	private Bitmap mBitmap;
	private int mBitmapWidth = 0;
	private int mBitmapHieght = 0;
	private Matrix matrix = new Matrix();

	// Portrait

	private boolean b_iSDraw = false;
	private String draw_buffer = "";

	private SurfaceHolder mHolder;
	private DrawThread mDrawThread;

	public LiveView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public LiveView(Context context) {
		super(context);
		init();
	}

	private void init() {
		mHolder = this.getHolder();
		mHolder.addCallback(this);
		/**
		 * Set surfaceView TRANSPARENT, but will overlap the others view.
		 */
		setZOrderOnTop(true);
		mHolder.setFormat(PixelFormat.TRANSPARENT);
	}

	public void setImageBitmap(Bitmap bm) {
		if (!b_iSDraw) {
			b_iSDraw = true;
			synchronized (draw_buffer) {
				if (mBitmapWidth != bm.getWidth() || mBitmapHieght != bm.getHeight()) {
					mBitmapWidth = bm.getWidth();
					mBitmapHieght = bm.getHeight();
					scaleSize();
				}

				mBitmapWidth = bm.getWidth();
				mBitmapHieght = bm.getHeight();

				mBitmap = bm;
			}
			b_iSDraw = false;
		}
	}

	private void scaleSize() {

		float scale_w = (float) ViewWidth / (float) mBitmapWidth;
		float tran_x = 0;
		float tran_y = Math.abs(((float) ViewHeight - ((float) mBitmapHieght * scale_w)) / 2f);

		matrix.reset();
		matrix.postScale(scale_w, scale_w);
		matrix.postTranslate(tran_x, tran_y);
		synchronized (draw_buffer) {
			setMatrix(matrix);
		}
	}

	public void showImage() {
		if (mDrawThread == null) {
			mDrawThread = new DrawThread(mHolder);
		}

		if (mDrawThread.getState() == Thread.State.NEW) {
			if (MainFrame.isDebug) Log.e(MainFrame.TAG, mDrawThread.getId() + " : START DRAW THREAD");
			mDrawThread.setDrawing(true);
			mDrawThread.start();
		}
	}

	public void stopShow() {
		if (mDrawThread != null) {
			if (MainFrame.isDebug) Log.e(MainFrame.TAG, mDrawThread.getId() + " : INTERRUPT DRAW THREAD");
			mDrawThread.setDrawing(false);
			mDrawThread.interrupt();
			mDrawThread = null;
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
	                        int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (ViewWidth == getWidth()) {
			return;
		}

		ViewWidth = getWidth();
		ViewHeight = getHeight();
		scaleSize();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (MainFrame.isDebug) Log.e(MainFrame.TAG, "surfaceCreated");
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (MainFrame.isDebug) Log.e(MainFrame.TAG, "surfaceChanged");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (MainFrame.isDebug) Log.e(MainFrame.TAG, "surfaceDestroyed");
	}

	@Override
	public Matrix getMatrix() {
		return matrix;
	}

	public void setMatrix(Matrix matrix) {
		this.matrix = matrix;
	}

	private class DrawThread extends Thread {
		private SurfaceHolder mHolder;
		private boolean drawing;


		public DrawThread(SurfaceHolder holder) {
			this.mHolder = holder;
		}

		@Override
		public void run() {
			Canvas canvas;
			while (drawing) {
				canvas = null;
				try {
					try {
						canvas = this.mHolder.lockCanvas();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
					synchronized (this.mHolder) {
						canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
						canvas.drawBitmap(mBitmap, getMatrix(), null);
					}
				} catch (NullPointerException e) {
					e.printStackTrace();
				} finally {
					if (canvas != null) {
						this.mHolder.unlockCanvasAndPost(canvas);
					}
				}
			}
		}

		public void setDrawing(boolean b) {
			drawing = b;
		}
	}
}
