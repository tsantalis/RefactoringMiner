const { Command } = require('discord-akairo');

class TagDownloadCommand extends Command {
	constructor() {
		super('tag-download', {
			category: 'tags',
			description: {
				content: 'Downloads a/all tag(s).',
				usage: '[member]'
			},
			channel: 'guild',
			ratelimit: 2,
			args: [
				{
					'id': 'member',
					'match': 'content',
					'type': 'member',
					'default': ''
				}
			]
		});
	}

	async exec(message, { member }) {
		const where = member ? { user: member.id, guild: message.guild.id } : { guild: message.guild.id };
		const tags = await this.client.db.models.tags.findAll({ where });
		if (!tags.length) return;
		const output = tags.reduce((out, t) => {
			out += `Name: ${t.name}\r\nContent:\r\n${t.content.replace(/\n/g, '\r\n')}\r\n\r\n========================================\r\n\r\n`
			return out;
		}, '');
		
		return message.util.send('Haiiiii~', { files: [{ attachment: Buffer.from(output, 'utf8'), name: `${member ? `${member.displayName}s_tags` : 'all_tags' }.txt` }] });
	}
}

module.exports = TagDownloadCommand;
