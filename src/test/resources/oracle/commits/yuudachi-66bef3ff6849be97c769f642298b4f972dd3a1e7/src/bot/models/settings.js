module.exports = (sequelize, DataTypes) =>
	sequelize.define('settings', {
		guild: {
			type: DataTypes.BIGINT,
			primaryKey: true,
			allowNull: false
		},
		settings: {
			type: DataTypes.JSONB,
			allowNull: false,
			defaultValue: {}
		}
	}, {
		timestamps: false
	});
