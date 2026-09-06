package com.cundong.izhihu.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.view.View;

import androidx.annotation.NonNull;

import com.cundong.izhihu.task.BackgroundTask;
import com.cundong.izhihu.util.Logger;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

	protected SwipeRefreshLayout mPullToRefreshLayout;

	protected Logger mLogger = Logger.getLogger();

	private Bundle savedState;

	private final List<BackgroundTask<?, ?, ?>> viewTasks =
			new ArrayList<BackgroundTask<?, ?, ?>>();

	public BaseFragment() {
        super();
        if (getArguments() == null)
            setArguments(new Bundle());
    }

	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		if (mPullToRefreshLayout != null) {
			mPullToRefreshLayout.setOnRefreshListener(this);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!restoreStateFromArguments()) {
			onFirstTimeLaunched();
		}
	}

	@Override
	public void onDestroyView() {
		cancelViewTasks();
		super.onDestroyView();

		saveStateToArguments();
	}

	/** Registers work whose callbacks are only valid for the current view lifecycle. */
	protected final <T extends BackgroundTask<?, ?, ?>> T trackViewTask(T task) {
		if (task != null) {
			viewTasks.add(task);
		}
		return task;
	}

	protected final void forgetViewTask(BackgroundTask<?, ?, ?> task) {
		viewTasks.remove(task);
	}

	protected final void cancelViewTask(BackgroundTask<?, ?, ?> task) {
		if (task != null) {
			task.cancel(true);
			viewTasks.remove(task);
		}
	}

	private void cancelViewTasks() {
		for (BackgroundTask<?, ?, ?> task : new ArrayList<BackgroundTask<?, ?, ?>>(viewTasks)) {
			task.cancel(true);
		}
		viewTasks.clear();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
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
	public void onRefresh() {
		doRefresh();
	}

	protected void doRefresh() {

	}

	protected void finishRefresh() {
		if (isAdded() && mPullToRefreshLayout != null) {
			mPullToRefreshLayout.setRefreshing(false);
		}
	}

	protected void dealException(Exception e) {
		finishRefresh();
	}
}
