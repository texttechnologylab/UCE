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
            const isCoherence = $checkbox[0]["id"].toLowerCase().includes('cohesion ');
            if (isCoherence) {
                checkboxCoherence = true;
            }
            const isStance = $checkbox[0]["id"].toLowerCase().includes('stance ');
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
            // Warte, bis das Div wirklich im DOM ist
            requestAnimationFrame(() => {
                let hasTtlab = document.querySelector('[id^="ttlab-scorer-table-"]');
                let hasCohmetrix = document.querySelector('[id^="cohmetrix-table-"]');

                if (hasTtlab) {
                    showAllTtlabScorerTables();
                } else {
                    console.warn("Kein Div mit id 'ttlab-scorer-table-*' gefunden.");
                }

                if (hasCohmetrix) {
                    showAllCohmetrixTables();
                } else {
                    console.warn("Kein Div mit id 'cohmetrix-table-*' gefunden.");
                }
            });
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

function showTabulatorTable() {
    if (typeof window.ttlabTableData !== 'undefined' && window.ttlabTableData.length > 0) {
        var table = new Tabulator("#ttlab-scorer-table", {
            data: window.ttlabTableData,
            layout: "fitColumns",
            columns: [
                { title: "Modell", field: "model" },
                {
                    title: "Name",
                    field: "name",
                    formatter: function(cell) {
                        return '<span style="color:blue;cursor:pointer;text-decoration:underline">' + cell.getValue() + '</span>';
                    },
                    cellClick: function(e, cell) {
                        alert("Name geklickt: " + cell.getValue());
                    }
                },
                { title: "Score", field: "score" }
            ],
        });
        console.log(table);
    } else {
        console.warn("Ttlab Table Data nicht gefunden oder leer.");
    }
}

let selectedNames = [];
let chartInstance = null;

// Beispielhafte Referenzdaten (kannst du später dynamisch ersetzen)
const referencePoints = [
    { id: "doc1", x: 0.2, y: 0.5 },
    { id: "doc2", x: 0.7, y: 0.8 },
    { id: "doc3", x: -0.3, y: 1.2 },
    { id: "doc4", x: 1.4, y: -0.2 }
];

function showAllTtlabScorerTables() {
    if (typeof window.ttlabTableDataByModel !== 'undefined') {
        window.ttlabTables = {};

        Object.keys(window.ttlabTableDataByModel).forEach(function (key) {
            const data = window.ttlabTableDataByModel[key];
            const tableId = "ttlab-scorer-table-" + key;
            const selector = "#" + tableId;
            const container = document.querySelector(selector);

            if (data.length > 0 && container) {
                const table = new Tabulator(selector, {
                    data: data,
                    layout: "fitDataFill",
                    resizableRows: true,
                    resizableRowGuide: true,
                    resizableColumnGuide: true,
                    columnDefaults: {
                        resizable: true,
                    },
                    columns: [
                        { title: "Modell", field: "model" },
                        {
                            title: "Name",
                            field: "name",
                            formatter: function (cell) {
                                return '<span style="color:blue;cursor:pointer;text-decoration:underline">' + cell.getValue() + '</span>';
                            },
                            cellClick: function (e, cell) {
                                const row = cell.getRow().getData();
                                handleNameSelection(row.name, row.score);
                            }
                        },
                        { title: "Score", field: "score" }
                    ]
                });
                // const table = new Tabulator(selector, {
                //     data: data,
                //     layout: "fitDataFill",
                //     resizableRows: true,
                //     resizableRowGuide: true,
                //     resizableColumnGuide: true,
                //     columnDefaults: {
                //         resizable: true,
                //     },
                //     columns: [
                //         { title: "Modell", field: "model" },
                //         {
                //             title: "Name",
                //             field: "name",
                //             formatter: function (cell) {
                //                 return '<span style="color:blue;cursor:pointer;text-decoration:underline">' + cell.getValue() + '</span>';
                //             }
                //             // cellClick: kann entfernt werden, da wir rowClick benutzen
                //         },
                //         { title: "Score", field: "score" }
                //     ],
                //     rowClick: function (e, row) {
                //         const rowData = row.getData();
                //         handleNameSelection(rowData.name, rowData.score);
                //     }
                // });

                window.ttlabTables[key] = table;
            }
        });
    }
}

function handleNameSelection(name, score) {
    selectedNames.push({ name, score });

    if (selectedNames.length === 2) {
        updateCorpusScatterPlot();
    } else if (selectedNames.length > 2) {
        selectedNames = selectedNames.slice(-2); // nur die letzten zwei behalten
        updateCorpusScatterPlot();
    }
}

