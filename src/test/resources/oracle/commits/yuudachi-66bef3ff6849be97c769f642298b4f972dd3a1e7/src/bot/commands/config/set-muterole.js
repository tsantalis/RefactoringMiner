const { Command } = require('discord-akairo');

class SetMuteRole extends Command {
	constructor() {
		super('set-muted', {
			aliases: ['set-muterole', 'set-muted'],
			description: {
				content: 'Sets the mute role of the guild.',
				usage: '<role>',
				examples: ['set-muterole @Muted']
			},
			category: 'config',
			channel: 'guild',
			userPermissions: ['MANAGE_GUILD'],
			ratelimit: 2,
			args: [
				{
					id: 'role',
					match: 'content',
					type: 'role'
				}
			]
		});
	}

	exec(message, { role }) {
		this.client.settings.set(message.guild, 'muteRole', role.id);
		return message.util.reply(`set mute role to **${role.name}**`);
	}
}

module.exports = SetMuteRole;
