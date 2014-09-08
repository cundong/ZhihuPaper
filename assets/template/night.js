var body = document.body;

set_night_mode('night');

onload = function () {
	body.style.visibility = 'visible';
}

function set_night_mode(mode) {
    body.className += mode ? 'night ' : ' ';
    body.style.visibility = 'visible';
    return 6;
}
