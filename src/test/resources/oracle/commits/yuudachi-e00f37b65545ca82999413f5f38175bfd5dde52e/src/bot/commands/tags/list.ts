import { Command } from 'discord-akairo';
import { Message, MessageEmbed, GuildMember } from 'discord.js';
import { Tag } from '../../models/Tags';

export default class TagListCommand extends Command {
	public constructor() {
		super('tag-list', {
			aliases: ['tags'],
			category: 'tags',
			description: {
				content: 'Lists all server tags.'
			},
			channel: 'guild',
			clientPermissions: ['EMBED_LINKS'],
			ratelimit: 2,
			args: [
				{
					id: 'member',
					type: 'member'
				}
			]
		});
	}

	public async exec(message: Message, { member }: { member: GuildMember }) {
		const tagsRepo = this.client.db.getRepository(Tag);
		if (member) {
			const tags = await tagsRepo.find({ user: member.id, guild: message.guild.id });
			if (!tags.length) {
				if (member.id === message.author.id) return message.util!.reply("you don't have any tags.");
				return message.util!.reply(`**${member.displayName}** doesn't have any tags.`);
			}
			const embed = new MessageEmbed()
				.setColor(0x30a9ed)
				.setAuthor(`${member.user.tag} (${member.id})`, member.user.displayAvatarURL())
				.setDescription(
					tags
						.map(tag => `\`${tag.name}\``)
						.sort()
						.join(', ')
				);

			return message.util!.send(embed);
		}
		const tags = await tagsRepo.find({ guild: message.guild.id });
		if (!tags.length) return message.util!.send(`**${message.guild.name}** doesn't have any tags. Why not add some?`);
		const hoistedTags = tags
			.filter(tag => tag.hoisted)
			.map(tag => `\`${tag.name}\``)
			.sort()
			.join(', ');
		const userTags = tags
			.filter(tag => !tag.hoisted)
			.filter(tag => tag.user === message.author.id)
			.map(tag => `\`${tag.name}\``)
			.sort()
			.join(', ');
		const embed = new MessageEmbed()
			.setColor(0x30a9ed)
			.setAuthor(`${message.author.tag} (${message.author.id})`, message.author.displayAvatarURL());
		if (hoistedTags) embed.addField('❯ Tags', hoistedTags);
		if (userTags) embed.addField(`❯ ${message.member.displayName}'s tags`, userTags);

		return message.util!.send(embed);
	}
}
