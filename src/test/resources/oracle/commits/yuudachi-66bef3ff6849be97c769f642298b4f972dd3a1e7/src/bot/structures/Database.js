const Sequelize = require('sequelize');
const { join } = require('path');

const sequelize = new Sequelize(process.env.DB, {
	logging: false,
	operatorsAliases: false
});

sequelize.import(join(__dirname, '..', 'models', 'settings'));
sequelize.import(join(__dirname, '..', 'models', 'tags'));
sequelize.import(join(__dirname, '..', 'models', 'role_states'));
sequelize.import(join(__dirname, '..', 'models', 'reminders'));
sequelize.import(join(__dirname, '..', 'models', 'cases'));

sequelize.sync();

module.exports = sequelize;
