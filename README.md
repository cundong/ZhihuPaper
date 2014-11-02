# 一个知乎日报客户端 - 知乎小报

------

## 介绍

如你所见，这是一个知乎日报客户端，我给它起名为：知乎小报。

因为有大把的时间需要在地铁上度过，而我又喜欢知乎日报中的内容，于是就想自己也开发一个知乎日报的客户端，没有让人皱眉的启动页广告，没有应用推荐，没有后台的消息推送，只提供最初的阅读功能。

经过了一段时间的开发，最初的设想已经全部完成：

* 最新新闻、过往的新闻的展示
* 下拉刷新数据
* 新闻正文的展示（WebView的各种使用）
* 正文中图片查看、保存至相册
* 离线下载
* 夜间模式
* 收藏夹
* 已读数据缓存
* 内容分享至SNS

由于时间和精力的关系，可能会有一些适配问题未能来得及解决，而且不可避免的会存在BUG，希望你能够Fork一个分支出来，和我一同开发。

本程序依赖：

* [Crouton][1]
* [SmoothProgressBar][2]
* [ActionBarSherlock][3]
* [ActionBarPullToRefresh][4]
* [PhotoView][5]

参考：
* [ZhihuDailyPurify][6]

使用Ant批量打渠道包：
* [Ant 批量多渠道打包][7]

部分资源文件来源于知乎日报的官方客户端，其他图片资源获取自网络。另外，程序中所使用的所有Api，都是通过破解知乎日报官方客户端得来，最终解释权归知乎所有。

## 体验

[体验apk][13]

## 截图

* 主页

![截屏][8]

* 详情页

![截屏][9]

* 收藏夹

![截屏][10]

* 偏好设置

![截屏][11]

## 关于我

* Blog: [http://my.oschina.net/liucundong/blog][12]
* Mail: cundong.liu#gmail.com

## License

    Copyright 2014 Cundong

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


  [1]: https://github.com/keyboardsurfer/Crouton
  [2]: https://github.com/castorflex/SmoothProgressBar
  [3]: https://github.com/JakeWharton/ActionBarSherlock
  [4]: https://github.com/chrisbanes/ActionBar-PullToRefresh
  [5]: https://github.com/chrisbanes/PhotoView
  [6]: https://github.com/izzyleung/ZhihuDailyPurify
  [7]: https://github.com/cundong/blog/blob/master/Android%20Ant%20%E6%89%B9%E9%87%8F%E5%A4%9A%E6%B8%A0%E9%81%93%E6%89%93%E5%8C%85%E5%AE%9E%E4%BE%8B.md
  [8]: https://raw.githubusercontent.com/cundong/ZhihuPaper/master/screenshot/one.png
  [9]: https://raw.githubusercontent.com/cundong/ZhihuPaper/master/screenshot/two.png
  [10]: https://raw.githubusercontent.com/cundong/ZhihuPaper/master/screenshot/three.png
  [11]: https://raw.githubusercontent.com/cundong/ZhihuPaper/master/screenshot/four.png
  [12]: http://my.oschina.net/liucundong/blog
  [13]: https://github.com/cundong/ZhihuPaper/blob/master/apk/ZhihuPaper%20V2.1.4.apk
