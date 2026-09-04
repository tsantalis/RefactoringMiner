import { Argument, Command } from 'discord-akairo';
import { Message, MessageEmbed, TextChannel } from 'discord.js';
import Util from '../../util';
import { Case } from '../../models/Cases';
const ms = require('@naval-base/ms');

export default class DurationCommand extends Command {
	public constructor() {
		super('duration', {
			aliases: ['duration'],
			category: 'mod',
			description: {
				content: 'Sets the duration for a mute and reschedules it.',
				usage: '<case> <duration>',
				examples: ['1234 30m', 'latest 20h']
			},
			channel: 'guild',
			clientPermissions: ['MANAGE_ROLES'],
			ratelimit: 2,
			args: [
				{
					id: 'caseNum',
					type: Argument.union('number', 'string'),
					prompt: {
						start: (message: Message) => `${message.author}, what case do you want to add a reason to?`,
						retry: (message: Message) => `${message.author}, please enter a case number.`
					}
				},
				{
					id: 'duration',
					type: str => {
						const duration = ms(str);
						if (duration && duration >= 300000) return duration;
						return null;
					},
					prompt: {
						start: (message: Message) => `${message.author}, for how long do you want the mute to last?`,
						retry: (message: Message) => `${message.author}, please use a proper time format.`
					}
				}
			]
		});
	}

	public async exec(message: Message, { caseNum, duration }: { caseNum: number | string, duration: number }) {
		if (!this.client.settings.get(message.guild, 'moderation', undefined)) {
			return message.reply('moderation commands are disabled on this server.');
		}
		const staffRole = message.member.roles.has(this.client.settings.get(message.guild, 'modRole', undefined));
		if (!staffRole) return message.reply('you know, I know, we should just leave it at that.');

		const totalCases = this.client.settings.get(message.guild, 'caseTotal', 0);
		const caseToFind = caseNum === 'latest' || caseNum === 'l' ? totalCases : caseNum;
		if (isNaN(caseToFind)) return message.reply('at least provide me with a correct number.');
		const casesRepo = this.client.db.getRepository(Case);
		const dbCase = await casesRepo.findOne({ case_id: caseToFind, action: Util.CONSTANTS.ACTIONS.MUTE, action_processed: false });
		if (!dbCase) {
			return message.reply('I looked where I could, but I couldn\'t find a case with that Id and action, maybe look for something that actually exists next time!');
		}
		if (dbCase.mod_id !== message.author.id && !message.member.permissions.has('MANAGE_GUILD')) {
			return message.reply('you\'d be wrong in thinking I would let you fiddle with other peoples achievements!');
		}

		const modLogChannel = this.client.settings.get(message.guild, 'modLogChannel', undefined);
		if (modLogChannel) {
			const caseEmbed = await (this.client.channels.get(modLogChannel) as TextChannel).messages.fetch(dbCase.message) as Message;
			if (!caseEmbed) return message.reply('looks like the message doesn\'t exist anymore!');
			const embed = new MessageEmbed(caseEmbed.embeds[0]);
			if (dbCase.action_duration) {
				embed.setDescription(caseEmbed.embeds[0].description.replace(/\*\*Length:\*\* (.+)*/, `**Length:** ${ms(duration, { 'long': true })}`));
			} else {
				embed.setDescription(caseEmbed.embeds[0].description.replace(/(\*\*Action:\*\* Mute)/, `$1\n**Length:** ${ms(duration, { 'long': true })}`));
			}
			await caseEmbed.edit(embed);
		}
		dbCase.action_duration = new Date(Date.now() + duration);
		await casesRepo.save(dbCase);
		this.client.muteScheduler.rescheduleMute(dbCase);

		return message.util!.send(`Successfully updated duration for case **#${caseToFind}**`);
	}
}
