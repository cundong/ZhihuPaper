$(function(){
	var totalWidth = document.body.clientWidth, margin, height;
	margin = 20;
	totalWidth = totalWidth - margin*2;
	height = parseInt(totalWidth*3/4);
	$('.video-wrap').each(function(index, obj){
		this.width = totalWidth;
		this.height = height;
	});
});