import { Command } from 'discord-akairo';
import { Message, Role } from 'discord.js';

export default class SetMuteRole extends Command {
	public constructor() {
		super('set-muted', {
			aliases: ['set-muterole', 'set-muted'],
			description: {
				content: 'Sets the mute role of the guild.',
				usage: '<role>',
				examples: ['@Muted', 'Muted']
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

	public exec(message: Message, { role }: { role: Role }) {
		this.client.settings.set(message.guild, 'muteRole', role.id);
		return message.util!.reply(`set mute role to **${role.name}**`);
	}
}
