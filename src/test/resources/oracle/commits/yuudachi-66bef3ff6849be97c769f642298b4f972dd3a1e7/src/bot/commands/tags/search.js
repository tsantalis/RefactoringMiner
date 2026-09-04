const { Command } = require('discord-akairo');
const { MessageEmbed, Util } = require('discord.js');
const { Op } = require('sequelize');

class SearchTagCommand extends Command {
	constructor() {
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
						start: message => `${message.author}, what would you like to search for?`
					}
				}
			]
		});
	}

	async exec(message, { name }) {
		name = Util.cleanContent(name, message);
		const tags = await this.client.db.models.tags.findAll({ where: { name: { [Op.like]: `%${name}%` }, guild: message.guild.id } });
		if (!tags.length) return message.util.reply(`No results found with query ${name}.`);
		const search = tags
			.map(tag => `\`${tag.name}\``)
			.sort()
			.join(', ');
		if (search.length >= 1950) {
			return message.util.reply('the output is way too big to display, make your search more specific and try again!');
		}
		const embed = new MessageEmbed()
			.setColor(0x30a9ed)
			.setAuthor(`${message.author.tag} (${message.author.id})`, message.author.displayAvatarURL())
			.setDescription(search);

		return message.util.send(embed);
	}
}

module.exports = SearchTagCommand;
