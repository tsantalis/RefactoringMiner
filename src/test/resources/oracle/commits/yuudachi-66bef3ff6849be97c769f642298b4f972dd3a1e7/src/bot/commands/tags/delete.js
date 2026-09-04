const { Command } = require('discord-akairo');

class TagDeleteCommand extends Command {
	constructor() {
		super('tag-delete', {
			category: 'tags',
			description: {
				content: 'Deletes a tag.',
				usage: '<tag>'
			},
			channel: 'guild',
			ratelimit: 2,
			args: [
				{
					id: 'tag',
					match: 'content',
					type: 'tag',
					prompt: {
						start: message => `${message.author}, what tag do you want to delete?`,
						retry: (message, _, provided) => `${message.author}, a tag with the name **${provided.phrase}** does not exist.`
					}
				}
			]
		});
	}

	exec(message, { tag }) {
		const staffRole = message.member.roles.has(this.client.settings.get(message.guild, 'modRole'));
		if (tag.user !== message.author.id && !staffRole) return message.util.reply('you can only delete your own tags.');
		tag.destroy();

		return message.util.reply(`successfully deleted **${tag.name}**.`);
	}
}

module.exports = TagDeleteCommand;
