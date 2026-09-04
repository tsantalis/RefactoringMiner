import { Command } from 'discord-akairo';
import { Message, GuildMember } from 'discord.js';
import Util from '../../util';
import { Case } from '../../models/Cases';

export default class HistoryCommand extends Command {
	public constructor() {
		super('history', {
			aliases: ['history'],
			category: 'mod',
			description: {
				content: 'Check the history of a member.',
				usage: '<member>',
				examples: ['@Crawl']
			},
			channel: 'guild',
			clientPermissions: ['MANAGE_ROLES'],
			ratelimit: 2,
			args: [
				{
					'id': 'member',
					'match': 'content',
					'type': 'member',
					'default': (message: Message) => message.member
				}
			]
		});
	}

	public async exec(message: Message, { member }: { member: GuildMember }) {
		if (!this.client.settings.get(message.guild, 'moderation', undefined)) {
			return message.reply('moderation commands are disabled on this server.');
		}
		const staffRole = message.member.roles.has(this.client.settings.get(message.guild, 'modRole', undefined));
		if (!staffRole && message.author.id !== member.id) return message.reply('you know, I know, we should just leave it at that.');

		const casesRepo = this.client.db.getRepository(Case);
		const dbCases = await casesRepo.find({ target_id: member.id });
		const embed = Util.historyEmbed(member, dbCases);

		return message.util!.send(embed);
	}
}
