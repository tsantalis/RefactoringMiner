const { Command } = require('discord-akairo');
const { MessageEmbed } = require('discord.js');
const fetch = require('node-fetch');
const { oneLine, stripIndents } = require('common-tags');

const { GITHUB_API_KEY } = process.env;

class GitHubSearchCommand extends Command {
	constructor() {
		super('gh-search', {
			aliases: ['gh-search'],
			description: {
				content: 'Get info on an issue or PR from a repository.',
				usage: '<commit/pr/issue>',
				examples: ['1Computer1/discord-akairo#1', 'discordjs/discord.js#1', 'discordjs.discord.js#24']
			},
			category: 'github',
			channel: 'guild',
			clientPermissions: ['EMBED_LINKS'],
			ratelimit: 2,
			args: [
				{
					id: 'repo',
					type: 'string'
				},
				{
					'id': 'commit',
					'type': 'string',
					'default': ''
				}
			]
		});
	}

	async exec(message, { repo, commit }) {
		// eslint-disable-line complexity
		if (!GITHUB_API_KEY) {
			return message.reply(oneLine`
				my master has not set a valid GitHub API key,
				therefore this command is not available.
			`);
		}
		const owner = repo.split('/')[0];
		if (commit.match(/[a-f0-9]{40}$/i)) {
			const repository = repo.split('/')[1];
			let body;
			try {
				const res = await fetch(`https://api.github.com/repos/${owner}/${repository}/commits/${commit}`,
					{ headers: { Authorization: `token ${GITHUB_API_KEY}` } });
				body = await res.json();
			} catch (error) {
				return message.util.reply("Yukikaze couldn't find the requested information. Maybe look for something that actually exists the next time!");
			}
			if (!body) {
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

			if (message.guild && !message.channel.permissionsFor(this.client.user).has(['ADD_REACTIONS', 'MANAGE_MESSAGES'], false)) {
				return message.util.send(embed);
			}
			const msg = await message.util.send(embed);
			const ownReaction = await msg.react('🗑');
			let react;
			try {
				react = await msg.awaitReactions(
					// eslint-disable-line no-redeclare
					(reaction, user) => reaction.emoji.name === '🗑' && user.id === message.author.id,
					{ max: 1, time: 10000, errors: ['time'] }
				);
			} catch (error) {
				if (message.guild) msg.clearReactions();
				else ownReaction.remove();

				return message;
			}
			react.first().message.delete();

			return message;
		}
		let repository = repo.split('/')[1];
		if (!repository) return;
		[repository] = repository.split('#');
		const number = repo.split('#')[1];
		if (!number) return;
		const query = `
			{
				repository(owner: "${owner}", name: "${repository}") {
					name
					issueOrPullRequest(number: ${number}) {
						... on PullRequest {
							comments {
								totalCount
							}
							commits(last: 1) {
								nodes {
									commit {
										oid
									}
								}
							}
							author {
								avatarUrl
								login
								url
							}
							body
							labels(first: 10) {
								nodes {
									name
								}
							}
							merged
							number
							publishedAt
							state
							title
							url
						}
						... on Issue {
							comments {
								totalCount
							}
							author {
								avatarUrl
								login
								url
							}
							body
							labels(first: 10) {
								nodes {
									name
								}
							}
							number
							publishedAt
							state
							title
							url
						}
					}
				}
			}
		`;
		let body;
		try {
			const res = await fetch('https://api.github.com/graphql', {
				method: 'POST',
				headers: { Authorization: `Bearer ${GITHUB_API_KEY}` },
				body: JSON.stringify({ query })
			});
			body = await res.json();
		} catch (error) {
			return message.util.reply("Yukikaze couldn't find the requested information. Maybe look for something that actually exists the next time!");
		}
		if (!body || !body.data || !body.data.repository) {
			return message.util.reply("Yukikaze couldn't find the requested information. Maybe look for something that actually exists the next time!");
		}
		const data = body.data.repository.issueOrPullRequest;
		const embed = new MessageEmbed()
			.setColor(data.merged ? 0x9c27b0 : data.state === 'OPEN' ? 0x43a047 : 0xef6c00)
			.setAuthor(
				data.author ? data.author.login ? data.author.login : 'Unknown' : 'Unknown',
				data.author ? data.author.avatarUrl ? data.author.avatarUrl : '' : '',
				data.author ? data.author.url ? data.author.url : '' : ''
			)
			.setTitle(data.title)
			.setURL(data.url)
			.setDescription(`${data.body.substring(0, 500)} ...`)
			.addField('State', data.state, true)
			.addField('Comments', data.comments.totalCount, true)
			.addField('Repo & Number', `${body.data.repository.name}#${data.number}`, true)
			.addField('Type', data.commits ? 'PULL REQUEST' : 'ISSUE', true)
			.addField(
				'Labels',
				data.labels.nodes.length ? data.labels.nodes.map(node => node.name) : 'NO LABEL(S)',
				true
			)
			.setThumbnail(data.author ? data.author.avatarUrl : '')
			.setTimestamp(new Date(data.publishedAt));
		if (data.commits) {
			embed.addField(
				'Install with',
				`\`npm i discordjs/discord.js#${data.commits.nodes[0].commit.oid.substring(0, 12)}\``
			);
		}

		if (message.guild && !message.channel.permissionsFor(this.client.user).has(['ADD_REACTIONS', 'MANAGE_MESSAGES'], false)) {
			return message.util.send(embed);
		}
		const msg = await message.util.send(embed);
		const ownReaction = await msg.react('🗑');
		let react;
		try {
			react = await msg.awaitReactions(
				// eslint-disable-line no-redeclare
				(reaction, user) => reaction.emoji.name === '🗑' && user.id === message.author.id,
				{ max: 1, time: 10000, errors: ['time'] }
			);
		} catch (error) {
			if (message.guild) msg.reactions.removeAll();
			else ownReaction.remove();

			return message;
		}
		react.first().message.delete();

		return message;
	}
}

module.exports = GitHubSearchCommand;
