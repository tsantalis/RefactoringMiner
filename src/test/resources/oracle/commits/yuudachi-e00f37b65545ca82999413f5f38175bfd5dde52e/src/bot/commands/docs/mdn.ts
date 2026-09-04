import { Command } from 'discord-akairo';
import { Message, MessageEmbed } from 'discord.js';
import fetch from 'node-fetch';
import * as qs from 'querystring';
const Turndown = require('turndown');

export default class MDNCommand extends Command {
	public constructor() {
		super('mdn', {
			aliases: ['mdn', 'mozilla-developer-network'],
			category: 'docs',
			description: {
				content: 'Searches MDN for your query.',
				usage: '<query>',
				examples: ['Map', 'Map#get', 'Map.set']
			},
			regex: /^(?:mdn,) (.+)/i,
			clientPermissions: ['EMBED_LINKS'],
			args: [
				{
					id: 'query',
					prompt: {
						start: (message: Message) => `${message.author}, what would you like to search for?`
					},
					match: 'content',
					type: query => query ? query.replace(/#/g, '.prototype.') : null
				}
			]
		});
	}

	public async exec(message: Message, { query, match }: { query: string, match: any }) {
		if (!query && match) query = match[1];
		const queryString = qs.stringify({ q: query });
		const res = await fetch(`https://mdn.pleb.xyz/search?${queryString}`);
		const body = await res.json();
		if (!body.URL || !body.Title || !body.Summary) {
			return message.util!.reply("Yukikaze couldn't find the requested information. Maybe look for something that actually exists the next time!");
		}
		const turndown = new Turndown();
		turndown.addRule('hyperlink', {
			filter: 'a',
			replacement: (text: string, node: { href: string }) => `[${text}](https://developer.mozilla.org${node.href})`
		});
		const embed = new MessageEmbed()
			.setColor(0x066FAD)
			.setAuthor('MDN', 'https://i.imgur.com/DFGXabG.png', 'https://developer.mozilla.org/')
			.setURL(`https://developer.mozilla.org${body.URL}`)
			.setTitle(body.Title)
			.setDescription(turndown.turndown(body.Summary));

		return message.util!.send(embed);
	}
}
