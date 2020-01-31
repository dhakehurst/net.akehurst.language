const path = require('path');
const UglifyJsPlugin = require('uglifyjs-webpack-plugin');
const CopyPkgJsonPlugin = require("copy-pkg-json-webpack-plugin");

const isDevelopment = process.env.NODE_ENV !== 'production';

module.exports = {
    entry: './src/AglEditorAce.ts',
    output: {
        path: './dist',
        library: 'AglEditorAce'
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
                    loader:'ts-loader',
                    options: {
                    }
                }],
                exclude: [/node_modules/],
            }
        ],
    },
    resolve: {
        extensions: ['.ts', '.js'],
    },
    plugins: [
        new UglifyJsPlugin({
            exclude: /node_modules/,
            sourceMap: true,
            uglifyOptions: {
                compress: {},
                mangle: true,
            }
        }),
        new CopyPkgJsonPlugin({
            remove: ['devDependencies']
        })
    ]
};