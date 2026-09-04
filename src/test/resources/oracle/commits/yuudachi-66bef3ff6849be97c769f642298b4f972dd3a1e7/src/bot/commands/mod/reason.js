const { Argument, Command } = require('discord-akairo');
const { MessageEmbed } = require('discord.js');

class ReasonCommand extends Command {
	constructor() {
		super('reason', {
			aliases: ['reason'],
			category: 'mod',
			description: {
				content: 'Sets/Updates the reason of a modlog entry.',
				usage: '<case> <...reason>',
				examples: ['reason 1234 dumb', 'reason latest dumb']
			},
			channel: 'guild',
			clientPermissions: ['MANAGE_ROLES'],
			ratelimit: 2,
			args: [
				{
					id: 'caseNum',
					type: Argument.union('number', 'string'),
					prompt: {
						start: message => `${message.author}, what case do you want to add a reason to?`,
						retry: message => `${message.author}, please enter a case number.`
					}
				},
				{
					id: 'reason',
					match: 'rest',
					type: 'string'
				}
			]
		});
	}

	async exec(message, { caseNum, reason }) {
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
		if (dbCase.mod_id && (dbCase.mod_id !== message.author.id && !message.member.permissions.has('MANAGE_GUILD'))) {
			return message.reply('you\'d be wrong in thinking I would let you fiddle with other peoples achievements!');
		}

		const modLogChannel = this.client.settings.get(message.guild, 'modLogChannel');
		if (modLogChannel) {
			const caseEmbed = await this.client.channels.get(modLogChannel).messages.fetch(dbCase.message);
			if (!caseEmbed) return message.reply('looks like the message doesn\'t exist anymore!');
			const embed = new MessageEmbed(caseEmbed.embeds[0]);
			if (!dbCase.mod_id && !dbCase.mod_tag) {
				embed.setAuthor(`${message.author.tag} (${message.author.id})`, message.author.displayAvatarURL());
			}
			embed.setDescription(caseEmbed.embeds[0].description.replace(/\*\*Reason:\*\* [\s\S]+/, `**Reason:** ${reason}`));
			await caseEmbed.edit(embed);
		}
		dbCase.update({
			mod_id: message.author.id,
			mod_tag: message.author.tag,
			reason
		});

		return message.util.send(`Successfully set reason for case **#${caseToFind}**`);
	}
}

module.exports = ReasonCommand;
