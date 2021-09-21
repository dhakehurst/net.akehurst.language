// use this import statement to switch between the various examples
import {Test_SimpleExample} from "./test_SimpleExample";

const main = new Test_SimpleExample();
try {
    main.doIt();
} catch (e) {
    console.log("main: " + e.message);
}
