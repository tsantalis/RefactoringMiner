const { Command, Control } = require('discord-akairo');

class TagAliasCommand extends Command {
	constructor() {
		super('tag-alias', {
			category: 'tags',
			description: {
				usage: '<--add/--del> <tag> <tagalias>',
				examples: ['--add Test1 Test2', '--del "Test 2" "Test 3"', '"Test 3" "Test 4" --add']
			},
			channel: 'guild',
			ratelimit: 2,
			args: [
				{
					id: 'first',
					type: 'tag',
					prompt: {
						start: message => `${message.author}, what's the tag you want to alias?`,
						retry: (message, _, provided) => `${message.author}, a tag with the name **${provided.phrase}** does not exist.`
					}
				},
				{
					id: 'add',
					match: 'flag',
					flag: '--add'
				},
				{
					id: 'del',
					match: 'flag',
					flag: '--del'
				},
				Control.if((_, args) => args.add, [
					{
						id: 'second',
						match: 'rest',
						type: 'existingTag',
						prompt: {
							start: message => `${message.author}, what's the alias you want to apply to this tag?`,
							retry: (message, _, provided) => `${message.author}, a tag with the name **${provided.phrase}** already exists.`
						}
					}
				], [
					{
						id: 'second',
						match: 'rest',
						type: 'string',
						prompt: {
							start: message => `${message.author}, what's the alias you want to apply to this tag?`,
							retry: (message, _, provided) => `${message.author}, a tag with the name **${provided.phrase}** already exists.`
						}
					}
				])
			]
		});
	}

	async exec(message, { first, second, add, del }) {
		if (add) {
			first.aliases.push(second);
		} else if (del) {
			const index = first.aliases.indexOf(second);
			first.aliases.splice(index, 1);
		} else {
			return message.util.reply('you have to either supply `--add` or `--del.`');
		}
		await first.update({ aliases: first.aliases, last_modified: message.author.id });

		return message.util.reply(`alias ${second} ${add ? 'added to' : 'deleted from'} tag ${first.name}.`);
	}
}

module.exports = TagAliasCommand;
