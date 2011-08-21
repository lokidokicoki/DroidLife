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
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import com.lokasenna.DroidLife.Matrix;
public class DroidLifeView extends SurfaceView implements SurfaceHolder.Callback {
	class DroidLifeThread extends Thread {
		/*
         * State-tracking constants
         */
        public static final int STATE_PAUSE = 1;
        public static final int STATE_READY = 2;
        public static final int STATE_RUNNING = 3;
        public static final int STATE_LOSE = 4;
        public static final int STATE_STEADY = 5;
        
        
        private static final String KEY_CHANCE = "chance";
        
        private java.util.Random random;

        /*
         * Member (state) fields
         */
        /** The drawable to use as the background of the animation canvas */
        private Bitmap bg_image;
        
        /** Message handler used by thread to interact with TextView */
        private Handler handler;

        /** Indicate whether the surface has been created & is ready to draw */
        private boolean running = false;
        private boolean grid_online = false;
        
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
        private int grid_height = 1;

        /**
         * Current width of the surface/canvas.
         *
         * @see #setSurfaceSize
         */
        private int grid_width = 1;
        
        private Paint paint;
        private Paint dead_paint;
        private RectF rect;
        
    	private int survive = (int)(Math.pow(2, 2) + Math.pow(2,3));
    	private int birth = (int)(Math.pow(2,3));
        private int rand_factor = 10;

        /**
         * Holds the image data.
         * linear array of points, i.e [x0, y0, x1, y1... xN, yN]
         */
        float[] points=null;
        
        private Matrix grid=null;
        private Matrix new_grid=null;
        
        
        
        
        
		public DroidLifeThread(SurfaceHolder sh, Context ctx,
                Handler h) {
        	//Log.d("DLT:ctor", "ctor");
			
			// get handles to some important objects
            surface_holder = sh;
            handler = h;
            context = ctx;
            paint=new Paint();
            dead_paint=new Paint();
            paint.setARGB(255,255,0,0);
            dead_paint.setARGB(255,0,0,0);
            rect = new RectF(0,0,0,0);
            bg_image=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);
            random = new java.util.Random();
            
		}
		
			/**
	         * Starts the game, setting parameters for the current difficulty.
	         */
	        public void doStart() {
	        	//Log.d("DLT:dostart", "dostart");
	            synchronized (surface_holder) {
	                last_time = System.currentTimeMillis() + 100;
	                setState(STATE_RUNNING);
	            }
	        }
			/**
	         * Pauses the physics update & animation.
	         */
	        public void pause() {
	        	//Log.d("DLT:pause", "pause");
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
	        	//Log.d("DLT:restorestate", "restorestate");
	            synchronized (surface_holder) {
	                setState(STATE_PAUSE);

	                //chance = savedState.getInt(KEY_CHANCE);
	            }
	        }

