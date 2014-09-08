package com.cundong.izhihu.task;

import android.text.TextUtils;

import com.cundong.izhihu.ZhihuApplication;

public class SaveNewsListTask extends MyAsyncTask<Void, Void, Void>{

	private String mDate;
    private String mContent;

    public SaveNewsListTask(String date, String content) {
        this.mDate = date;
        this.mContent = content;
    }
    
	@Override
	protected Void doInBackground(Void... params) {
		
		saveNewsList(mDate, mContent);
		
		return null;
	}
	
	private void saveNewsList(String date, String content) {

        ZhihuApplication.getDataSource().insertOrUpdateNewsList(date, content);
    }
}