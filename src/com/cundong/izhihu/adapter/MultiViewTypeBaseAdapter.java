package com.cundong.izhihu.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.util.SparseArray;
import android.view.View;
import android.widget.BaseAdapter;

public abstract class MultiViewTypeBaseAdapter<T> extends BaseAdapter {

	protected static int TYPE_COUNTER = 1;
	
	protected Context mContext;
	protected ArrayList<T> mDataList;

	public MultiViewTypeBaseAdapter(Context context, ArrayList<T> list) {
		this.mContext = context;
		this.mDataList = list == null ? new ArrayList<T>() : new ArrayList<T>(list);
	}

	public MultiViewTypeBaseAdapter(Context context, ArrayList<T> list, int viewTypeCount) {
		this(context, list);
		
		TYPE_COUNTER = viewTypeCount;
	}
	
	@Override
	public int getCount() {
		return mDataList!=null ? mDataList.size() : 0;
	}

	@Override
	public Object getItem(int position) {
		if (position >= mDataList.size())
			return null;
		return mDataList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public int getViewTypeCount() {
		return TYPE_COUNTER;
	}
	
	/**
	 * 该方法需要子类实现，需要根据type返回item布局的resource id
	 * 
	 * @param type
	 * @return
	 */
	public abstract int getItemResourceId(int type);
	
	 /**
     * 使用该getItemView方法替换原来的getView方法中部分功能，需要子类实现
     * 
     * @param position
     * @param convertView
     * @param holder
     * @param type
     * @return
     */
	public abstract View getItemView(int position, View convertView, ViewHolder holder, int type);

	public class ViewHolder {
	    private SparseArray<View> views = new SparseArray<View>();
	    private View convertView;

	    public ViewHolder(View convertView) {
	        this.convertView = convertView;
	    }

	    @SuppressWarnings({ "hiding", "unchecked" })
		public <T extends View> T getView(int resId) {
	        View v = views.get(resId);
	        if (null == v) {
	            v = convertView.findViewById(resId);
	            views.put(resId, v);
	        }
	        return (T) v;
	    }
	}
}