package com.cundong.izhihu.entity;

import java.io.Serializable;
import java.util.ArrayList;

public class NewsDetailEntity extends BaseEntity implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String body;
	public String image_source;
	public String title;
	public String image;
	public String share_url;
	public ArrayList<String> js;
	public int type;
	public String ga_prefix;
	public long id;
//	public ArrayList<String> css;
}