package com.cundong.izhihu.entity;

import java.io.Serializable;

/** Read marker and normalized (0..1) article reading position. */
public class ReadingStateEntity extends BaseEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public String newsId;
	public long readAt;
	public long lastReadAt;
	public float progress;
}
