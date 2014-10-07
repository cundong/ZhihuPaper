# 开源知乎日报客户端 - ZhihuPaper

------

## 介绍

如你所见，这是一个知乎日报客户端。

因为有大把的时间需要在地铁上度过，而我又喜欢知乎日报中的内容，于是就想自己也开发一个知乎日报的客户端。

经过了一段时间的准备，已经基本能用，实现了以下功能：

* 基础框架的搭建
* 当天最新新闻展示，下拉展示更多过往的新闻
* 下拉刷新数据
* 新闻正文的展示
* 新闻正文向上滑动隐藏标题栏，向下滑动展示标题栏
* 正文中图片点击查看大图、图片保存至相册
* 2G、3G网络下，无图浏览
* 内容分享至SNS
* 离线下载
* 新闻列表的 已读、未读

还有以下功能待实现：
* 收藏夹
* 夜间模式

另外，由于时间和精力的关系，有很多适配问题没有解决，并且不可避免的会存在很多BUG，欢迎Fork一个分支出来，和我一同开发。

本程序依赖：

* [Crouton][3]
* [SmoothProgressBar][4]
* [ActionBarSherlock][5]
* [ActionBarPullToRefresh][6]
* [PhotoView][8]

参考：
* [ZhihuDailyPurify][7]

部分资源文件来源于知乎日报的官方客户端，其他图片资源获取自网络。

## 截图

![截屏][1]
![截屏][2]

## 关于我

* Blog: [http://my.oschina.net/liucundong/blog][4]
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

[1]: https://raw.githubusercontent.com/cundong/ZhihuPaper/master/screenshot/one.png
[2]: https://raw.githubusercontent.com/cundong/ZhihuPaper/master/screenshot/two.png
[3]: https://github.com/keyboardsurfer/Crouton
[4]: https://github.com/castorflex/SmoothProgressBar
[5]: https://github.com/JakeWharton/ActionBarSherlock
[6]: https://github.com/chrisbanes/ActionBar-PullToRefresh
[7]: https://github.com/izzyleung/ZhihuDailyPurify
[8]: https://github.com/chrisbanes/PhotoView
