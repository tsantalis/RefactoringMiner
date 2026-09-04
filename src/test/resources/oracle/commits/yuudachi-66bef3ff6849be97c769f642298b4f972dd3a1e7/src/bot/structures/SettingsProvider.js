const { SequelizeProvider } = require('discord-akairo');
const { Guild } = require('discord.js');

class SettingsProvider extends SequelizeProvider {
	constructor(table) {
		super(table, {
			idColumn: 'guild',
			dataColumn: 'settings'
		});
	}

	get(guild, key, defaultValue) {
		const id = this.constructor.getGuildId(guild);
		return super.get(id, key, defaultValue);
	}

	set(guild, key, value) {
		const id = this.constructor.getGuildId(guild);
		return super.set(id, key, value);
	}

	delete(guild, key) {
		const id = this.constructor.getGuildId(guild);
		return super.delete(id, key);
	}

	clear(guild) {
		const id = this.constructor.getGuildId(guild);
		return super.clear(id);
	}

	static getGuildId(guild) {
		if (guild instanceof Guild) return guild.id;
		if (guild === 'global' || guild === null) return '0';
		if (typeof guild === 'string' && /^\d+$/.test(guild)) return guild;
		throw new TypeError('Invalid guild specified. Must be a Guild instance, guild ID, "global", or null.');
	}
}

module.exports = SettingsProvider;
