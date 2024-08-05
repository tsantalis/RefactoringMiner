x = 1;
$(document).ready(function() {
    console.log("single loaded");
    // Select all elements with an ID that starts with 'diff_row_'
    $('[id^="diff_row_"]').each(function() {
        // Update the href attribute of each selected element
        var id = $(this).attr('id');
        $(this).attr('href', '#' + id);

        // Add click event listener to scroll the accordion panel
        $(this).click(function(event) {
            event.preventDefault(); // Prevent the default anchor behavior
            var targetId = 'heading-' + id.split('_')[2];
            var $target = $('#' + targetId);
            x = $target
            var targetOffset = $('#' + targetId).offset().top;
            var accordionOffset = $('#accordion').offset().top;
            var scrollTo = targetOffset - accordionOffset + $('#accordion').scrollTop();
            // console.log($target)

            // Check if the target is already in view
            if (Math.abs($('#accordion').scrollTop() - scrollTo) < 1) {
                // If it's already in view, shake it
                $target.effect('shake', { direction: 'up', distance: 10, times: 3 }, 500);
            } else {
                // Scroll the accordion panel to the element with the corresponding heading ID
                $('#accordion').animate({
                    scrollTop: scrollTo
                }, 500, function() {
                    // Optionally shake after scrolling
                    // $target.effect('shake', { direction: 'up', distance: 10, times: 3 }, 500);
                }); // Adjust the scroll duration as needed
            }
        });
    });
});

document.addEventListener('DOMContentLoaded', () => {
    const iframes = document.querySelectorAll('iframe');
    const workers = [];

    iframes.forEach((iframe, index) => {
        const worker = new Worker('/path/to/worker.js');
        workers.push(worker);
        console.log("test" + index);
        worker.onmessage = function (e) {
            const data = e.data;
            console.log(`Iframe ${data.index}: ${data.result}`);
            // Post the processed data to the iframe if necessary
            iframe.contentWindow.postMessage(data.result, '*');
        };

        worker.postMessage(index);
    });
});

if (typeof $.fn.effect === 'undefined') {
    console.error('jQuery UI effect method is not loaded.');
}

document.addEventListener('DOMContentLoaded', function() {
    document.addEventListener('scroll', function(event) {
        console.log('Scroll event detected!', event);
    });
});