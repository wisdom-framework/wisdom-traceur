// greeter.js
//!ES6
class Greeter {
    sayHi(name = 'Anonymous') {
        console.log(`Hi ${name}!`);
    }
}

var greeter = new Greeter();
greeter.sayHi();