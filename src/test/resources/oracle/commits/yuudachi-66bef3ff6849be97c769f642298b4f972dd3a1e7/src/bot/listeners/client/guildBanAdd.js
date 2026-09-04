const { Listener } = require('discord-akairo');
const { CONSTANTS: { ACTIONS, COLORS }, logEmbed } = require('../../util');

class GuildBanAddListener extends Listener {
	constructor() {
		super('guildBanAdd', {
			emitter: 'client',
			event: 'guildBanAdd',
			category: 'client'
		});
	}

	async exec(guild, member) {
		if (!this.client.settings.get(guild, 'moderation')) return;
		if (this.client._cachedCases.delete(`${guild.id}:${member.id}:BAN`)) return;
		const totalCases = this.client.settings.get(guild, 'caseTotal', 0) + 1;
		this.client.settings.set(guild, 'caseTotal', totalCases);
		const modLogChannel = this.client.settings.get(guild, 'modLogChannel');
		let modMessage;
		if (modLogChannel) {
			const prefix = this.client.commandHandler.prefix({ guild });
			const reason = `Use \`${prefix}reason ${totalCases} <...reason>\` to set a reason for this case`;
			const embed = logEmbed({ member, action: 'Ban', caseNum: totalCases, reason }).setColor(COLORS.BAN);
			modMessage = await this.client.channels.get(modLogChannel).send(embed);
		}
		await this.client.db.models.cases.create({
			guild: guild.id,
			message: modMessage ? modMessage.id : null,
			case_id: totalCases,
			target_id: member.id,
			target_tag: member.user.tag,
			action: ACTIONS.BAN
		});
	}
}

module.exports = GuildBanAddListener;
