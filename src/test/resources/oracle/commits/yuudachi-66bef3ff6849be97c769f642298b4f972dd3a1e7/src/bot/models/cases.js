module.exports = (sequelize, DataTypes) =>
	sequelize.define('cases', {
		guild: {
			type: DataTypes.BIGINT,
			allowNull: false
		},
		message: {
			type: DataTypes.BIGINT
		},
		case_id: {
			type: DataTypes.INTEGER,
			allowNull: false
		},
		ref_id: {
			type: DataTypes.INTEGER
		},
		target_id: {
			type: DataTypes.BIGINT,
			allowNull: false
		},
		target_tag: {
			type: DataTypes.TEXT,
			allowNull: false
		},
		mod_id: {
			type: DataTypes.BIGINT
		},
		mod_tag: {
			type: DataTypes.TEXT
		},
		action: {
			type: DataTypes.INTEGER,
			allowNull: false
		},
		reason: {
			type: DataTypes.TEXT
		},
		action_duration: {
			type: DataTypes.DATE
		},
		action_processed: {
			type: DataTypes.BOOLEAN,
			defaultValue: true
		},
		createdAt: {
			type: DataTypes.DATE,
			defaultValue: DataTypes.NOW
		}
	}, {
		timestamps: false
	});
