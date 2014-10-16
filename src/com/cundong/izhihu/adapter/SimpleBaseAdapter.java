package com.cundong.izhihu.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class SimpleBaseAdapter<T> extends BaseAdapter {

	protected Context mContext;
	protected ArrayList<T> mDataList;

	public SimpleBaseAdapter(Context context, ArrayList<T> list) {
		this.mContext = context;
		this.mDataList = list == null ? new ArrayList<T>() : new ArrayList<T>(list);
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
	
	/**
	 * 该方法需要子类实现，需要返回item布局的resource id
	 * 
	 * @return
	 */
	public abstract int getItemResourceId();
    
	 /**
     * 使用该getItemView方法替换原来的getView方法，需要子类实现
     * 
     * @param position
     * @param convertView
     * @param holder
     * @return
     */
    public abstract View getItemView(int position, View convertView, ViewHolder holder);

	@SuppressWarnings("unchecked")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if( convertView == null ){
			convertView = LayoutInflater.from(mContext).inflate(getItemResourceId(), parent, false);
			holder = new ViewHolder( convertView );
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		return getItemView(position, convertView, holder);
	}

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