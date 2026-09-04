const { Command } = require('discord-akairo');
const { Util } = require('discord.js');
const { Op } = require('sequelize');

class TagShowCommand extends Command {
	constructor() {
		super('tag-show', {
			category: 'tags',
			description: {
				content: 'Displays a tag.',
				usage: '<tag>'
			},
			channel: 'guild',
			ratelimit: 2,
			args: [
				{
					id: 'name',
					match: 'content',
					type: 'lowercase',
					prompt: {
						start: message => `${message.author}, what tag would you like to see?`
					}
				}
			]
		});
	}

	async exec(message, { name }) {
		if (!name) return;
		if (Boolean(message.member.roles.find(r => r.name === 'Embed restricted'))) return;
		name = Util.cleanContent(name, message);
		const tag = await this.client.db.models.tags.findOne({
			where: {
				[Op.or]: [
					{ name },
					{ aliases: { [Op.contains]: [name] } }
				],
				guild: message.guild.id
			}
		});
		if (!tag) return;
		tag.increment('uses');

		return message.util.send(tag.content);
	}
}

module.exports = TagShowCommand;
