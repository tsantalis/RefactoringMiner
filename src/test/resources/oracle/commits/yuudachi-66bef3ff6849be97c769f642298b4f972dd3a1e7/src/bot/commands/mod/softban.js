const { Command } = require('discord-akairo');
const { stripIndents } = require('discord.js');
const { CONSTANTS: { ACTIONS, COLORS }, logEmbed } = require('../../util');

class SoftbanCommand extends Command {
	constructor() {
		super('softban', {
			aliases: ['softban'],
			category: 'mod',
			description: {
				content: 'Softbans a member, duh.',
				usage: '<member> <...reason>',
				examples: ['softban @Crawl']
			},
			channel: 'guild',
			clientPermissions: ['MANAGE_ROLES'],
			ratelimit: 2,
			args: [
				{
					id: 'member',
					type: 'member',
					prompt: {
						start: message => `${message.author}, what member do you want to softban?`,
						retry: message => `${message.author}, please mention a member.`
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

	async exec(message, { member, reason }) {
		if (!this.client.settings.get(message.guild, 'moderation')) {
			return message.reply('moderation commands are disabled on this server.');
		}
		const staffRole = this.client.settings.get(message.guild, 'modRole');
		const hasStaffRole = message.member.roles.has(staffRole);
		if (!hasStaffRole) return message.reply('you know, I know, we should just leave it at that.');
		if (member.id === message.author.id) return;
		if (member.roles.has(staffRole)) {
			return message.reply('nuh-uh! You know you can\'t do this.');
		}

		const keys = [`${message.guild.id}:${member.id}:BAN`, `${message.guild.id}:${member.id}:UNBAN`];
		if (this.client._cachedCases.has(keys[0]) && this.client._cachedCases.has(keys[1])) {
			return message.reply('that user is currently being moderated by someone else.');
		}
		this.client._cachedCases.add(keys[0]);
		this.client._cachedCases.add(keys[1]);

		const totalCases = this.client.settings.get(message.guild, 'caseTotal', 0) + 1;

		let sentMessage;
		try {
			sentMessage = await message.channel.send(`Softbanning **${member.user.tag}**...`);
			try {
				await member.send(stripIndents`
					**You have been softbanned from ${message.guild.name}**
					${reason ? `\n**Reason:** ${reason}\n` : ''}
					A softban is a kick that uses ban + unban to remove your messages from the server.
					You may rejoin whenever.
				`);
			} catch {}
			await member.ban({ days: 1, reason: `Softbanned by ${message.author.tag} | Case #${totalCases}` });
			await message.guild.members.unban(member, `Softbanned by ${message.author.tag} | Case #${totalCases}`);
		} catch (error) {
			this.client._cachedCases.delete(keys[0]);
			this.client._cachedCases.delete(keys[1]);
			return message.reply(`there was an error softbanning this member: \`${error}\``);
		}

		this.client.settings.set(message.guild, 'caseTotal', totalCases);

		if (!reason) {
			const prefix = this.handler.prefix(message);
			reason = `Use \`${prefix}reason ${totalCases} <...reason>\` to set a reason for this case`;
		}

		const modLogChannel = this.client.settings.get(message.guild, 'modLogChannel');
		let modMessage;
		if (modLogChannel) {
			const embed = logEmbed({ message, member, action: 'Softban', caseNum: totalCases, reason }).setColor(COLORS.SOFTBAN);
			modMessage = await this.client.channels.get(modLogChannel).send(embed);
		}
		await this.client.db.models.cases.create({
			guild: message.guild.id,
			message: modMessage ? modMessage.id : null,
			case_id: totalCases,
			target_id: member.id,
			target_tag: member.user.tag,
			mod_id: message.author.id,
			mod_tag: message.author.tag,
			action: ACTIONS.SOFTBAN,
			reason
		});

		return sentMessage.edit(`Successfully softbanned **${member.user.tag}**`);
	}
}

module.exports = SoftbanCommand;
