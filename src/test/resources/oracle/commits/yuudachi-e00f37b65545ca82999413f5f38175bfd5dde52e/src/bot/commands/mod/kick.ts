import { Command } from 'discord-akairo';
import { Message, GuildMember, TextChannel } from 'discord.js';
import { stripIndents } from 'common-tags';
import Util from '../../util';
import { Case } from '../../models/Cases';

export default class KickCommand extends Command {
	public constructor() {
		super('kick', {
			aliases: ['kick'],
			category: 'mod',
			description: {
				content: 'Kicks a member, duh.',
				usage: '<member> <...reason>',
				examples: ['@Crawl', '@Crawl']
			},
			channel: 'guild',
			clientPermissions: ['MANAGE_ROLES'],
			ratelimit: 2,
			args: [
				{
					id: 'member',
					type: 'member',
					prompt: {
						start: (message: Message) => `${message.author}, what member do you want to kick?`,
						retry: (message: Message) => `${message.author}, please mention a member.`
					}
				},
				{
					'id': 'reason',
					'match': 'rest',
					'type': 'string',
					'default': ''
				}
			]
		});
	}

	public async exec(message: Message, { member, reason }: { member: GuildMember, reason: string }) {
		if (!this.client.settings.get(message.guild, 'moderation', undefined)) {
			return message.reply('moderation commands are disabled on this server.');
		}
		const staffRole = this.client.settings.get(message.guild, 'modRole', undefined);
		const hasStaffRole = message.member.roles.has(staffRole);
		if (!hasStaffRole) return message.reply('you know, I know, we should just leave it at that.');
		if (member.id === message.author.id) return;
		if (member.roles.has(staffRole)) {
			return message.reply('nuh-uh! You know you can\'t do this.');
		}

		const totalCases = this.client.settings.get(message.guild, 'caseTotal', 0) + 1;

		let sentMessage;
		try {
			sentMessage = await message.channel.send(`Kicking **${member.user.tag}**...`) as Message;
			try {
				await member.send(stripIndents`
					**You have been kicked from ${message.guild.name}**
					${reason ? `\n**Reason:** ${reason}\n` : ''}
					You may rejoin whenever.
				`);
			} catch {}
			await member.kick(`Kicked by ${message.author.tag} | Case #${totalCases}`);
		} catch (error) {
			return message.reply('there is no mute role configured on this server.');
		}

		this.client.settings.set(message.guild, 'caseTotal', totalCases);

		if (!reason) {
			// @ts-ignore
			const prefix = this.handler.prefix(message);
			reason = `Use \`${prefix}reason ${totalCases} <...reason>\` to set a reason for this case`;
		}

		const modLogChannel = this.client.settings.get(message.guild, 'modLogChannel', undefined);
		let modMessage;
		if (modLogChannel) {
			const embed = Util.logEmbed({ message, member, action: 'Kick', caseNum: totalCases, reason }).setColor(Util.CONSTANTS.COLORS.KICK);
			modMessage = await (this.client.channels.get(modLogChannel) as TextChannel).send(embed) as Message;
		}

		const casesRepo = this.client.db.getRepository(Case);
		const dbCase = new Case();
		dbCase.guild = message.guild.id;
		if (modMessage) dbCase.message = modMessage.id;
		dbCase.case_id = totalCases;
		dbCase.target_id = member.id;
		dbCase.target_tag = member.user.tag;
		dbCase.mod_id = message.author.id;
		dbCase.mod_tag = message.author.tag;
		dbCase.action = Util.CONSTANTS.ACTIONS.KICK;
		dbCase.reason = reason;
		await casesRepo.save(dbCase);

		return sentMessage.edit(`Successfully kicked **${member.user.tag}**`);
	}
}
