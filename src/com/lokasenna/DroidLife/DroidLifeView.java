package com.lokasenna.DroidLife;

import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

public class DroidLifeView extends SurfaceView implements SurfaceHolder.Callback {
	class DroidLifeThread extends Thread {
		/*
         * State-tracking constants
         */
        public static final int STATE_PAUSE = 1;
        public static final int STATE_READY = 2;
        public static final int STATE_RUNNING = 3;
        public static final int STATE_LOSE = 4;
        
        private static final String KEY_CHANCE = "chance";

        /*
         * Member (state) fields
         */
        /** The drawable to use as the background of the animation canvas */
        private Bitmap bg_image;
        
        /** Message handler used by thread to interact with TextView */
        private Handler handler;

        /** Indicate whether the surface has been created & is ready to draw */
        private boolean running = false;
        
        /** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
        private int mode;
        
        /** Used to figure out elapsed time between frames */
        private long last_time;

        /** Handle to the surface manager object we interact with */
        private SurfaceHolder surface_holder;        
        
        private int chance = 20;
        
        /**
         * Current height of the surface/canvas.
         *
         * @see #setSurfaceSize
         */
        private int canvas_height = 1;

        /**
         * Current width of the surface/canvas.
         *
         * @see #setSurfaceSize
         */
        private int canvas_width = 1;
        
		public DroidLifeThread(SurfaceHolder sh, Context ctx,
                Handler h) {
			// get handles to some important objects
            surface_holder = sh;
            handler = h;
            context = ctx;
		}
		
			/**
	         * Starts the game, setting parameters for the current difficulty.
	         */
	        public void doStart() {
	            synchronized (surface_holder) {
	            	Math.random();


	                last_time = System.currentTimeMillis() + 100;
	                setState(STATE_RUNNING);
	            }
	        }
			/**
	         * Pauses the physics update & animation.
	         */
	        public void pause() {
	            synchronized (surface_holder) {
	                if (mode == STATE_RUNNING) setState(STATE_PAUSE);
	            }
	        }

	        /**
	         * Restores game state from the indicated Bundle. Typically called when
	         * the Activity is being restored after having been previously
	         * destroyed.
	         *
	         * @param savedState Bundle containing the game state
	         */
	        public synchronized void restoreState(Bundle savedState) {
	            synchronized (surface_holder) {
	                setState(STATE_PAUSE);

	                //chance = savedState.getInt(KEY_CHANCE);
	            }
	        }

	        @Override
	        public void run() {
	            while (running) {
	                Canvas c = null;
	                try {
	                    c = surface_holder.lockCanvas(null);
	                    synchronized (surface_holder) {
	                        if (mode == STATE_RUNNING) update_world();
	                        doDraw(c);
	                    }
	                } finally {
	                    // do this in a finally so that if an exception is thrown
	                    // during the above, we don't leave the Surface in an
	                    // inconsistent state
	                    if (c != null) {
	                        surface_holder.unlockCanvasAndPost(c);
	                    }
	                }
	            }
	        }
			/**
	         * Dump game state to the provided Bundle. Typically called when the
	         * Activity is being suspended.
	         *
	         * @return Bundle with this view's state
	         */
	        public Bundle saveState(Bundle map) {
	            synchronized (surface_holder) {
	                if (map != null) {
	                    map.putInt(KEY_CHANCE, Integer.valueOf(chance));
	                }
	            }
	            return map;
	        }
			
			/**
	         * Used to signal the thread whether it should be running or not.
	         * Passing true allows the thread to run; passing false will shut it
	         * down if it's already running. Calling start() after this was most
	         * recently called with false will result in an immediate shutdown.
	         *
	         * @param b true to run, false to shut down
	         */
	        public void setRunning(boolean b) {
	            running = b;
	        }

	        /**
	         * Sets the game mode. That is, whether we are running, paused, in the
	         * failure state, in the victory state, etc.
	         *
	         * @see #setState(int, CharSequence)
	         * @param mode one of the STATE_* constants
	         */
	        public void setState(int mode) {
	            synchronized (surface_holder) {
	                setState(mode, null);
	            }
	        }

