const path = require('path');

module.exports = {
    entry: path.resolve(__dirname, '../resources/templates/js/tiptap-init.js'),
    output: {
        path: path.resolve(__dirname, 'src/main/resources/public/js/dist'),
        filename: 'tiptap-bundle.js',
        library: 'tiptap',
        libraryTarget: 'umd'
    },
    mode: 'production',
};
