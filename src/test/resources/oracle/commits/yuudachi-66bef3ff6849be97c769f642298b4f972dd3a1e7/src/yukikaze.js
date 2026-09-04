require('dotenv').config();
const YukikazeClient = require('./bot/client/YukikazeClient');

const client = new YukikazeClient({ owner: process.env.OWNERS, token: process.env.TOKEN });

client
	.on('error', err => client.logger.error(`Error:\n${err.stack}`))
	.on('warn', warn => client.logger.warn(`Warning:\n${warn}`));

client.start();
