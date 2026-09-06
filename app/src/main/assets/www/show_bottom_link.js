var $theme;

function show(info) {
	if (!info) return;
	info = JSON.parse(info);
	if (info.theme_id && info.theme_name && info.theme_image) {
		$theme = $('<div class="origin-source-wrap"><a class="origin-source with-link" href="zhihudaily://theme/' + info.theme_id + '"><img src="' + info.theme_image + '" class="source-logo"><span class="text">本文来自：' + info.theme_name + '</span></a><a class="focus-link" href="zhihu-theme-subscribe"><span class="btn-label">关注</span></a></div>').appendTo($(".question").last());

        if (info.theme_subscribed === true) {
            $theme.removeClass('unfocused');
        } else if (info.theme_subscribed === false) {
            $theme.addClass('unfocused');
        }

	} else if (info.section_id && info.section_name && info.section_thumbnail) {
		$(".question").last().append('<div class="origin-source-wrap"><a class="origin-source with-link" href="zhihudaily://section/' + info.section_id + '"><img src="' + info.section_thumbnail + '" class="source-logo"><span class="text">本文来自：' + info.section_name.replace(/#/g, "") + ' · 合集</span></a></div>');
	}
}
