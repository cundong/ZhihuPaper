# 开源知乎日报客户端 - ZhihuPaper

------

## 介绍

如你所见，这是一个知乎日报客户端。

因为有大把的时间需要在地铁上度过，而我又喜欢知乎日报中的内容，于是就想自己也开发一个知乎日报的客户端。

经过了一段时间的准备，已经初具规模，实现了以下功能：

> * 基础框架的搭建
> * 列表展示当天的最新新闻
> * 下拉刷新
> * 正文信息展示
> * 离线下载

还有以下功能等待实现：
> * 下拉展示更多新闻
> * 内容分享至SNS
> * 正文中图片保存
> * 无图模式开发
> * 已读、未读
> * 收藏夹
> * 夜间模式
> * bug fix

本程序依赖：

> * [Crouton][3]
> * [SmoothProgressBar][4]
> * [ActionBarSherlock][5]
> * [ActionBarPullToRefresh][6]

参考：
> * [ZhihuDailyPurify][7]

## 截图

![截屏][1]
![截屏][2]

## License

    Copyright 2014 cundong

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
