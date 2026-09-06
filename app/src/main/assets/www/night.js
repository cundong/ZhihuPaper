
function set_night_mode(mode) {
    document.body.className += mode ? 'night ' : ' ';
    document.body.style.visibility = 'visible';
}