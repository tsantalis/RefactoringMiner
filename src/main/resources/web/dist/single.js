$(document).ready(function() {
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
            var targetOffset = $('#' + targetId).offset().top;
            var accordionOffset = $('#accordion').offset().top;
            var scrollTo = targetOffset - accordionOffset + $('#accordion').scrollTop();
            // console.log($target)

            // Check if the target is already in view
            if (Math.abs($('#accordion').scrollTop() - scrollTo) < 1) {
                // If it's already in view, shake it
                shakeElement($('#' + targetId));
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

function shakeElement(element) {
    $(element).addClass('shake');
    // Remove the class after the animation ends
    setTimeout(function() {
        $(element).removeClass('shake');
    }, 500); // Duration of the animation in milliseconds
}