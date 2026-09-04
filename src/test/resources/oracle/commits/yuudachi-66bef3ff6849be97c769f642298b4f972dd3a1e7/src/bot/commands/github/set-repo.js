const { Command } = require('discord-akairo');

class GitHubSetRepositoryCommand extends Command {
	constructor() {
		super('gh-set-repo', {
			aliases: ['set-repo', 'set-repository'],
			description: {
				content: 'Sets the repository the GitHub commands use.',
				usage: '<repo>',
				examples: ['1Computer1/discord-akairo', 'discordjs/discord.js']
			},
			category: 'github',
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

	exec(message, { repository }) {
		this.client.settings.set(message.guild, 'githubRepository', repository);
		return message.util.reply(`set repository to **${repository}**`);
	}
}

module.exports = GitHubSetRepositoryCommand;
