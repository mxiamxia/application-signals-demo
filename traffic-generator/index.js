const axios = require('axios');
const cron = require('node-cron');

const baseUrl = process.env.URL || 'http://your-sample-app-end-point';
const highLoadMaxRequests = parseInt(process.env.HIGH_LOAD_MAX, 10) || 1200;
const highLoadMinRequests = parseInt(process.env.HIGH_LOAD_MIN, 10) || 600;
const burstMaxDelay = parseInt(process.env.BURST_DELAY_MAX, 10) || 200;
const burstMinDelay = parseInt(process.env.BURST_DELAY_MIN, 10) || 100;
const lowLoadMaxRequests = parseInt(process.env.LOW_LOAD_MAX, 10) || 40;
const lowLoadMinRequests = parseInt(process.env.LOW_LOAD_MIN, 10) || 20;

const pets = new Map([
    [1, 1],
    [2, 2],
    [3, 3],
    [4, 3],
    [5, 4],
    [6, 5],
    [7, 6],
    [8, 6],
    [9, 7],
    [10, 8],
    [11, 9],
    [12, 10],
    [13, 10]
  ]);

function getRandomNumber(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

const postPaymentData = (url, amount, notes) => {
    const data = {
        amount: amount,
        notes: notes
    };

    return axios.post(url, data, { timeout: 20000 });
}

const postVisitData = (url, date, description) => {
    const data = {
        date: date,
        description: description
    };

    return axios.post(url, data, { timeout: 20000 });
}


function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

const lowTrafficTask = cron.schedule('* * * * *', () => {
    const lowLoad = getRandomNumber(lowLoadMinRequests, lowLoadMaxRequests);
    for (let i = 0; i < lowLoad; i++) {
        console.log('send low load traffic: ' + (i + 1))
        sleep(2 * 1000)
        const pet = getRandomNumber(1, 13);
        const owner = pets.get(pet);
        const url = `${baseUrl}/api/visit/owners/${owner}/pets/${pet}/visits`;
        postVisitData(url, '2023-08-01', `low-traffic-visit-${i + 1}`)
            .catch(err => {
                console.error("Failed to post " + url + ". Error: " + (err.response ? err.response.data : err.message));

            }); // Catch and log errors
        axios.get(`${baseUrl}/api/gateway/owners/1`, { timeout: 20000 })
            .catch(err => {
                console.error("Failed to get " + url + ". Error: " + (err.response ? err.response.data : err.message));
            }); // Catch and log errors
    }
}, { scheduled: false });

lowTrafficTask.start();

const generateHighLoad = async () => {
    const highLoad = getRandomNumber(highLoadMinRequests, highLoadMaxRequests);
    for (let i = 0; i < highLoad; i++) {
        console.log('send high traffic: ' + (i + 1))
        const pet = getRandomNumber(1, 13);
        const owner = pets.get(pet);
        const url = `${baseUrl}/api/visit/owners/${owner}/pets/${pet}/visits`;
        postVisitData(url, '2023-08-08', `high-traffic-visit-${i + 1}`)
            .catch(err => {
                console.error("Failed to post " + url + ". Error: " + (err.response ? err.response.data : err.message));
            }); // Catch and log errors
    }
    scheduleHighLoad();  // Schedule the next high load
}

const scheduleHighLoad = () => {
    const delay = getRandomNumber(burstMinDelay, burstMaxDelay) * 60 * 1000;
    setTimeout(generateHighLoad, delay);
}

// Start with a high load
scheduleHighLoad();


const invalidRequestTask = cron.schedule('*/5 * * * *', () => {
    const lowLoad = getRandomNumber(2, 5);
    for (let i = 0; i < lowLoad; i++) {
        sleep(2*1000);
        console.log('send invalid traffic: ' + (i + 1))
        axios.get(`${baseUrl}/api/gateway/owners/-1`, { timeout: 20000 })
            .catch(err => {
                console.error("Failed to get /api/gateway/owners/-1, error: " + (err.response ? err.response.data : err.message));
            }); // Catch and log errors
    }
}, { scheduled: false });

invalidRequestTask.start();

const bedrockRequestTask = cron.schedule('* * * * *', () => {
    const lowLoad = getRandomNumber(1, 2);
    for (let i = 0; i < lowLoad; i++) {
        sleep(5*1000);
        console.log('calling bedrock: ' + (i + 1))
        axios.get(`${baseUrl}/api/customer/diagnose/owners/1/pets/1`, { timeout: 30000 })
            .catch(err => {
                console.error("Failed to get /api/customer/diagnose/owners/1/pets/1, error: " + (err.response ? err.response.data : err.message));
            }); // Catch and log errors
    }
}, { scheduled: false });

bedrockRequestTask.start();


const createOwnerLowTrafficTask = cron.schedule('*/2 * * * *', () => {
    const lowLoad = 2;
    for (let i = 0; i < lowLoad; i++) {
        console.log('create owner low traffic: ' + (i + 1))
        sleep(2 * 1000)
        const data = { firstName: "random-traffic", address: "A", city: "B", telephone: "123489067542", lastName: "NA" }
        axios.post(`${baseUrl}/api/customer/owners`, data, { timeout: 20000 })
            .catch(err => {
                console.error("Failed to post /api/customer/owners, error: " + (err.response ? err.response.data : err.message));
            }); // Catch and log errors
    }
}, { scheduled: false });

createOwnerLowTrafficTask.start();



const createOwnerHighTrafficTask = cron.schedule('*/2 * * * *', () => {
    const highLoad = getRandomNumber(50, 80);
    sleep(getRandomNumber(1,2)*60*1000);
    for (let i = 0; i < highLoad; i++) {
        console.log('create owner high traffic: ' + (i + 1))
        sleep(3 * 1000)
        const data = { firstName: "random-traffic", address: "A", city: "B", telephone: "123489067542", lastName: "NA" }
        axios.post(`${baseUrl}/api/customer/owners`, data, { timeout: 20000 })
            .catch(err => {
                console.error("Failed to post /api/customer/owners, error: " + (err.response ? err.response.data : err.message));
            }); // Catch and log errors
    }
}, { scheduled: false });

createOwnerHighTrafficTask.start();

const postPetsLowTrafficTask = cron.schedule('*/2 * * * *', () => {
    const lowLoad = getRandomNumber(lowLoadMinRequests, lowLoadMaxRequests);
    for (let i = 0; i < lowLoad; i++) {
        console.log('send low load pet traffic: ' + (i + 1))

        console.log('add 1 pet every 2 minutes');
        const name = "lastName" + new Date().toLocaleTimeString();
        const data = {"id":0,"name":name ,"birthDate":"2023-11-20T08:00:00.000Z","typeId":"1"}
        axios.post(`${baseUrl}/api/customer/owners/7/pets`, data, { timeout: 20000 })
            .catch(err => {
                console.error("Failed to post /api/customer/owners/7/pets, error: " + (err.response ? err.response.data : err.message));
            }); // Catch and log errors
    }
   
}, { scheduled: false });

postPetsLowTrafficTask.start();

const postPetsHighTrafficTask = cron.schedule('0 * * * *', async () => {
sleepMins = getRandomNumber(1,10);
console.log(`sleep ${sleepMins} minutes`);
await sleep(sleepMins*60*1000);
console.log('add 2 pets within 1 minute');
for (let i = 0; i < 2; i++) {
    console.log('add 2 pets within 1 minute');
    const name = "lastName" + new Date().toLocaleTimeString();
    const data = {"id": 0, "name": name, "birthDate": "2023-11-20T08:00:00.000Z", "typeId": "2"}
    await axios.post(`${baseUrl}/api/customer/owners/7/pets`, data, {timeout: 20000})
        .catch(err => {
            console.error("Failed to post /api/customer/owners/7/pets, error: " + (err.response ? err.response.data : err.message));
        }); // Catch and log errors
}
}, { scheduled: false });

postPetsHighTrafficTask.start();

const lowTrafficPaymentTask = cron.schedule('* * * * *', () => {
    const lowLoad = getRandomNumber(lowLoadMinRequests, lowLoadMaxRequests);
    for (let i = 0; i < lowLoad; i++) {
        console.log('send low load payment traffic: ' + (i + 1))
        const amount = getRandomNumber(1,111)
        const pet = getRandomNumber(1, 13);
        const owner = pets.get(pet);
        const url = `${baseUrl}/api/payments/owners/${owner}/pets/${pet}`;
        postPaymentData(url, amount, `low-traffic-payment`)
            .catch(err => {
                console.error("Failed to post to " + url + ". Error: " + (err.response ? err.response.data : err.message));
    
            }); // Catch and log errors
        axios.get(url, { timeout: 20000 })
            .catch(err => {
                console.error("Failed to get " + url + ". Error: " + (err.response ? err.response.data : err.message));
            }); // Catch and log errors
    }
   
}, { scheduled: false });

lowTrafficPaymentTask.start();

const clearPaymentTableTask = cron.schedule('0 */1 * * *', () => {
    console.log('clear payment table every 1 hour');
    axios.delete(`${baseUrl}/api/payments/clean-db`, { timeout: 20000 })
        .catch(err => {
            console.error(`${baseUrl}/api/payments/clean-db, error: ` + (err.response ? err.response.data : err.message));
        }); // Catch and log errors
}, { scheduled: false });

clearPaymentTableTask.start();