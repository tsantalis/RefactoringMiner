import { Command } from 'discord-akairo';
import { Message } from 'discord.js';

export default class ToggleModerationCommand extends Command {
	public constructor() {
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

	public exec(message: Message) {
		const moderation = this.client.settings.get(message.guild, 'moderation', undefined);
		if (moderation) {
			this.client.settings.set(message.guild, 'moderation', false);
			return message.util!.reply('disabled moderation commands!');
		}
		this.client.settings.set(message.guild, 'moderation', true);

		return message.util!.reply('activated moderation commands!');
	}
}
