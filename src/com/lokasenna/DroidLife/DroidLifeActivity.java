package com.lokasenna.DroidLife;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.lokasenna.DroidLife.DroidLifeView.DroidLifeThread;

public class DroidLifeActivity extends Activity {
    private static final int MENU_PAUSE = 1;
    private static final int MENU_RESUME = 2;
    private static final int MENU_START = 3;
    private static final int MENU_STOP = 4;

    /** A handle to the thread that's actually running the animation. */
    private DroidLifeThread thread;

    /** A handle to the View in which the game is running. */
    private DroidLifeView view;

    /**
     * Invoked during init to give the Activity a chance to set up its Menu.
     *
     * @param menu the Menu to which entries may be added
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

    	Log.d(this.getClass().getName(), "build menu");

        menu.add(0, MENU_START, 0, R.string.menu_start);
        menu.add(0, MENU_STOP, 0, R.string.menu_stop);
        menu.add(0, MENU_PAUSE, 0, R.string.menu_pause);
        menu.add(0, MENU_RESUME, 0, R.string.menu_resume);

        return true;
    }
    
    /**
     * Invoked when the user selects an item from the Menu.
     *
     * @param item the Menu entry which was selected
     * @return true if the Menu item was legit (and we consumed it), false
     *         otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_START:
                thread.doStart();
                return true;
            case MENU_STOP:
                thread.setState(DroidLifeThread.STATE_LOSE,
                        getText(R.string.message_stopped));
                return true;
            case MENU_PAUSE:
                thread.pause();
                return true;
            case MENU_RESUME:
                thread.unpause();
                return true;
        }

        return false;
    }
    
    /**
     * Invoked when the Activity is created.
     *
     * @param savedInstanceState a Bundle containing state saved from a previous
     *        execution, or null if this is a new execution
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(this.getClass().getName(), "starting up!");
        // tell system to use the layout defined in our XML file
        setContentView(R.layout.main);

        // get handles to the DroidLifeView from XML, and its DroidLifeThread
        view = (DroidLifeView) findViewById(R.id.life);
        thread = view.getThread();

        // give the DroidLifeView a handle to the TextView used for messages
        view.setTextView((TextView) findViewById(R.id.text));

        if (savedInstanceState == null) {
            // we were just launched: set up a new game
            thread.setState(DroidLifeThread.STATE_READY);
            Log.w(this.getClass().getName(), "SIS is null");
        } else {
            // we are being restored: resume a previous game
            thread.restoreState(savedInstanceState);
            Log.w(this.getClass().getName(), "SIS is nonnull");
        }
    }

    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        super.onPause();
        view.getThread().pause(); // pause game when Activity pauses
    }

    /**
     * Notification that something is about to happen, to give the Activity a
     * chance to save state.
     *
     * @param outState a Bundle into which this Activity should save its state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
        thread.saveState(outState);
        Log.w(this.getClass().getName(), "SIS called");
    }    
}