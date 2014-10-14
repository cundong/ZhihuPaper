# 一个知乎日报客户端 - 知乎小报

------

## 介绍

如你所见，这是一个知乎日报客户端，我给它起名为：知乎小报。

因为有大把的时间需要在地铁上度过，而我又喜欢知乎日报中的内容，于是就想自己也开发一个知乎日报的客户端。

经过了一段时间的开发，一个新闻阅读类App该有的功能已全部完成：

* 最新新闻、过往的新闻的展示
* 下拉刷新数据
* 新闻正文的展示
* 正文中图片查看、保存至相册
* 离线下载
* 夜间模式
* 收藏夹
* 已读数据缓存
* 内容分享至SNS

由于时间和精力的关系，有很多适配问题没有解决，并且不可避免的会存在很多BUG，希望你能够Fork一个分支出来，和我一同开发。

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

* 主页

![截屏][1]

* 详情页

![截屏][2]

* 收藏夹

![截屏][10]

* 偏好设置

![截屏][11]

## 关于我

* Blog: [http://my.oschina.net/liucundong/blog][9]
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
[9]: http://my.oschina.net/liucundong/blog
[10]: https://raw.githubusercontent.com/cundong/ZhihuPaper/master/screenshot/three.png
[11]: https://raw.githubusercontent.com/cundong/ZhihuPaper/master/screenshot/four.png
