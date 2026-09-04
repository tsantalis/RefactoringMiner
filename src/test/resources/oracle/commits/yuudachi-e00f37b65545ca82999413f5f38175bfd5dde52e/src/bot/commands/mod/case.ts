import { Argument, Command } from 'discord-akairo';
import { Message, MessageEmbed } from 'discord.js';
import { stripIndents } from 'common-tags';
import Util from '../../util';
import { Case } from '../../models/Cases';
const ms = require('@naval-base/ms');

const ACTIONS = {
	1: 'Ban',
	2: 'Unban',
	3: 'Softban',
	4: 'Kick',
	5: 'Mute',
	6: 'Embed restriction',
	7: 'Emoji restriction',
	8: 'Reaction restriction',
	9: 'Warn'
} as { [key: number]: string };

export default class CaseCommand extends Command {
	public constructor() {
		super('case', {
			aliases: ['case'],
			category: 'mod',
			description: {
				content: 'Inspect a case, pulled from the database.',
				usage: '<case>',
				examples: ['1234']
			},
			channel: 'guild',
			clientPermissions: ['MANAGE_ROLES'],
			ratelimit: 2,
			args: [
				{
					id: 'caseNum',
					type: Argument.union('number', 'string'),
					prompt: {
						start: (message: Message) => `${message.author}, what case do you want to look up?`,
						retry: (message: Message) => `${message.author}, please enter a case number.`
					}
				}
			]
		});
	}

	public async exec(message: Message, { caseNum }: { caseNum: number | string }) {
		if (!this.client.settings.get(message.guild, 'moderation', undefined)) {
			return message.reply('moderation commands are disabled on this server.');
		}
		const staffRole = message.member.roles.has(this.client.settings.get(message.guild, 'modRole', undefined));
		if (!staffRole) return message.reply('you know, I know, we should just leave it at that.');

		const totalCases = this.client.settings.get(message.guild, 'caseTotal', 0);
		const caseToFind = caseNum === 'latest' || caseNum === 'l' ? totalCases : caseNum;
		if (isNaN(caseToFind)) return message.reply('at least provide me with a correct number.');
		const casesRepo = this.client.db.getRepository(Case);
		const dbCase = await casesRepo.findOne({ case_id: caseToFind });
		if (!dbCase) {
			return message.reply('I looked where I could, but I couldn\'t find a case with that Id, maybe look for something that actually exists next time!');
		}

		const moderator = await message.guild.members.fetch(dbCase.mod_id);
		const color = Object.keys(Util.CONSTANTS.ACTIONS).find(key => Util.CONSTANTS.ACTIONS[key] === dbCase.action)!.split(' ')[0].toUpperCase();
		const embed = new MessageEmbed()
			.setAuthor(`${dbCase.mod_tag} (${dbCase.mod_id})`, moderator ? moderator.user.displayAvatarURL() : '')
			.setColor(Util.CONSTANTS.COLORS[color])
			.setDescription(stripIndents`
				**Member:** ${dbCase.target_tag} (${dbCase.target_id})
				**Action:** ${ACTIONS[dbCase.action]}${dbCase.action === 5 ? `\n**Length:** ${ms(dbCase.action_duration.getTime(), { 'long': true })}` : ''}
				**Reason:** ${dbCase.reason}${dbCase.ref_id ? `\n**Ref case:** ${dbCase.ref_id}` : ''}
			`)
			.setFooter(`Case ${dbCase.case_id}`)
			.setTimestamp(new Date(dbCase.createdAt));

		return message.util!.send(embed);
	}
}
