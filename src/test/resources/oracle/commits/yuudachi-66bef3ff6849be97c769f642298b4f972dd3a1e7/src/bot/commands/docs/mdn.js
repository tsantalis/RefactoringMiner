const { Command } = require('discord-akairo');
const { MessageEmbed } = require('discord.js');
const fetch = require('node-fetch');
const qs = require('querystring');
const Turndown = require('turndown');

class MDNCommand extends Command {
	constructor() {
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
						start: message => `${message.author}, what would you like to search for?`
					},
					match: 'content',
					type: query => query ? query.replace(/#/g, '.prototype.') : null // eslint-disable-line no-confusing-arrow
				}
			]
		});
	}

	async exec(message, { query, match }) {
		if (!query && match) query = match[1]; // eslint-disable-line prefer-destructuring
		const queryString = qs.stringify({ q: query });
		const res = await fetch(`https://mdn.topkek.pw/search?${queryString}`);
		const body = await res.json();
		if (!body.URL || !body.Title || !body.Summary) {
			return message.util.reply("Yukikaze couldn't find the requested information. Maybe look for something that actually exists the next time!");
		}
		const turndown = new Turndown();
		turndown.addRule('hyperlink', {
			filter: 'a',
			replacement: (text, node) => `[${text}](https://developer.mozilla.org${node.href})`
		});
		const embed = new MessageEmbed()
			.setColor(0x066FAD)
			.setAuthor('MDN', 'https://i.imgur.com/DFGXabG.png', 'https://developer.mozilla.org/')
			.setURL(`https://developer.mozilla.org${body.URL}`)
			.setTitle(body.Title)
			.setDescription(turndown.turndown(body.Summary));

		return message.util.send(embed);
	}
}

module.exports = MDNCommand;
