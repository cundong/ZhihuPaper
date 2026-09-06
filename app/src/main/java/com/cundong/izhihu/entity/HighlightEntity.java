package com.cundong.izhihu.entity;

import java.io.Serializable;

/** A user highlight anchored to offsets in a stored article body. */
public class HighlightEntity extends BaseEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public long id;
	public String newsId;
	public String quote;
	public String note;
	public int startOffset = -1;
	public int endOffset = -1;
	public long createdAt;
	public long updatedAt;
}
