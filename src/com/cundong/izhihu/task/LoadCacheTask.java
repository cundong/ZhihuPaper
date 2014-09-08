package com.cundong.izhihu.task;

import java.util.ArrayList;

import com.cundong.izhihu.ZhihuApplication;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;

public class LoadCacheNewsTask extends MyAsyncTask<String, Void, ArrayList<NewsEntity>> {

	@Override
	protected ArrayList<NewsEntity> doInBackground(String... params) {
		return ZhihuApplication.getDataSource().newsOfTheDay( params[0] );
	}

	@Override
	protected void onPostExecute(ArrayList<NewsEntity> result) {
		super.onPostExecute(result);
		
		if(result!=null&&!result.isEmpty()){
			
		}
	}
}