const Fotmob = require('fotmob').default;
const fotmob = new Fotmob();

fotmob.getTeam(9825).then(res => {
    console.log(JSON.stringify(res.fixtures.allFixtures.fixtures[0]));
}).catch(console.error);
