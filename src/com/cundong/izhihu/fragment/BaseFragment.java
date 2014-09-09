package com.cundong.izhihu.fragment;

import com.cundong.izhihu.util.Logger;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

public abstract class BaseFragment extends Fragment implements OnRefreshListener {
	
	protected PullToRefreshLayout mPullToRefreshLayout;
	
	protected Logger mLogger = Logger.getLogger();
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		ActionBarPullToRefresh.from(getActivity()).allChildrenArePullable()
				.listener(this).setup(this.mPullToRefreshLayout);
	}
	
	@Override
	public void onRefreshStarted(View view) {
		doRefresh();
	}
	
	protected void doRefresh(){
		
	}
	
	
}