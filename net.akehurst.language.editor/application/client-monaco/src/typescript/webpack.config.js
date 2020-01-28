const path = require('path');
const HtmlWebPackPlugin = require('html-webpack-plugin');
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin');

const isDevelopment = process.env.NODE_ENV !== 'production';

module.exports = {
    entry: './src/index.ts',
    devtool: 'source-map',
    module: {
        rules: [
            {
                test: /\.css$/,
                use: ['style-loader', 'css-loader'],
            },
            {
                test: /\.ts$/,
                use: 'ts-loader',
                exclude: [/node_modules/],
            },
            {
                test: /\.html$/,
                use: [
                    {
                        loader: 'html-loader',
                        options: {minimize: !isDevelopment}
                    }
                ]
            },
            {
                test: /\.ttf$/,
                use: ['file-loader']
            }
        ],
    },
    resolve: {
        extensions: ['.ts', '.js', '.css'],
    },
    plugins: [
        new HtmlWebPackPlugin({
            template: './src/index.html',
            filename: './index.html'
        }),
        new MonacoWebpackPlugin({
            languages: []
        })
    ]
};