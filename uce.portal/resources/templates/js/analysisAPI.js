/**
 * Runs the analysis pipeline with the selected models and input text
 */
async function runAnalysisPipeline() {
    const selectedModels = [];
    const inputText = $('#input').val().trim();
    const $runButton = $('.run-pipeline-btn');

    // Get only the *lowest* selected checkboxes (no partially selected parents)
    $('.model-checkbox:checked').each(function() {
        const $checkbox = $(this);
        const $childCheckboxes = $checkbox.closest('li').find('ul input[type="checkbox"]');
        if ($childCheckboxes.length === 0) {
            selectedModels.push($checkbox[0]["id"]);
        }
    });

    // Validation: input text must not be empty
    if (inputText.length === 0) {
        showMessageModal("Input Error", "Please enter text to analyze.");
        return;
    }

    // Validation: at least one model must be selected
    if (selectedModels.length === 0) {
        showMessageModal("Selection Error", "Please select at least one model.");
        return;
    }

    // Disable the button and show loading spinner
    $runButton.prop('disabled', true);
    $('.analysis-result-container .loader-container').fadeIn(150);

    // API call
    const firstResponse = await $.ajax({
        url: "/api/analysis/runPipeline",
        // url: "/api/analysis/runPipeline?inputText=" + inputText + "&selectedModels=" +selectedModels,
        type: "POST",
        data: JSON.stringify({
            selectedModels: selectedModels,
            inputText: inputText,
        }),
        contentType: "application/json",
        success: function(firstResponse) {
            console.log(firstResponse);
            $('#analysis-result-container').html(firstResponse);
        },
        error: function(xhr, status, error) {
            console.error(xhr.responseText);
            showMessageModal("Analysis Error", "There was an error running the analysis pipeline.");
        }
    }).always(function() {
        $runButton.prop('disabled', false);
        $('.analysis-result-container .loader-container').fadeOut(150);
    });
    console.log("First response:", firstResponse);
    const secondResponse = await $.ajax({
        url: "/api/analysis/setHistory",
        type: "GET",
        // data: JSON.stringify({
        //     selectedModels: selectedModels,
        //     inputText: inputText,
        // }),
        // contentType: "application/json",
        success: function(secondResponse) {
            console.log(secondResponse);
            $('#analysis-result-history').html(secondResponse);
        },
        error: function(xhr, status, error) {
            console.error(xhr.responseText);
            showMessageModal("History Error", "There was an error retrieving the analysis history.");
        }
    });
    console.log("Second response:", secondResponse);
}

// Bind to the "Run Pipeline" button
$('body').on('click', '.run-pipeline-btn', function() {
    runAnalysisPipeline();
});


$('body').on('click', '[id^="history-"]', function() {
    const historyId = $(this).attr('id').replace('history-', '');
    console.log("Clicked history:", historyId);

    const $runButton = $('.run-pipeline-btn');
    $runButton.prop('disabled', true);
    $('.analysis-result-container .loader-container').fadeIn(150);
    // API call
    // Load result
    $.ajax({
        url: "/api/analysis/callHistory",
        type: "POST",
        data: JSON.stringify({ historyId }),
        contentType: "application/json",
        success: function (response) {
            console.log(response);
            $('#analysis-result-container').html(response);
        },
        error: function (xhr) {
            console.error(xhr.responseText);
            showMessageModal("Analysis Error", "Fehler beim Laden des Analyse-Ergebnisses.");
        }
    });
    // Load input text
    $.ajax({
        url: "/api/analysis/callHistoryText",
        type: "POST",
        data: JSON.stringify({ historyId }),
        contentType: "application/json",
        success: function (response) {
            console.log(response);
            $('#analysis-InputText-container').html(response);
        },
        error: function (xhr) {
            console.error(xhr.responseText);
            showMessageModal("Analysis Error", "Fehler beim Laden des Eingabetexts.");
        },
        complete: function () {
            // Run only after second call finishes
            $runButton.prop('disabled', false);
            $('.analysis-result-container .loader-container').fadeOut(150);
        }
    });
});
