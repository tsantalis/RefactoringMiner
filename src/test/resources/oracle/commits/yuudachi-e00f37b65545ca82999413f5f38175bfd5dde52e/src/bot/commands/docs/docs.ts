import { Command } from 'discord-akairo';
import { Message, TextChannel } from 'discord.js';
import fetch from 'node-fetch';
import * as qs from 'querystring';

export default class DocsCommand extends Command {
	public constructor() {
		super('docs', {
			aliases: ['docs'],
			description: {
				content: 'Searches discord.js documentation.',
				usage: '<query>',
				examples: ['TextChannel', 'Client', 'ClientUser#setActivity master']
			},
			category: 'docs',
			clientPermissions: ['EMBED_LINKS'],
			ratelimit: 2,
			args: [
				{
					id: 'query',
					match: 'rest',
					type: 'lowercase',
					prompt: {
						start: (message: Message) => `${message.author}, what would you like to search?`
					}
				},
				{
					id: 'force',
					match: 'flag',
					flag: ['--force', '-f']
				}
			]
		});
	}

	public async exec(message: Message, { query, force }: { query: any, force: boolean }) {
		query = query.split(' ');
		let project = 'main';
		let branch = ['stable', 'master', 'rpc', 'commando'].includes(query.slice(-1)[0]) ? query.pop() : 'stable';
		if (['rpc', 'commando'].includes(branch)) {
			project = branch;
			branch = 'master';
		}
		const queryString = qs.stringify({ q: query.join(' '), force });
		const res = await fetch(`https://djsdocs.sorta.moe/${project}/${branch}/embed?${queryString}`);
		const embed = await res.json();
		if (!embed) {
			return message.util!.reply("Yukikaze couldn't find the requested information. Maybe look for something that actually exists the next time!");
		}
		if (message.channel.type === 'dm' || !(message.channel as TextChannel).permissionsFor(message.guild.me)!.has(['ADD_REACTIONS', 'MANAGE_MESSAGES'], false)) {
			return message.util!.send({ embed });
		}
		const msg = await message.util!.send({ embed }) as Message;
		msg.react('🗑');
		let react;
		try {
			react = await msg.awaitReactions(
				(reaction, user) => reaction.emoji.name === '🗑' && user.id === message.author.id,
				{ max: 1, time: 5000, errors: ['time'] }
			);
		} catch (error) {
			msg.reactions.removeAll();

			return message;
		}
		react.first()!.message.delete();

		return message;
	}
}