	        /**
	         * Sets the game mode. That is, whether we are running, paused, in the
	         * failure state, in the victory state, etc.
	         *
	         * @param mode one of the STATE_* constants
	         * @param message string to add to screen or null
	         */
	        public void setState(int m, CharSequence message) {
	            /*
	             * This method optionally can cause a text message to be displayed
	             * to the user when the mode changes. Since the View that actually
	             * renders that text is part of the main View hierarchy and not
	             * owned by this thread, we can't touch the state of that View.
	             * Instead we use a Message + Handler to relay commands to the main
	             * thread, which updates the user-text View.
	             */
	            synchronized (surface_holder) {
	                mode = m;

	                if (mode == STATE_RUNNING) {
	                    Message msg = handler.obtainMessage();
	                    Bundle b = new Bundle();
	                    b.putString("text", "");
	                    b.putInt("viz", View.INVISIBLE);
	                    msg.setData(b);
	                    handler.sendMessage(msg);
	                } else {
	                    Resources res = context.getResources();
	                    CharSequence str = "";
	                    if (mode == STATE_READY)
	                        str = res.getText(R.string.mode_ready);
	                    else if (mode == STATE_PAUSE)
	                        str = res.getText(R.string.mode_pause);

	                    if (message != null) {
	                        str = message + "\n" + str;
	                    }

	                    Message msg = handler.obtainMessage();
	                    Bundle b = new Bundle();
	                    b.putString("text", str.toString());
	                    b.putInt("viz", View.VISIBLE);
	                    msg.setData(b);
	                    handler.sendMessage(msg);
	                }
	            }
	        }

			/* Callback invoked when the surface dimensions change. */
	        public void setSurfaceSize(int width, int height) {
	            // synchronized to make sure these all change atomically
	            synchronized (surface_holder) {
	                canvas_width = width;
	                canvas_height = height;

	                // don't forget to resize the background image
	                bg_image = Bitmap.createScaledBitmap(
	                        bg_image, width, height, true);
	            }
	        }

	        /**
	         * Draws the ship, fuel/speed bars, and background to the provided
	         * Canvas.
	         */
	        private void doDraw(Canvas canvas) {
	            // Draw the background image. Operations on the Canvas accumulate
	            // so this is like clearing the screen.
	            canvas.drawBitmap(bg_image, 0, 0, null);
/*
	            int yTop = mCanvasHeight - ((int) mY + mLanderHeight / 2);
	            int xLeft = (int) mX - mLanderWidth / 2;

	            // Draw the fuel gauge
	            int fuelWidth = (int) (UI_BAR * mFuel / PHYS_FUEL_MAX);
	            mScratchRect.set(4, 4, 4 + fuelWidth, 4 + UI_BAR_HEIGHT);
	            canvas.drawRect(mScratchRect, mLinePaint);

	            // Draw the speed gauge, with a two-tone effect
	            double speed = Math.sqrt(mDX * mDX + mDY * mDY);
	            int speedWidth = (int) (UI_BAR * speed / PHYS_SPEED_MAX);

	            if (speed <= mGoalSpeed) {
	                mScratchRect.set(4 + UI_BAR + 4, 4,
	                        4 + UI_BAR + 4 + speedWidth, 4 + UI_BAR_HEIGHT);
	                canvas.drawRect(mScratchRect, mLinePaint);
	            } else {
	                // Draw the bad color in back, with the good color in front of
	                // it
	                mScratchRect.set(4 + UI_BAR + 4, 4,
	                        4 + UI_BAR + 4 + speedWidth, 4 + UI_BAR_HEIGHT);
	                canvas.drawRect(mScratchRect, mLinePaintBad);
	                int goalWidth = (UI_BAR * mGoalSpeed / PHYS_SPEED_MAX);
	                mScratchRect.set(4 + UI_BAR + 4, 4, 4 + UI_BAR + 4 + goalWidth,
	                        4 + UI_BAR_HEIGHT);
	                canvas.drawRect(mScratchRect, mLinePaint);
	            }

	            // Draw the landing pad
	            canvas.drawLine(mGoalX, 1 + mCanvasHeight - TARGET_PAD_HEIGHT,
	                    mGoalX + mGoalWidth, 1 + mCanvasHeight - TARGET_PAD_HEIGHT,
	                    mLinePaint);


	            // Draw the ship with its current rotation
	            canvas.save();
	            canvas.rotate((float) mHeading, (float) mX, mCanvasHeight
	                    - (float) mY);
	            if (mMode == STATE_LOSE) {
	                mCrashedImage.setBounds(xLeft, yTop, xLeft + mLanderWidth, yTop
	                        + mLanderHeight);
	                mCrashedImage.draw(canvas);
	            } else if (mEngineFiring) {
	                mFiringImage.setBounds(xLeft, yTop, xLeft + mLanderWidth, yTop
	                        + mLanderHeight);
	                mFiringImage.draw(canvas);
	            } else {
	                mLanderImage.setBounds(xLeft, yTop, xLeft + mLanderWidth, yTop
	                        + mLanderHeight);
	                mLanderImage.draw(canvas);
	            }
	            canvas.restore();
	            */
	        }

