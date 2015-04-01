package com.cundong.izhihu.fragment;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import com.cundong.izhihu.util.Logger;

public abstract class BaseFragment extends Fragment implements OnRefreshListener {
	
	protected PullToRefreshLayout mPullToRefreshLayout;
	
	protected Logger mLogger = Logger.getLogger();
	
	private Bundle savedState;
	
	public BaseFragment() {
        super();
        if (getArguments() == null)
            setArguments(new Bundle());
    }
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		ActionBarPullToRefresh.from(getActivity()).allChildrenArePullable()
				.listener(this)
				.setup(this.mPullToRefreshLayout);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Restore State Here
		if (!restoreStateFromArguments()) {
			// First Time, Initialize something here
			onFirstTimeLaunched();
		}
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();

		saveStateToArguments();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		saveStateToArguments();
	}

	private void saveStateToArguments() {
		if (getView() != null) {
			savedState = saveState();
		} 
		
		if (savedState != null) {
			Bundle b = getArguments();
			b.putBundle("internalSavedViewState", savedState);
		}
	}

	private boolean restoreStateFromArguments() {
		Bundle b = getArguments();
		savedState = b.getBundle("internalSavedViewState");
		if (savedState != null) {
			restoreState();
			return true;
		}
		return false;
	}

	private void restoreState() {
		if (savedState != null) {
			onRestoreState(savedState);
		}
	}

	private Bundle saveState() {
		Bundle state = new Bundle();
		onSaveState(state);
		return state;
	}

	protected abstract void onRestoreState(Bundle savedInstanceState);
	
	protected abstract void onSaveState(Bundle outState);

	protected abstract void onFirstTimeLaunched();
	
	@Override
	public void onRefreshStarted(View view) {
		doRefresh();
	}
	
	protected void doRefresh() {

	}
	
	protected void dealException(Exception e) {
		
		if (isAdded()) {
			mPullToRefreshLayout.setRefreshComplete();
		}
	}
}