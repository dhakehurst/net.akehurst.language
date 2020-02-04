const path = require('path');
const CopyPkgJsonPlugin = require("copy-pkg-json-webpack-plugin");

const isDevelopment = process.env.NODE_ENV !== 'production';

module.exports = {
    entry: path.resolve('./src/AglEditorAce.ts'),
    output: {
        library: 'AglEditorAce',
        libraryTarget: 'umd'
    },
    devtool: 'source-map',
    module: {
        rules: [
            {
                test: /\.css$/,
                use: ['style-loader', 'css-loader'],
            },
            {
                test: /\.ts$/,
                use: [{
                    loader:'awesome-typescript-loader',
                    options: {
                    }
                }],
                exclude: [/node_modules/],
            }
        ],
    },
    resolve: {
        extensions: ['.ts', '.js', ".json"],
    },
    externals:[
        'ace-builds/src-noconflict/ace',
        'ace-builds/src-noconflict/ext-language_tools',
        'net.akehurst.language-agl-processor'
    ],
    plugins: [
        new CopyPkgJsonPlugin({
            remove: ['devDependencies']
        })
    ]
};