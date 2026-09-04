import { Listener } from 'discord-akairo';
import { Message, Guild, User, TextChannel } from 'discord.js';
import Util from '../../util';
import { Case } from '../../models/Cases';

export default class GuildBanRemoveListener extends Listener {
	public constructor() {
		super('guildBanRemove', {
			emitter: 'client',
			event: 'guildBanRemove',
			category: 'client'
		});
	}

	public async exec(guild: Guild, user: User) {
		if (!this.client.settings.get(guild, 'moderation', undefined)) return;
		if (this.client.cachedCases.delete(`${guild.id}:${user.id}:UNBAN`)) return;
		const totalCases = this.client.settings.get(guild, 'caseTotal', 0) + 1;
		this.client.settings.set(guild, 'caseTotal', totalCases);
		const modLogChannel = this.client.settings.get(guild, 'modLogChannel', undefined);
		let modMessage;
		if (modLogChannel) {
			// @ts-ignore
			const prefix = this.client.commandHandler.prefix({ guild });
			const reason = `Use \`${prefix}reason ${totalCases} <...reason>\` to set a reason for this case`;
			const embed = Util.logEmbed({ member: user, action: 'Unban', caseNum: totalCases, reason }).setColor(Util.CONSTANTS.COLORS.UNBAN);
			modMessage = await (this.client.channels.get(modLogChannel) as TextChannel).send(embed) as Message;
		}
		const casesRepo = this.client.db.getRepository(Case);
		const dbCase = new Case();
		dbCase.guild = guild.id;
		if (modMessage) dbCase.message = modMessage.id;
		dbCase.case_id = totalCases;
		dbCase.target_id = user.id;
		dbCase.target_tag = user.tag;
		dbCase.action = Util.CONSTANTS.ACTIONS.UNBAN;
		await casesRepo.save(dbCase);
	}
}
