const { Command } = require('discord-akairo');

class TagSourceCommand extends Command {
	constructor() {
		super('tag-source', {
			category: 'tags',
			description: {
				content: 'Displays a tags source (Highlighted with Markdown).',
				usage: '[--file/-f] <tag>'
			},
			channel: 'guild',
			ratelimit: 2,
			args: [
				{
					id: 'file',
					match: 'flag',
					flag: ['--file', '-f']
				},
				{
					id: 'tag',
					match: 'rest',
					type: 'tag',
					prompt: {
						start: message => `${message.author}, what tag would you like to see the source of?`,
						retry: (message, _, provided) => `${message.author}, a tag with the name **${provided.phrase}** does not exist.`
					}
				}
			]
		});
	}

	exec(message, { tag, file }) {
		return message.util.send(tag.content, {
			code: 'md',
			files: file ? [{
				attachment: Buffer.from(tag.content.replace(/\n/g, '\r\n'), 'utf8'),
				name: `${tag.name}_source.txt`
			}] : null
		});
	}
}

module.exports = TagSourceCommand;
