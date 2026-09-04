const { Argument, Command } = require('discord-akairo');
const { MessageEmbed } = require('discord.js');
const { stripIndents } = require('common-tags');
const ms = require('@naval-base/ms');
const { CONSTANTS } = require('../../util');

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
};

class CaseCommand extends Command {
	constructor() {
		super('case', {
			aliases: ['case'],
			category: 'mod',
			description: {
				content: 'Inspect a case, pulled from the database.',
				usage: '<case>',
				examples: ['case 1234']
			},
			channel: 'guild',
			clientPermissions: ['MANAGE_ROLES'],
			ratelimit: 2,
			args: [
				{
					id: 'caseNum',
					type: Argument.union('number', 'string'),
					prompt: {
						start: message => `${message.author}, what case do you want to look up?`,
						retry: message => `${message.author}, please enter a case number.`
					}
				}
			]
		});
	}

	async exec(message, { caseNum }) {
		if (!this.client.settings.get(message.guild, 'moderation')) {
			return message.reply('moderation commands are disabled on this server.');
		}
		const staffRole = message.member.roles.has(this.client.settings.get(message.guild, 'modRole'));
		if (!staffRole) return message.reply('you know, I know, we should just leave it at that.');

		const totalCases = this.client.settings.get(message.guild, 'caseTotal', 0);
		const caseToFind = caseNum === 'latest' || caseNum === 'l' ? totalCases : caseNum;
		if (isNaN(caseToFind)) return message.reply('at least provide me with a correct number.');
		const dbCase = await this.client.db.models.cases.findOne({ where: { case_id: caseToFind } });
		if (!dbCase) {
			return message.reply('I looked where I could, but I couldn\'t find a case with that Id, maybe look for something that actually exists next time!');
		}

		const moderator = await message.guild.members.fetch(dbCase.mod_id);
		const color = Object.keys(CONSTANTS.ACTIONS).find(key => CONSTANTS.ACTIONS[key] === dbCase.action).split(' ')[0].toUpperCase();
		const embed = new MessageEmbed()
			.setAuthor(`${dbCase.mod_tag} (${dbCase.mod_id})`, moderator ? moderator.user.displayAvatarURL() : '')
			.setColor(CONSTANTS.COLORS[color])
			.setDescription(stripIndents`
				**Member:** ${dbCase.target_tag} (${dbCase.target_id})
				**Action:** ${ACTIONS[dbCase.action]}${dbCase.action === 5 ? `\n**Length:** ${ms(dbCase.action_duration.getTime(), { 'long': true })}` : ''}
				**Reason:** ${dbCase.reason}${dbCase.ref_id ? `\n**Ref case:** ${dbCase.ref_id}` : ''}
			`)
			.setFooter(`Case ${dbCase.case_id}`)
			.setTimestamp(new Date(dbCase.createdAt));

		return message.util.send(embed);
	}
}

module.exports = CaseCommand;
