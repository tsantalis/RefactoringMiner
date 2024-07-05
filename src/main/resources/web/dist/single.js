$(document).ready(function() {
    // Function to resize all iframes based on their content
    console.log("single.js loaded");

    function resizeAllIframes() {
        console.log("resizeAllIframes");
        $('iframe').each(function() {
            var iframe = this;
            iframe.style.height = iframe.contentWindow.document.body.scrollHeight + 'px';
        });
    }

    // Call resizeAllIframes when each iframe is loaded and when the window is resized
    $('iframe').on('load', function() {
        resizeAllIframes();
    });

    $(window).resize(function() {
        resizeAllIframes();
    });
});
