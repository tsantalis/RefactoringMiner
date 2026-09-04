import { Command } from 'discord-akairo';
import { Message } from 'discord.js';
import { stripIndents } from 'common-tags';

export default class RestrictCommand extends Command {
	public constructor() {
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
					'img @Crawl nsfw',
					'embed @Crawl img spam',
					'emoji @Dim dumb',
					'reaction @appellation why though'
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
						start: (message: Message) => `${message.author}, `,
						retry: (message: Message) => `${message.author}, `
					}
				}
			]
		});
	}

	public exec(message: Message, { restriction, rest }: { restriction: string, rest: string }) {
		if (!this.client.settings.get(message.guild, 'moderation', undefined)) {
			return message.reply('moderation commands are disabled on this server.');
		}
		if (!restriction) {
			// @ts-ignore
			const prefix = this.handler.prefix(message);
			return message.util!.send(stripIndents`
				When you beg me so much I just can't not help you~
				Check \`${prefix}help restrict\` for more information.
			`);
		}
		const command = ({
			embed: this.handler.modules.get('restrict-embed'),
			embeds: this.handler.modules.get('restrict-embed'),
			image: this.handler.modules.get('restrict-embed'),
			images: this.handler.modules.get('restrict-embed'),
			img: this.handler.modules.get('restrict-embed'),
			emoji: this.handler.modules.get('restrict-emoji'),
			reaction: this.handler.modules.get('restrict-reaction'),
			react: this.handler.modules.get('restrict-reaction')
		} as { [key: string]: Command })[restriction];

		return this.handler.handleDirectCommand(message, rest, command, true);
	}
}
