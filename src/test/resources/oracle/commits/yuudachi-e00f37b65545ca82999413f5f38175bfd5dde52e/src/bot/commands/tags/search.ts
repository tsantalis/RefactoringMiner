import { Command } from 'discord-akairo';
import { Message, MessageEmbed, Util } from 'discord.js';
import { Tag } from '../../models/Tags';
import { Like } from 'typeorm';

export default class SearchTagCommand extends Command {
	public constructor() {
		super('tag-search', {
			category: 'tags',
			description: {
				content: 'Searches a tag.',
				usage: '<tag>'
			},
			channel: 'guild',
			clientPermissions: ['EMBED_LINKS'],
			ratelimit: 2,
			args: [
				{
					id: 'name',
					match: 'content',
					type: 'lowercase',
					prompt: {
						start: (message: Message) => `${message.author}, what would you like to search for?`
					}
				}
			]
		});
	}

	public async exec(message: Message, { name }: { name: string }) {
		name = Util.cleanContent(name, message);
		const tagsRepo = this.client.db.getRepository(Tag);
		const tags = await tagsRepo.find({ name: Like(`%${name}%`), guild: message.guild.id });
		if (!tags.length) return message.util!.reply(`No results found with query ${name}.`);
		const search = tags
			.map(tag => `\`${tag.name}\``)
			.sort()
			.join(', ');
		if (search.length >= 1950) {
			return message.util!.reply('the output is way too big to display, make your search more specific and try again!');
		}
		const embed = new MessageEmbed()
			.setColor(0x30a9ed)
			.setAuthor(`${message.author.tag} (${message.author.id})`, message.author.displayAvatarURL())
			.setDescription(search);

		return message.util!.send(embed);
	}
}
