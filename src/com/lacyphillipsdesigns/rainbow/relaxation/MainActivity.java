package com.lacyphillipsdesigns.rainbow.relaxation;

import com.lacyphillipsdesigns.rainbow.relaxation.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

public class MainActivity extends Activity {

	private static final String LOGTAG = "Main" ;
	protected Handler handler ;
	protected Panel myPanel ;
	protected boolean dialogUp = false ;
	protected SharedPreferences prefs ;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(LOGTAG, "Entered activity.") ;
		myPanel = new Panel(this) ;
		setContentView(myPanel) ;

		prefs = getSharedPreferences("rainbow", 0) ;
		myPanel.setSteps(prefs.getInt("steps", 100)) ;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		showDialog(0) ;
		return super.onTouchEvent(event);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Log.d(LOGTAG, "Showing Dialog") ;
		if (!dialogUp) {
			dialogUp = true ;
			Context mContext = MainActivity.this;
			final Dialog speedDialog = new Dialog(mContext);
			speedDialog.setContentView(R.layout.speed_dialog);
			speedDialog.setTitle(R.string.speed_dialog_title);
			SeekBar speedBar = (SeekBar) speedDialog.findViewById(R.id.speedSelection);
			speedBar.setProgress(myPanel.getDelay());
			speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					prefs.edit().putInt("speed", progress).commit() ;
					myPanel.setDelay(progress);
				}
			});

			SeekBar stepBar = (SeekBar) speedDialog.findViewById(R.id.stepSetting) ;
			stepBar.setProgress(myPanel.getSteps()) ;
			stepBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					prefs.edit().putInt("steps", progress).commit() ;
					myPanel.setSteps(progress) ;
				}
			}) ;

			Button closeButton = (Button) speedDialog.findViewById(R.id.button1);
			closeButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					dialogUp = false ;
					speedDialog.dismiss();
				}
			});
			speedDialog.show();
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.finish() ;
	}

	private class Panel extends SurfaceView implements SurfaceHolder.Callback {

		protected ColorChangeThread colorChanger ;
		private int step = 0 ;

		private int center = 128 ;
		private int width = 127 ;
		private int steps = 8000 ;
		private float freq = 0.1F ;

		public Panel(Context context) {
			super(context);
			Log.d(LOGTAG, "Panel instantiated.") ;
			getHolder().addCallback(this) ;
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			colorChanger = new ColorChangeThread(holder, this) ;
			Log.d(LOGTAG, "Starting color changer thread.") ;
			colorChanger.start() ;
		}

		public int getSteps() {
			int value = Math.round((steps-4000)/40) ;
			Log.d(LOGTAG, "Get Steps: "+value) ;
			return value ;
		}

		public void setSteps(int steps) {
			this.steps = (steps*40)+4000 ;
			freq = (314.159F*2)/this.steps ;
			Log.d(LOGTAG, "Steps: "+this.steps) ;
		}

		public void setDelay(int delay) {
			colorChanger.setDelay(delay) ;
		}

		public int getDelay() {
			return colorChanger.getDelay() ;
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			colorChanger.end() ;
		}

		@Override
		protected void onDraw(Canvas canvas) {
			int red = Math.round(FloatMath.sin(freq*step + 0) * width + center) ;
			int green = Math.round(FloatMath.sin(freq*step + 2) * width + center) ;
			int blue = Math.round(FloatMath.sin(freq*step + 4) * width + center) ;
			step++ ;
			if (step>=steps) {
				step = 0 ;
			}
			try {
				canvas.drawARGB(255, red, green, blue) ;
			} catch (NullPointerException e) {
				Log.i(LOGTAG, "Canvas was NULL.") ;
			}
		}
		
	}

	protected class ColorChangeThread extends Thread {

	    private SurfaceHolder surfaceHolder;
	    private Panel panel;
	    private boolean run = true ;
	    private int delay = 100 ;
	    private boolean isRunning = false ;

	    public ColorChangeThread(SurfaceHolder holder, Panel panel) {
	    	super() ;
	    	this.surfaceHolder = holder ;
	    	this.panel = panel ;
		}

	    public void setDelay(int delay) {
	    	this.delay = delay ;
	    }

	    public int getDelay() {
	    	return this.delay ;
	    }
		
		@Override
		public void run() {
			isRunning = true ;
			Canvas c ;
			if (prefs.contains("speed")) {
				myPanel.setDelay(prefs.getInt("speed", 100)) ;
			}
			while (run) {
				c = null ;
				try {
					c = surfaceHolder.lockCanvas(null) ;
					synchronized (surfaceHolder) {
						panel.onDraw(c) ;
					}
				} finally {
					if (c != null) {
						surfaceHolder.unlockCanvasAndPost(c) ;
					}
				}

				try {
					int delayTime = Math.round(100-(delay)) ;
					Thread.sleep(delayTime) ;
				} catch (InterruptedException e) {
					Log.w(LOGTAG, "Got an interruption in the update thread: "+e.getLocalizedMessage()) ;
				}
			}
			isRunning = false ;
		}

		public boolean isRunning() {
			return isRunning ;
		}

		public void pause() {
			this.run = false ;
		}

		public void enable() {
			this.run = true ;
		}

		public void end() {
			this.run = false ;
		}
	}
}
