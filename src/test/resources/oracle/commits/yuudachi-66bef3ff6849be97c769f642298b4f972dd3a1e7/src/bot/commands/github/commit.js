const { Argument, Command } = require('discord-akairo');
const { MessageEmbed } = require('discord.js');
const fetch = require('node-fetch');
const { oneLine, stripIndents } = require('common-tags');

const { GITHUB_API_KEY } = process.env;

class GitHubCommitCommand extends Command {
	constructor() {
		super('gh-commit', {
			aliases: ['gh-commit', 'commit'],
			description: {
				content: 'Get information on a commit in a predefined repository.',
				usage: '<commit>',
				examples: ['8335f499c5e0cfac1267d426d854a2209416595f', 'd9f772cdc1139b9a118be4321fe719ffa0dfc2fa']
			},
			regex: /\bgc#[a-f0-9]{40}$/i,
			category: 'github',
			channel: 'guild',
			clientPermissions: ['EMBED_LINKS'],
			ratelimit: 2,
			args: [
				{
					id: 'commit',
					match: 'content',
					type: Argument.validate('string', str => str.length >= 40)
				}
			]
		});
	}

	async exec(message, args) {
		if (!GITHUB_API_KEY) {
			return message.util.reply(oneLine`
				my master has not set a valid GitHub API key,
				therefore this command is not available.
			`);
		}
		const repository = this.client.settings.get(message.guild, 'githubRepository');
		if (!repository) return message.reply("the guild owner didn't set a GitHub repository yet.");
		const owner = repository.split('/')[0];
		const repo = repository.split('/')[1];
		const commit = args.match ? args.match[0].split('#')[1] : args.commit;
		let body;
		try {
			const res = await fetch(`https://api.github.com/repos/${owner}/${repo}/commits/${commit}`,
				{ headers: { Authorization: `token ${GITHUB_API_KEY}` } });
			body = await res.json();
		} catch (error) {
			return message.util.reply("Yukikaze couldn't find the requested information. Maybe look for something that actually exists the next time!");
		}
		if (!body || !body.commit) {
			return message.util.reply("Yukikaze couldn't find the requested information. Maybe look for something that actually exists the next time!");
		}
		const embed = new MessageEmbed()
			.setColor(3447003)
			.setAuthor(
				body.author ? body.author.login ? body.author.login : 'Unknown' : 'Unknown',
				body.author ? body.author.avatar_url ? body.author.avatar_url : '' : '',
				body.author ? body.author.html_url ? body.author.html_url : '' : ''
			)
			.setTitle(body.commit.message.split('\n')[0])
			.setURL(body.html_url)
			.setDescription(
				`${body.commit.message
					.replace('\r', '')
					.replace('\n\n', '\n')
					.split('\n')
					.slice(1)
					.join('\n')
					.substring(0, 300)} ...
			`
			)
			.addField(
				'Stats',
				stripIndents`
					• Total: ${body.stats.total}
					• Additions: ${body.stats.additions}
					• Deletions: ${body.stats.deletions}
				`,
				true
			)
			.addField(
				'Committer',
				body.committer ? `• [**${body.committer.login}**](${body.committer.html_url})` : 'Unknown',
				true
			)
			.setThumbnail(body.author ? body.author.avatar_url : '')
			.setTimestamp(new Date(body.commit.author.date));

		if (!message.channel.permissionsFor(message.guild.me).has(['ADD_REACTIONS', 'MANAGE_MESSAGES'], false)) {
			return message.util.send(embed);
		}
		const msg = await message.util.send(embed);
		msg.react('🗑');
		let react;
		try {
			react = await msg.awaitReactions(
				(reaction, user) => reaction.emoji.name === '🗑' && user.id === message.author.id,
				{ max: 1, time: 10000, errors: ['time'] }
			);
		} catch (error) {
			msg.reactions.removeAll();

			return message;
		}
		react.first().message.delete();

		return message;
	}
}

module.exports = GitHubCommitCommand;
