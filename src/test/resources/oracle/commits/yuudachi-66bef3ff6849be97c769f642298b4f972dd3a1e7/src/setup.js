require('dotenv').config();
const sequelize = require('./bot/structures/Database');

sequelize.sync();
