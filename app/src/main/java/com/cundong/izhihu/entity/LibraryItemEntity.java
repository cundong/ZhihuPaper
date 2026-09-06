package com.cundong.izhihu.entity;

import java.io.Serializable;
import java.util.ArrayList;

/** User-owned state for an article in the local reading library. */
public class LibraryItemEntity extends BaseEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public String newsId;
	public String title;
	public String logo;
	public String shareUrl;
	public boolean favorite;
	public boolean readLater;
	public String note;
	public String body;
	public long createdAt;
	public long updatedAt;
	public ArrayList<String> tags = new ArrayList<String>();
}
