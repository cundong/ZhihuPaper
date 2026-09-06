package com.cundong.izhihu.task;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;

/**
 * 类说明：	后台任务基类
 *
 * 替代原先从 android.os.AsyncTask 抄改而来的 MyAsyncTask。改造原因：
 *
 * 1. AsyncTask 自 API 30 起标记 @Deprecated，官方明确建议改用 java.util.concurrent；
 *    原 MyAsyncTask 复制了它的整套实现（InternalHandler、WorkerRunnable、
 *    AsyncTaskResult 等），把这些问题一并继承了下来。
 * 2. 原实现用 {@code Params[]} 泛型数组承载参数（{@code AsyncTaskResult(MyAsyncTask, Data...)}），
 *    Java 无法创建泛型数组，只能靠 @SuppressWarnings("unchecked") 压住，
 *    这正是编译期 "使用了未经检查或不安全的操作" 警告的来源。这里改用
 *    {@code List<Params>}，泛型信息完整，无需任何 unchecked 压制。
 * 3. 原实现的线程池用 DiscardOldestPolicy 且队列仅 10/5，队列满时会静默丢弃
 *    最老的任务。这里使用有界并发、无界排队：不会丢任务，也不会因拒绝策略
 *    把网络/数据库工作退回主线程执行。
 *
 * 生命周期回调（onPreExecute/onPostExecute/onProgressUpdate/onCancelled）
 * 仍在主线程执行，doInBackground 在线程池执行，与原语义一致。
 *
 * @version 2.0
 */
public abstract class BackgroundTask<Params, Progress, Result> {

	private static final int GENERAL_POOL_SIZE = 4;
	private static final int DOWNLOAD_POOL_SIZE = 2;

	private static final ThreadFactory sThreadFactory = new NamedThreadFactory("ZhihuTask #");

	private static final ThreadFactory sDownloadThreadFactory = new NamedThreadFactory("ZhihuDownload #");

	private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<Runnable>();

	private static final BlockingQueue<Runnable> sDownloadPoolWorkQueue = new LinkedBlockingQueue<Runnable>();

	/**
	 * 通用后台线程池（原 MyAsyncTask.THREAD_POOL_EXECUTOR）
	 */
	public static final ExecutorService THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
			GENERAL_POOL_SIZE, GENERAL_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
			sPoolWorkQueue, sThreadFactory);

	/**
	 * 下载专用线程池（原 MyAsyncTask.DOWNLOAD_THREAD_POOL_EXECUTOR）
	 */
	public static final ExecutorService DOWNLOAD_THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
			DOWNLOAD_POOL_SIZE, DOWNLOAD_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
			sDownloadPoolWorkQueue, sDownloadThreadFactory);

	private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

	private final AtomicBoolean mCancelled = new AtomicBoolean(false);

	private final AtomicBoolean mFinished = new AtomicBoolean(false);

	private volatile Status mStatus = Status.PENDING;

	private volatile Future<?> mFuture;

	private static final class NamedThreadFactory implements ThreadFactory {

		private final AtomicInteger mCount = new AtomicInteger(1);

		private final String mPrefix;

		NamedThreadFactory(String prefix) {
			mPrefix = prefix;
		}

		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, mPrefix + mCount.getAndIncrement());
		}
	}

	/**
	 * 任务状态，语义与原 MyAsyncTask.Status 一致。
	 */
	public enum Status {
		PENDING,
		RUNNING,
		FINISHED,
	}

	public final Status getStatus() {
		return mStatus;
	}

	/**
	 * 在后台线程执行。
	 *
	 * @param params 调用 execute 时传入的参数，永不为 null（无参时为空列表）
	 */
	protected abstract Result doInBackground(List<Params> params);

	/**
	 * 主线程，任务开始前调用。
	 */
	protected void onPreExecute() {
	}

	/**
	 * 主线程，任务正常结束后调用；任务被取消时不会调用。
	 */
	protected void onPostExecute(Result result) {
	}

	/**
	 * 主线程，收到 publishProgress 时调用。
	 */
	protected void onProgressUpdate(Progress value) {
	}

	/**
	 * 主线程，任务被取消后调用。
	 */
	protected void onCancelled() {
	}

	public final boolean isCancelled() {
		return mCancelled.get();
	}

	/**
	 * 请求取消任务。
	 *
	 * @param mayInterruptIfRunning 是否中断已在执行的线程
	 */
	public final boolean cancel(boolean mayInterruptIfRunning) {
		if (!mCancelled.compareAndSet(false, true)) {
			return false;
		}

		Future<?> future = mFuture;
		if (future != null) {
			future.cancel(mayInterruptIfRunning);
		}

		postToMainThread(new Runnable() {
			@Override
			public void run() {
				if (mFinished.compareAndSet(false, true)) {
					mStatus = Status.FINISHED;
					onCancelled();
				}
			}
		});

		return true;
	}

	/**
	 * 在默认线程池上执行。
	 */
	@SafeVarargs
	public final BackgroundTask<Params, Progress, Result> execute(Params... params) {
		return executeOnExecutor(THREAD_POOL_EXECUTOR, params);
	}

	/**
	 * 在指定线程池上执行。必须在主线程调用，且同一实例只能执行一次。
	 */
	@SafeVarargs
	public final BackgroundTask<Params, Progress, Result> executeOnExecutor(
			ExecutorService executor, Params... params) {

		if (mStatus != Status.PENDING) {
			switch (mStatus) {
				case RUNNING:
					throw new IllegalStateException("Cannot execute task: the task is already running.");
				default:
					throw new IllegalStateException("Cannot execute task: the task has already been "
							+ "executed (a task can be executed only once)");
			}
		}

		mStatus = Status.RUNNING;

		onPreExecute();

		final List<Params> paramList = (params == null || params.length == 0)
				? Collections.<Params>emptyList()
				: Collections.unmodifiableList(Arrays.asList(params));

		mFuture = executor.submit(new Runnable() {
			@Override
			public void run() {
				Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

				Result result = null;
				try {
					if (!isCancelled()) {
						result = doInBackground(paramList);
					}
				} catch (Throwable t) {
					// doInBackground 里未捕获的异常在旧实现中会被 FutureTask 包成
					// RuntimeException 重新抛出，实际表现为静默吞掉。这里显式打印，
					// 并按取消路径收尾，避免任务卡在 RUNNING 状态。
					t.printStackTrace();
					mCancelled.set(true);
				}

				final Result finalResult = result;

				postToMainThread(new Runnable() {
					@Override
					public void run() {
						if (!mFinished.compareAndSet(false, true)) {
							return;
						}

						mStatus = Status.FINISHED;

						if (isCancelled()) {
							onCancelled();
						} else {
							onPostExecute(finalResult);
						}
					}
				});
			}
		});

		return this;
	}

	/**
	 * 后台线程调用，把进度回抛到主线程。任务取消后不再回调。
	 */
	protected final void publishProgress(final Progress value) {
		if (isCancelled()) {
			return;
		}

		postToMainThread(new Runnable() {
			@Override
			public void run() {
				if (!isCancelled()) {
					onProgressUpdate(value);
				}
			}
		});
	}

	private static void postToMainThread(Runnable runnable) {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			runnable.run();
		} else {
			sMainHandler.post(runnable);
		}
	}
}
