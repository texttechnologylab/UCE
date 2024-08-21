
/*
 * Gets the next token branches from a given input
 * @param {} input 
 * @param {*} callback 
 */
async function getNextTokenBranches(input, overwriteK = -1) {
    const result = await $.ajax({
        type: 'POST',
        contentType: 'application/json',
        url: '/api/tokens/next',
        data: JSON.stringify({ input: input, overwriteK: overwriteK }),
        error: function (error) {
            console.log(error);
        }
    });
    return result.result;
}

export { getNextTokenBranches };