import { Command } from 'discord-akairo';
import { Message } from 'discord.js';

export default class SetGitHubRepositoryCommand extends Command {
	public constructor() {
		super('gh-set-repo', {
			aliases: ['set-repo', 'set-repository'],
			description: {
				content: 'Sets the repository the GitHub commands use.',
				usage: '<repo>',
				examples: ['1Computer1/discord-akairo', 'discordjs/discord.js']
			},
			category: 'config',
			channel: 'guild',
			userPermissions: ['MANAGE_GUILD'],
			ratelimit: 2,
			args: [
				{
					id: 'repository',
					type: 'string'
				}
			]
		});
	}

	public exec(message: Message, { repository }: { repository: string }) {
		this.client.settings.set(message.guild, 'githubRepository', repository);
		return message.util!.reply(`set repository to **${repository}**`);
	}
}
