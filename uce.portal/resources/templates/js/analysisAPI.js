/**
 * Runs the analysis pipeline with the selected models and input text
 */
async function runAnalysisPipeline() {
    const selectedModels = [];
    const inputText = $('#analysis-input').val().trim();
    const $runButton = $('.run-pipeline-btn');
    const inputClaim = $('#claim-text').val().trim();
    const inputCoherence = $('#coherence-text').val().trim();
    const inputStance = $('#stance-text').val().trim();
    const inputLLM = $('#llm-text').val().trim();
    let checkboxClaim = false;
    let checkboxCoherence = false;
    let checkboxStance = false;
    let checkboxLLM = false;

    // Get only the *lowest* selected checkboxes (no partially selected parents)
    $('.analysis-model-checkbox:checked').each(function() {
        const $checkbox = $(this);
        const $childCheckboxes = $checkbox.closest('li').find('ul input[type="checkbox"]');
        if ($childCheckboxes.length === 0) {
            selectedModels.push($checkbox[0]["id"]);
            const isFactChecking = $checkbox[0]["id"].toLowerCase().includes('factchecking');
            if (isFactChecking) {
                checkboxClaim = true;
            }
            const isCoherence = $checkbox[0]["id"].toLowerCase().includes('cohesion');
            if (isCoherence) {
                checkboxCoherence = true;
            }
            const isStance = $checkbox[0]["id"].toLowerCase().includes('stance');
            if (isStance) {
                checkboxStance = true;
            }
            const isLLM = $checkbox[0]["id"].toLowerCase().includes('llm');
            if (isLLM) {
                checkboxLLM = true;
            }
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

    // Validation: if FactChecking is selected, the claim field must not be empty
    if (checkboxClaim && inputClaim.length === 0) {
        showMessageModal("Claim Error", "Please enter a claim to analyze.");
        return;
    }

    // Validation: if Coherence is selected, the coherence field must not be empty
    if (checkboxCoherence && inputCoherence.length === 0) {
        showMessageModal("Coherence Error", "Please enter a coherence text to analyze.");
        return;
    }

    // Validation: if Stance is selected, the stance field must not be empty
    if (checkboxStance && inputStance.length === 0) {
        showMessageModal("Stance Error", "Please enter a Hypothesis text to analyze.");
        return;
    }

    // Validation: if LLM is selected, the LLM field must not be empty
    if (checkboxLLM && inputLLM.length === 0) {
        showMessageModal("LLM Error", "Please enter a System Prompt text to analyze.");
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
            inputClaim: inputClaim,
            inputCoherence: inputCoherence,
            inputStance: inputStance,
            inputLLM: inputLLM,
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
    const secondResponse = await $.ajax({
        url: "/api/analysis/setHistory",
        type: "GET",
        // data: JSON.stringify({
        //     selectedModels: selectedModels,
        //     inputText: inputText,
        // }),
        // contentType: "application/json",
        success: function(secondResponse) {
            // console.log(secondResponse);
            $('#analysis-result-history').html(secondResponse);
        },
        error: function(xhr, status, error) {
            console.error(xhr.responseText);
            showMessageModal("History Error", "There was an error retrieving the analysis history.");
        }
    });
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
