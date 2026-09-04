module.exports = (sequelize, DataTypes) =>
	sequelize.define('role_states', {
		guild: {
			type: DataTypes.BIGINT,
			allowNull: false
		},
		user: {
			type: DataTypes.BIGINT,
			allowNull: false
		},
		roles: {
			type: DataTypes.ARRAY(DataTypes.STRING), // eslint-disable-line new-cap
			allowNull: false,
			defaultValue: []
		}
	}, {
		timestamps: false,
		indexes: [
			{ fields: ['guild'] },
			{ fields: ['user'] },
			{ fields: ['guild', 'user'], unique: true }
		]
	});