	        @Override
	        public void run() {
	        	//Log.d("DLT:run", "run");
	            while (running) {
	                Canvas c = null;
	                try {
	                    c = surface_holder.lockCanvas(null);
	                    synchronized (surface_holder) {
	                    	if (mode == STATE_READY) init_world();
	                        //Log.d("DLT:run", "call doDraw");
	                        doDraw(c);
	                        if (mode == STATE_RUNNING) update_world();
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
	        	//Log.d("DLT:savestate", "saveState");
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
	        	//Log.d("DLT:setRunning", "b:"+b);
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
	        	//Log.d("DLT:setState", "mode:"+mode);
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
	        	//Log.d("DLT:setState", "m:"+m);
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
	    	        	//Log.d("DLT:setState", "state:running");
	                    Message msg = handler.obtainMessage();
	                    Bundle b = new Bundle();
	                    b.putString("text", "");
	                    b.putInt("viz", View.INVISIBLE);
	                    msg.setData(b);
	                    handler.sendMessage(msg);
	                } else {
	                	if (context == null)
		    	        	Log.d("DLT:setState", "context is null");

	                	try{
		    	        	//Log.d("DLT:setState", "get resources");
		                    Resources res = context.getResources();
		                    CharSequence str = "";
		    	        	//Log.d("DLT:setState", "resources got");
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
		    	        	//Log.d("DLT:setState", "msg:"+str);
	                	}catch(Resources.NotFoundException e){
	                		Log.e("DLT:setState", e.toString());
	                	}
	                	

	                }
	            }
	        }

			/* Callback invoked when the surface dimensions change. */
	        public void setSurfaceSize(int width, int height) {
	        	//Log.d("DLT:setSurfaceSize", "w:"+width+", h:"+height);
	            // synchronized to make sure these all change atomically
	            synchronized (surface_holder) {
	                grid_width = width/2;
	                grid_height = height/2;

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
	            
	            if (mode == STATE_RUNNING){
	            	for (int x=0; x < grid_width; x++){
		            	for(int y=0; y< grid_height; y++){
		            		try{
		            			int cell_state = grid.get(x, y);
		            			//Log.d("DLT:doDraw", "point x:"+x+","+y+", state:"+cell_state);
			            		if(cell_state == 1){
			            			canvas.drawPoint(x, y, paint);
			            			//canvas.drawPoint(x, y+1, paint);
			            			//canvas.drawPoint(x+1, y, paint);
			            			//canvas.drawPoint(x+1, y+1, paint);
			            			//Log.d("DLT:doDraw", "point x:"+x+","+y);
			            			
			            		}
			            		else{
			            			canvas.drawPoint(x,y,dead_paint);
			            		}
		            		}catch(Matrix.BoundsViolation bv){
		            			//Log.e("DLT:doDraw", bv.getError());
		            		}
		            	}
		            }
	            }//else{
		            //rect.set(10,10,50,50);
	            	//canvas.drawRect(rect, paint);
	            //}
	            
	            //canvas.restore();
	        }
	        
	        private void init_world(){
	        	if (grid_online != true){
		        	try {
		        		//Log.d("DLT:init_world","init_grid");
		        		grid = new Matrix(grid_width, grid_height);
		        		new_grid = new Matrix(grid_width, grid_height);
		        		
		        		for(int x=0; x < grid_width; x++){
		        			for(int y=0; y < grid_height; y++){
		        				grid.set(x, y, new Integer(0));
		        				new_grid.set(x, y, new Integer(0));
		        			}
		        		}
		        		
		        		//init_rule();
		        		for(int x=0; x < grid_width; x++){
		        			for(int y=0; y < grid_height; y++){
		        				int num = (int)((random.nextInt(100)%rand_factor)+1);//(int) ((Math.random() % rand_factor)+1);
		        				//Log.d("DLT:init_world","num:"+num);
		        				if (num == rand_factor){
		    		        		//Log.d("DLT:init_world","set point x:"+x+", y:"+y);
		        					grid.set(x, y, new Integer(1));
		        				}
		        			}
		        		}
		        		Log.d("DLT:init_world","grids built");
		        		
		        		grid_online=true;
		        	} catch(Matrix.BoundsViolation bv){
		        		//Log.e("DLT:init_world", "bound violation:"+bv.getError());
		        	}
	        	}
	        }

	        private int check_cell(int x, int y){
	        	int count=0;
	        	try{
	        		int cell_value = grid.get(x,y);
	        		//Log.d("DLT:check_cell", "x:"+x+", y:"+y+", state:"+cell_value);
	        		if (cell_value != 0){// && cell_value != 4){
	        			count=1;
	        		}
	        	
	        	}catch(Matrix.BoundsViolation bv){
	        		//Log.e("DLT:check_cell", bv.getError());
	        	}
	        	return count;
	        }
	        
	        private int check_rule(int cell_state, int count){
	        	int result=0; 
	        	//Log.d("DLT:check_rule","state:"+cell_state+", survive:"+survive+", birth:"+birth+", count:"+count+" <C:"+pow(2,count));
	            if(cell_state != 0){
	              //  if((survive & ((int)pow(2,count))) == 1){
	            	if(count == 2 || count == 3){
	    	        	//Log.d("DLT:check_rule","do_survive");
	                    result=1;
	                }
	            }else{
	                //if((birth & ((int)pow(2,count))) == 1){
	            	if(count == 3){
	    	        	//Log.d("DLT:check_rule","do_birth");
	                    result=1;
	                }
	            }
	            return result;   
	        }

	        private int pow(int i, int count) {
				// TODO Auto-generated method stub
	        	double result = Math.pow((double)i, (double)count);
				return (int)result;
			}

			private void update_world() {
	            //long now = System.currentTimeMillis();
	        	
	            
	            //make new grid based on old grid
        		for(int x=0; x < grid_width; x++){
        			for(int y=0; y < grid_height; y++){
        				int count=0;
        				count += check_cell(x-1,y-1);
        				count += check_cell(x,y-1);
        				count += check_cell(x+1,y-1);
        				count += check_cell(x-1,y);
        				count += check_cell(x+1,y);
        				count += check_cell(x-1,y+1);
        				count += check_cell(x,y+1);
        				count += check_cell(x+1,y+1);
        				
    					//Log.d("DLT:update_world", "x:"+x+" y:"+y+" count:"+count);
        				
        				try{
        					new_grid.set(x,y, check_rule(grid.get(x,y), count));
        					//Log.d("DLT:update_world", "new_grid x:"+x+" y:"+y+" state:"+new_grid.get(x, y));
        					
        				}catch(Matrix.BoundsViolation bv){
        					//Log.e("DLT:update_world", bv.getError());
        				}
        			}
        			
        		}

				Log.d("DLT:update_world","fix grid");
        		//make new grid based on old grid
        		for(int x=0; x < grid_width; x++){
        			for(int y=0; y < grid_height; y++){
        				try{
        					grid.set(x, y, new_grid.get(x, y));
        				}catch(Matrix.BoundsViolation bv){
        					//Log.e("DLT:update_world", bv.getError());
        				}
        			}
        		}
        		
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
	    	//Log.d("DLV:ctor", "ctor");

	        // register our interest in hearing about changes to our surface
	        SurfaceHolder holder = getHolder();
	        holder.addCallback(this);
	        
	        // create thread only; it's started in surfaceCreated()
	        thread = new DroidLifeThread(holder, ctx, new Handler() {
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
	    	//Log.d("DLV:surfaceChanged", "w:"+width+", h:"+height);
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
	            	//Log.e("loki", "failed to rejoin thread");
	            }
	        }
	    }
	}

