package com.cundong.izhihu.task;

import android.util.Log;

public class DemoAsyncTask extends MyAsyncTask<Object, Object, Object> {

	private int mThreadID = 0;

	public DemoAsyncTask( int thredId){
		this.mThreadID = thredId;
	}
	
	@Override
	protected Object doInBackground(Object... params) {

		int counter = 0;

		while (true) {
			Log.i("@Cundong", "mThreadID:" + mThreadID + ",counter=" + counter);
			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}