function updateCorpusScatterPlot() {
    if (selectedNames.length < 2) return;

    const container = document.getElementById('scatter-container');
    if (container.style.display === 'none') {
        container.style.display = 'block';
    }

    const xName = selectedNames[1].name;
    const yName = selectedNames[0].name;
    const xScore = selectedNames[1].score;
    const yScore = selectedNames[0].score;

    const userPoint = { id: "origin", x: xScore, y: yScore };

    const allPoints = [...referencePoints, userPoint];

    const xValues = allPoints.map(p => p.x);
    const yValues = allPoints.map(p => p.y);

    const pad = (min, max) => {
        const range = max - min || 1;
        return {
            min: min - range * 0.05,
            max: max + range * 0.05
        };
    };

    const xScale = pad(Math.min(...xValues), Math.max(...xValues));
    const yScale = pad(Math.min(...yValues), Math.max(...yValues));

    const datasets = [
        {
            label: "Referenzpunkte",
            data: referencePoints.map(p => ({ x: p.x, y: p.y, id: p.id })),
            backgroundColor: "rgba(54, 162, 235, 0.6)",
            pointRadius: 5,
            pointHoverRadius: 7
        },
        {
            label: "Ursprungspunkt",
            data: [{ x: userPoint.x, y: userPoint.y, id: "Ursprung" }],
            backgroundColor: "rgba(255, 99, 132, 1)",
            pointRadius: 7,
            pointHoverRadius: 9
        }
    ];

    const canvas = document.getElementById("ttlab-scatter-chart");

    // const container = document.getElementById("scatter-container");
    const minWidth = 400;
    const minHeight = 300;
    const style = getComputedStyle(container);
    const width = Math.max(container.clientWidth, minWidth);
    const height = Math.max(container.clientHeight, minHeight);
    const dpr = window.devicePixelRatio || 1;
    canvas.width = width * dpr;
    canvas.height = height * dpr;
    canvas.style.width = width + "px";
    canvas.style.height = height + "px";

    const ctx = canvas.getContext("2d");
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0); // für DPI Skalierung


    if (chartInstance) chartInstance.destroy();

    chartInstance = new Chart(ctx, {
        type: "scatter",
        data: { datasets },
        options: {
            responsive: true,
            plugins: {
                tooltip: {
                    callbacks: {
                        label: function (context) {
                            const { x, y, id } = context.raw;
                            return id + " (x: " + x.toFixed(3) + ", y: " + y.toFixed(3) + ")";
                        }
                    }
                },
                legend: {
                    position: "top"
                },
                title: {
                    display: true,
                    text: "Vergleich: " + xName + " (X) vs. " + yName + " (Y)"
                }
            },
            scales: {
                x: {
                    title: { display: true, text: xName },
                    min: xScale.min,
                    max: xScale.max,
                    grid: {
                        color: (ctx) => ctx.tick.value === 0 ? '#000000' : 'rgba(0,0,0,0.1)', // x=0 Linie schwarz
                        lineWidth: (ctx) => ctx.tick.value === 0 ? 2 : 1
                    }
                },
                y: {
                    title: { display: true, text: yName },
                    min: yScale.min,
                    max: yScale.max,
                    grid: {
                        color: (ctx) => ctx.tick.value === 0 ? '#000000' : 'rgba(0,0,0,0.1)', // y=0 Linie schwarz
                        lineWidth: (ctx) => ctx.tick.value === 0 ? 2 : 1
                    }
                }
            }

        }
    });
}


document.body.addEventListener('click', function (e) {
    if (e.target && e.target.id === 'close-scatter') {
        const container = document.getElementById('scatter-container');
        if (container) {
            container.style.display = 'none';
            selectedNames = [];
            chartInstance = null;
        }
    }
});

function showAllCohmetrixTables() {
    if (typeof window.cohmetrixTableDataByModel !== 'undefined') {
        window.cohmetrixTables = {};

        Object.keys(window.cohmetrixTableDataByModel).forEach(function (key) {
            const data = window.cohmetrixTableDataByModel[key];
            const tableId = "cohmetrix-table-" + key;
            const selector = "#" + tableId;
            const container = document.querySelector(selector);

            if (data.length > 0 && container) {
                const table = new Tabulator(selector, {
                    data: data,
                    // layout: "fitDataFill",
                    layout: "fitDataTable",
                    responsiveLayout: false,
                    resizableRows: true,
                    columnDefaults: {
                        resizable: true
                    },
                    columns: [
                        { title: "Modell", field: "model", widthGrow: 2  },
                        {
                            title: "Name",
                            field: "name",
                            widthGrow: 1
                        },
                        { title: "Score", field: "score", widthGrow: 1,
                            formatter: function (cell) {
                                return '<span style="color:green;cursor:pointer;text-decoration:underline">' + cell.getValue() + '</span>';
                            },
                            cellClick: function (e, cell) {
                                const row = cell.getRow().getData();
                                console.log("Coh-Metrix geklickt:", row.name, row.score);
                            }
                        },
                        { title: "Description", field: "description", widthGrow: 3  },
                    ]
                });

                window.cohmetrixTables[key] = table;
            } else {
                console.warn("Keine Daten oder Container für Coh-Metrix Tabelle #" + key);
            }
        });
    } else {
        console.warn("cohmetrixTableDataByModel ist nicht definiert.");
    }
}










