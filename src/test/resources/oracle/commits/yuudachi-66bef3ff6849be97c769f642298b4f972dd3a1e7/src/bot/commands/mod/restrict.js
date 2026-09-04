const { Command } = require('discord-akairo');
const { stripIndents } = require('common-tags');

class RestrictCommand extends Command {
	constructor() {
		super('restrict', {
			aliases: ['restrict'],
			description: {
				content: stripIndents`
					Restrict a members ability to post embeds/use custom emojis/react.

					Available restrictions:
					 • embed \`<member> <...reason>\`
					 • emoji \`<member> <...reason>\`
					 • reaction \`<member> <...reason>\`

					Required: \`<>\` | Optional: \`[]\`

					For additional \`<...arguments>\` usage refer to the examples below.
				`,
				usage: '<restriction> <...argumens>',
				examples: [
					'restrict img @Crawl nsfw',
					'restrict embed @Crawl img spam',
					'restrict emoji @Dim dumb',
					'restrict reaction @appellation why though'
				]
			},
			category: 'mod',
			channel: 'guild',
			clientPermissions: ['MANAGE_ROLES'],
			ratelimit: 2,
			args: [
				{
					id: 'restriction',
					type: ['embed', 'embeds', 'image', 'images', 'img', 'emoji', 'reaction', 'react']
				},
				{
					'id': 'rest',
					'match': 'rest',
					'default': '',
					'prompt': {
						start: message => `${message.author}, `,
						retry: message => `${message.author}, `
					}
				}
			]
		});
	}

	exec(message, { restriction, rest }) {
		if (!this.client.settings.get(message.guild, 'moderation')) {
			return message.reply('moderation commands are disabled on this server.');
		}
		if (!restriction) {
			const prefix = this.handler.prefix(message);
			return message.util.send(stripIndents`
				When you beg me so much I just can't not help you~
				Check \`${prefix}help restrict\` for more information.
			`);
		}
		const command = {
			embed: this.handler.modules.get('restrict-embed'),
			embeds: this.handler.modules.get('restrict-embed'),
			image: this.handler.modules.get('restrict-embed'),
			images: this.handler.modules.get('restrict-embed'),
			img: this.handler.modules.get('restrict-embed'),
			emoji: this.handler.modules.get('restrict-emoji'),
			reaction: this.handler.modules.get('restrict-reaction'),
			react: this.handler.modules.get('restrict-reaction')
		}[restriction];

		return this.handler.handleDirectCommand(message, rest, command, true);
	}
}

module.exports = RestrictCommand;
