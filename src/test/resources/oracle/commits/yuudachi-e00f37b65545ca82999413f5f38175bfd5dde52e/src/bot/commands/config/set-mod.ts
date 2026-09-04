import { Command } from 'discord-akairo';
import { Message, Role } from 'discord.js';

export default class SetModRoleCommand extends Command {
	public constructor() {
		super('set-mod', {
			aliases: ['set-mod', 'mod-role'],
			description: {
				content: 'Sets the mod role many of the commands use for permission checking.',
				usage: '<role>',
				examples: ['@Mod', 'Mods']
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
		this.client.settings.set(message.guild, 'modRole', role.id);
		return message.util!.reply(`set moderation role to **${role.name}**`);
	}
}
