const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin');
//if (!config.plugins) config.module.plugins = [];
config.plugins.push(
    new MonacoWebpackPlugin({
        languages: []
    })
);