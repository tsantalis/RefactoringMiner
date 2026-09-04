const { Command } = require('discord-akairo');

class TagAddCommand extends Command {
	constructor() {
		super('tag-add', {
			category: 'tags',
			description: {
				content: 'Adds a tag, usable for everyone on the server (Markdown can be used).',
				usage: '[--hoisted] <tag> <content>',
				examples: ['Test Test', '--hoisted "Test 2" Test2', '"Test 3" "Some more text" --hoisted']
			},
			channel: 'guild',
			ratelimit: 2,
			args: [
				{
					id: 'name',
					type: 'existingTag',
					prompt: {
						start: message => `${message.author}, what should the tag be named?`,
						retry: (message, _, provided) => `${message.author}, a tag with the name **${provided.phrase}** already exists.`
					}
				},
				{
					id: 'content',
					match: 'rest',
					type: 'tagContent',
					prompt: {
						start: message => `${message.author}, what should the content of the tag be?`
					}
				},
				{
					id: 'hoist',
					match: 'flag',
					flag: ['--hoist', '--pin']
				}
			]
		});
	}

	async exec(message, { name, content, hoist }) {
		if (content && content.length >= 1950) {
			return message.util.reply('you must still have water behind your ears to not realize that messages have a limit of 2000 characters!');
		}
		const staffRole = message.member.roles.has(this.client.settings.get(message.guild, 'modRole'));
		await this.client.db.models.tags.create({
			user: message.author.id,
			guild: message.guild.id,
			name,
			hoisted: hoist && staffRole ? true : false,
			content
		});

		return message.util.reply(`leave it to me! A tag with the name **${name}** has been added.`);
	}
}

module.exports = TagAddCommand;
