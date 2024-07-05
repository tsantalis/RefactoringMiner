$(document).ready(function() {

    function resizeAllIframes() {
        $('iframe').each(function() {
            var iframe = this;
            iframe.style.height = iframe.contentWindow.document.body.scrollHeight + 'px';
        });
    }

    $('iframe').on('load', function() {
        resizeAllIframes();
    });

    $(window).resize(function() {
        resizeAllIframes();
    });
});