	        /**
	         * Figures the lander state (x, y, fuel, ...) based on the passage of
	         * realtime. Does not invalidate(). Called at the start of draw().
	         * Detects the end-of-game and sets the UI to the next state.
	         */
	        private void update_world() {
	            long now = System.currentTimeMillis();
	        }
	        
	        /**
	         * Resumes from a pause.
	         */
	        public void unpause() {
	            // Move the real time clock up to now
	            synchronized (surface_holder) {
	                last_time = System.currentTimeMillis() + 100;
	            }
	            setState(STATE_RUNNING);
	        }			
		}
		
	    /** Handle to the application context, used to e.g. fetch Drawables. */
	    private Context context;

	    /** Pointer to the text view to display "Paused.." etc. */
	    private TextView status_text;

	    /** The thread that actually draws the animation */
		private DroidLifeThread thread;

	    public DroidLifeView(Context ctx, AttributeSet attrs) {
	        super(ctx, attrs);
	    	Log.e(this.getClass().getName(), "spin up");

	        // register our interest in hearing about changes to our surface
	        SurfaceHolder holder = getHolder();
	        holder.addCallback(this);

	        // create thread only; it's started in surfaceCreated()
	        thread = new DroidLifeThread(holder, context, new Handler() {
	            @Override
	            public void handleMessage(Message m) {
	                status_text.setVisibility(m.getData().getInt("viz"));
	                status_text.setText(m.getData().getString("text"));
	            }
	        });

	        setFocusable(true); // make sure we get key events
	    }

	    /**
	     * Fetches the animation thread corresponding to this LunarView.
	     *
	     * @return the animation thread
	     */
	    public DroidLifeThread getThread() {
	        return thread;
	    }
	    
	    /**
	     * Standard window-focus override. Notice focus lost so we can pause on
	     * focus lost. e.g. user switches to take a call.
	     */
	    @Override
	    public void onWindowFocusChanged(boolean hasWindowFocus) {
	        if (!hasWindowFocus) thread.pause();
	    }

	    /**
	     * Installs a pointer to the text view used for messages.
	     */
	    public void setTextView(TextView textView) {
	        status_text = textView;
	    }
	    
		/* Callback invoked when the surface dimensions change. */
	    public void surfaceChanged(SurfaceHolder holder, int format, int width,
	            int height) {
	        thread.setSurfaceSize(width, height);
	    }

	    /*
	     * Callback invoked when the Surface has been created and is ready to be
	     * used.
	     */
	    public void surfaceCreated(SurfaceHolder holder) {
	        // start the thread here so that we don't busy-wait in run()
	        // waiting for the surface to be created
	        thread.setRunning(true);
	        thread.start();
	    }

	    /*
	     * Callback invoked when the Surface has been destroyed and must no longer
	     * be touched. WARNING: after this method returns, the Surface/Canvas must
	     * never be touched again!
	     */
	    public void surfaceDestroyed(SurfaceHolder holder) {
	        // we have to tell thread to shut down & wait for it to finish, or else
	        // it might touch the Surface after we return and explode
	        boolean retry = true;
	        thread.setRunning(false);
	        while (retry) {
	            try {
	                thread.join();
	                retry = false;
	            } catch (InterruptedException e) {
	            }
	        }
	    }
	}

