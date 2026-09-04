const { Command } = require('discord-akairo');

class ToggleModerationCommand extends Command {
	constructor() {
		super('toggle-moderation', {
			aliases: ['moderation'],
			description: {
				content: 'Toggle moderation features on the server.'
			},
			category: 'config',
			channel: 'guild',
			userPermissions: ['MANAGE_GUILD'],
			ratelimit: 2
		});
	}

	exec(message) {
		const moderation = this.client.settings.get(message.guild, 'moderation');
		if (moderation) {
			this.client.settings.set(message.guild, 'moderation', false);
			return message.util.reply('disabled moderation commands!');
		}
		this.client.settings.set(message.guild, 'moderation', true);

		return message.util.reply('activated moderation commands!');
	}
}

module.exports = ToggleModerationCommand;
