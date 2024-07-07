$(document).ready(function() {
    // Select all elements with an ID that starts with 'diff_row_'
    $('[id^="diff_row_"]').each(function() {
        // Update the href attribute of each selected element
        var id = $(this).attr('id');
        $(this).attr('href', '#' + id);

        // Add click event listener to scroll the right panel
        $(this).click(function(event) {
            event.preventDefault(); // Prevent the default anchor behavior
            var targetId = 'heading-' + id.split('_')[2];
            console.log(targetId);
            // Scroll the monaco panel to the element with the corresponding heading ID
            $('#accordion').animate({
                scrollTop: $('#' + targetId).offset().top
            }, 500); // Adjust the scroll duration as needed
        });
    });
});