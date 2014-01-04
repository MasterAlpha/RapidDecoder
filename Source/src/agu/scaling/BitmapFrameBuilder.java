package agu.scaling;

import static agu.caching.ResourcePool.PAINT;
import static agu.caching.ResourcePool.RECT;
import agu.bitmap.Decoder;
import agu.bitmap.SimulatedDecoder;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;

public class BitmapFrameBuilder {
	private Decoder decoder;
	private int frameWidth;
	private int frameHeight;
	private ScaleAlignment align = ScaleAlignment.CENTER;
	private boolean onlyWhenOverflowed = false;
	private Drawable background;
	
	public BitmapFrameBuilder(Bitmap bitmap, int frameWidth, int frameHeight) {
		this(new SimulatedDecoder(bitmap), frameWidth, frameHeight);
	}

	public BitmapFrameBuilder(Decoder decoder, int frameWidth, int frameHeight) {
		this.decoder = decoder;
		this.frameWidth = frameWidth;
		this.frameHeight = frameHeight;
	}
	
	public BitmapFrameBuilder scaleAlignment(ScaleAlignment align) {
		this.align = align;
		return this;
	}
	
	public BitmapFrameBuilder scaleOnlyWhenOverflowed(boolean on) {
		this.onlyWhenOverflowed = on;
		return this;
	}
	
	public BitmapFrameBuilder background(Drawable d) {
		this.background = d;
		return this;
	}
	
	public Bitmap fitIn() {
		return scale(true);
	}
	
	public Bitmap cutOut() {
		return scale(false);
	}
	
	private Bitmap scale(boolean fitIn) {
		final int width = decoder.sourceWidth();
		final int height = decoder.sourceHeight();
		
		if (width == frameWidth && height == frameHeight) {
			return decoder.decode();
		}
		
		final Rect bounds = RECT.obtain(false);
		try {
			if (onlyWhenOverflowed && 
					((!fitIn && (width <= frameWidth || height <= frameHeight)) ||
					 (fitIn && (width <= frameWidth && height <= frameHeight)))) {
				
				// If image is smaller than frame
				
				final boolean vert = (fitIn && width > height) ||
						(!fitIn && height > width);
				
				if (vert) {
					bounds.left = (frameWidth - width) / 2;
					bounds.right = bounds.left + width;
					
					if (align == ScaleAlignment.LEFT_OR_TOP) {
						bounds.top = 0;
					} else if (align == ScaleAlignment.RIGHT_OR_BOTTOM) {
						bounds.top = frameHeight - height;
					} else {
						bounds.top = (frameHeight - height) / 2;
					}
					
					bounds.bottom = bounds.top + height;
				} else {
					bounds.top = (frameHeight - height) / 2;
					bounds.bottom = bounds.top + height;
					
					if (align == ScaleAlignment.LEFT_OR_TOP) {
						bounds.left = 0;
					} else if (align == ScaleAlignment.RIGHT_OR_BOTTOM) {
						bounds.left = frameWidth - width;
					} else {
						bounds.left = (frameWidth - width) / 2;
					}
					
					bounds.right = bounds.left + width;
				}
				
				final Bitmap bitmap = decoder.scale(bounds.width(), bounds.height(), true).decode();
				
				final Bitmap bitmap2 = Bitmap.createBitmap(frameWidth, frameHeight, bitmap.getConfig());
				final Canvas cv = new Canvas(bitmap2);
				
				if (background != null) {
					cv.save(Canvas.CLIP_SAVE_FLAG);
					cv.clipRect(bounds, Op.DIFFERENCE);
					
					background.setBounds(0, 0, frameWidth, frameHeight);
					background.draw(cv);
					
					cv.restore();
				}
				
				final Paint p = PAINT.obtain();
				try {
					p.setFilterBitmap(true);
					cv.drawBitmap(bitmap, bounds.left, bounds.top, p);
				} finally {
					PAINT.recycle(p);
				}
				
				return bitmap2;
			} else {
				AspectRatioCalculator.frame(width, height,
						frameWidth, frameHeight, align, fitIn, bounds);
				
				final int w = bounds.width();
				final int h = bounds.height();
				
				if (frameWidth == w && frameHeight == h) {
					return decoder.scale(frameWidth, frameHeight, true).decode();
				} else {
					final Bitmap bitmap = decoder.scale(w, h, true).decode();
					
					final Bitmap bitmap2 = Bitmap.createBitmap(frameWidth, frameHeight, bitmap.getConfig());
					final Canvas canvas = new Canvas(bitmap2);
					
					if (background != null) {
						canvas.save(Canvas.CLIP_SAVE_FLAG);
						canvas.clipRect(bounds, Op.DIFFERENCE);
						
						background.setBounds(0, 0, frameWidth, frameHeight);
						background.draw(canvas);
						
						canvas.restore();
					}
					
					final Paint p = PAINT.obtain();
					try {
						p.setFilterBitmap(true);
						canvas.drawBitmap(bitmap, bounds.left, bounds.top, p);
					} finally {
						PAINT.recycle(p);
					}
	
					return bitmap2;
				}
			}
		} finally {
			RECT.recycle(bounds);
		}
	}
}