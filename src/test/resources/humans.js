//!ES6
import Human from './human';

class Humans {

    constructor (args) {

        console.log("===Humans Collection Constructor===");

        this.model = Human;
        this.url = "/humans";
    }
}

export default Humans;