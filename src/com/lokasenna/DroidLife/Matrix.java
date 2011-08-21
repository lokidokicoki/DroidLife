package com.lokasenna.DroidLife;

import java.lang.Object;

public class Matrix{
	//***********************************************
	// Custom exception class that descends from Java's Exception class.
	class BoundsViolation extends Exception
	{
	  String mistake;
	//----------------------------------------------
	// Default constructor - initializes instance variable to unknown
	  public BoundsViolation()
	  {
	    super();             // call superclass constructor
	    mistake = "unknown";
	  }
	  
	//-----------------------------------------------
	// Constructor receives some kind of message that is saved in an instance variable.
	  public BoundsViolation(String err)
	  {
	    super(err);     // call super class constructor
	    mistake = err;  // save message
	  }
	  
	//------------------------------------------------  
	// public method, callable by exception catcher. It returns the error message.
	  public String getError()
	  {
	    return mistake;
	  }
	}
	
	private int ncols;
	private int nrows;
	private int[] data;
	
	public Matrix(int rows, int cols) throws BoundsViolation {
		ncols=cols;
		nrows=rows;
		if (nrows == 0 || ncols == 0){
			throw new BoundsViolation("bad ctor params");
		}
		data = new int[nrows*ncols];
	}
	
	public int get(int row, int col) throws BoundsViolation {
		if(row >= nrows || col >= ncols)
			throw new BoundsViolation("get row/col too big");
		if(row < 0 || col < 0)
			throw new BoundsViolation("get row/col less than zero");
		
		return data[row*ncols + col];
	}
	
	public void set(int row, int col, int value) throws BoundsViolation {
		if(row >= nrows || col >= ncols)
			throw new BoundsViolation("set row/col too big");
		if(row < 0 || col < 0)
			throw new BoundsViolation("set row/col less than zero");
		
		data[row*ncols + col] = value;
	}
}