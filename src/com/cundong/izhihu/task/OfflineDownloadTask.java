package com.cundong.izhihu.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.text.TextUtils;

import com.cundong.izhihu.Constants;
import com.cundong.izhihu.db.NewsDataSource;
import com.cundong.izhihu.entity.NewsDetailEntity;
import com.cundong.izhihu.entity.NewsListEntity;
import com.cundong.izhihu.entity.NewsListEntity.NewsEntity;
import com.cundong.izhihu.http.HttpClientUtils;
import com.cundong.izhihu.util.FileUtils;
import com.cundong.izhihu.util.GsonUtils;
import com.cundong.izhihu.util.StreamUtils;
import com.cundong.izhihu.util.ZhihuUtils;

/**
 * 类说明： 	离线下载新闻，Task
 * 
 * @date 	2014-9-7
 * @version 1.0
 */
public class OfflineDownloadTask extends BaseGetContentTask {
	
	private String mUrl = null;
	
	public OfflineDownloadTask(Context context, ResponseListener listener) {
		super(context, listener);
	}

	@Override
	protected String doInBackground(String... params) {

		mUrl = params[0];
		String content = null;

		try {
			content = getUrl(mUrl);
			
			NewsListEntity newsListEntity = (NewsListEntity) GsonUtils.getEntity(content, NewsListEntity.class);
			ArrayList<NewsEntity> stories = newsListEntity != null ? newsListEntity.stories : null;
			
			if (stories != null && stories.size() != 0) {

				for (NewsEntity newsEntity : stories) {
					
					String detailContent = getUrl(Constants.Url.URL_DETAIL + newsEntity.id);
					NewsDetailEntity detailEntity = (NewsDetailEntity) GsonUtils.getEntity(detailContent, NewsDetailEntity.class);
					
					if (detailEntity == null || TextUtils.isEmpty(detailEntity.body)) {
						continue;
					}
					
					((NewsDataSource) getDataSource()).insertOrUpdateNewsList(Constants.NEWS_DETAIL, "detail_" + newsEntity.id, detailContent);
					
					ArrayList<String> imageList = new ArrayList<String>();
					
					if(!TextUtils.isEmpty(detailEntity.image)) {
						imageList.add(detailEntity.image);
					}
					
					imageList.addAll(getImgs(detailEntity.body));
					
					File file = null;
					for (String imageUrl : imageList) {
						
						String filePath = ZhihuUtils.getCacheImgFilePath(mContext, imageUrl);
						file = new File(filePath);
						
						boolean needDownload = true;
						
						if (!file.exists()) {
							try {
								file.createNewFile();
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else {
							long fileSize = FileUtils.getFileSize(filePath);
							
							if (fileSize == 0) {
								// need re download
							} else {
								needDownload = false;
							}
						}
						
						if (needDownload) {
							InputStream in = null;
							OutputStream out = null;
							
							// from web
							try {
								in = HttpClientUtils.getStream(mContext, imageUrl, null);
								out = new FileOutputStream(file);

								StreamUtils.copy(in, out);
								
							} catch (IOException e) {
								e.printStackTrace();
							} catch (Exception e) {
								e.printStackTrace();
							} finally {
								StreamUtils.close(out);
								StreamUtils.close(in);
							}
						} else {
							// no need download
						}
					}
				}
				
				return "success";
			}
		} catch (IOException e) {
			e.printStackTrace();

			isRefreshSuccess = false;
		} catch (Exception e) {
			e.printStackTrace();

			isRefreshSuccess = false;
		}
		
		return null;
	}
	
	/**
	 * 从body字段中获取所有<img标签 例：http://news-at.zhihu.com/api/3/news/4074764
	 * 
	 * @param html
	 * @return
	 */
	private ArrayList<String> getImgs(String html) {

		ArrayList<String> imgList = new ArrayList<String>();

		Document doc = Jsoup.parse(html);
		Elements es = doc.getElementsByTag("img");
		
		for (Element e : es) {
			String src = e.attr("src");

			String newImgUrl = src.replaceAll("\"", "");
			newImgUrl = newImgUrl.replace('\\', ' ');
			newImgUrl = newImgUrl.replaceAll(" ", "");

			if(!TextUtils.isEmpty(newImgUrl)) {
				imgList.add(newImgUrl);
			}
		}
		
		return imgList;
	}
}