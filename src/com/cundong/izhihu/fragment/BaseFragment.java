package com.cundong.izhihu.fragment;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import com.cundong.izhihu.util.Logger;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

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
	
	protected void dealException(Exception e) {
		
		if (isAdded()) {
			mPullToRefreshLayout.setRefreshComplete();
		}
		
//		if ( isAdded() && e != null) {
//			// 无网络异常
//			if (e != null && e instanceof SocketException) {
//				Crouton.makeText( getActivity(), "网络连接失败", Style.ALERT).show();
//			}
//			// 网络超时
//			else if (e != null && e instanceof SocketTimeoutException
//					|| e instanceof UnknownHostException) {
//				Crouton.makeText(getActivity(), "网络请求超时", Style.ALERT).show();
//			}
//		}
	}
}