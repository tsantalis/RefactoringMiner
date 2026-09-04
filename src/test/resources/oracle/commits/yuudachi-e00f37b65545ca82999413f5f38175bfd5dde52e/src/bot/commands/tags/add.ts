import { Command } from 'discord-akairo';
import { Message } from 'discord.js';
import { Tag } from '../../models/Tags';

export default class TagAddCommand extends Command {
	public constructor() {
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
						start: (message: Message) => `${message.author}, what should the tag be named?`,
						retry: (message: Message, _: any, provided: { phrase: string }) => `${message.author}, a tag with the name **${provided.phrase}** already exists.`
					}
				},
				{
					id: 'content',
					match: 'rest',
					type: 'tagContent',
					prompt: {
						start: (message: Message) => `${message.author}, what should the content of the tag be?`
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

	public async exec(message: Message, { name, content, hoist }: { name: any, content: string, hoist: boolean }) {
		if (content && content.length >= 1950) {
			return message.util!.reply('you must still have water behind your ears to not realize that messages have a limit of 2000 characters!');
		}
		const staffRole = message.member.roles.has(this.client.settings.get(message.guild, 'modRole', undefined));
		const tagsRepo = this.client.db.getRepository(Tag);
		const tag = new Tag();
		tag.user = message.author.id;
		tag.guild = message.guild.id;
		tag.hoisted = hoist && staffRole ? true : false;
		tag.content = content;
		await tagsRepo.save(tag);

		return message.util!.reply(`leave it to me! A tag with the name **${name}** has been added.`);
	}
}
