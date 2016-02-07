package edu.rice.rubbos.db;

import edu.rice.rubbos.servlets.ServletPrinter;

public class DbProcessResult {
	 public ServletPrinter   sp;
	 public Boolean exceptionOccured;
	
	 public DbProcessResult(ServletPrinter sp, Boolean exceptionOccured) {
		super();
		this.sp = sp;
		this.exceptionOccured = exceptionOccured;
	}
	 
	 

}
