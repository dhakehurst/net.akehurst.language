config.set({
    client: {
        captureConsole: false,
        mocha: {
            timeout: 30000
        }
    },
    browserDisconnectTimeout: 5000,
    browserConsoleLogOptions :{
        level: "debug"
    },
    logLevel: config.LOG_DEBUG,
});