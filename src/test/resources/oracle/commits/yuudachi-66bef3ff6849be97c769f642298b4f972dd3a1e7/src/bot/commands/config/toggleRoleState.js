const { Command } = require('discord-akairo');

class ToggleRoleStateCommand extends Command {
	constructor() {
		super('toggle-role-state', {
			aliases: ['role-state'],
			description: {
				content: 'Toggle role state on the server.'
			},
			category: 'config',
			channel: 'guild',
			userPermissions: ['MANAGE_GUILD'],
			ratelimit: 2
		});
	}

	async exec(message) {
		const roleState = this.client.settings.get(message.guild, 'roleState');
		if (roleState) {
			this.client.settings.set(message.guild, 'roleState', false);
			const users = await this.client.db.models.role_states.findAll({ where: { guild: message.guild.id } });
			for (const user of users) await user.destroy();

			return message.util.reply('successfully removed all records!');
		}
		this.client.settings.set(message.guild, 'roleState', true);
		const members = await message.guild.members.fetch();
		const records = [];
		for (const member of members.values()) {
			records.push({
				guild: message.guild.id,
				user: member.id,
				roles: member.roles.filter(role => role.id !== message.guild.id).map(role => role.id)
			});
		}
		await this.client.db.models.role_states.bulkCreate(records.filter(record => record.roles.length));

		return message.util.reply('successfully inserted all the records!');
	}
}

module.exports = ToggleRoleStateCommand;
