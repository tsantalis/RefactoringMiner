import { Listener } from 'discord-akairo';
import { Collection, Message, MessageEmbed } from 'discord.js';
import * as moment from 'moment';
import 'moment-duration-format';

export default class MessageDeleteBulkListener extends Listener {
	public constructor() {
		super('messageDeleteBulk', {
			emitter: 'client',
			event: 'messageDeleteBulk',
			category: 'client'
		});
	}

	public exec(messages: Collection<string, Message>) {
		if (messages.first()!.author.bot) return;
		const guildLogs = this.client.settings.get(messages.first()!.guild, 'guildLogs', undefined);
		if (guildLogs) {
			const webhook = this.client.webhooks.get(guildLogs);
			if (!webhook) return;
			const output = messages.reduce((out: string, msg) => {
				const attachment = msg.attachments.first();
				out += `[${moment.utc(msg.createdTimestamp).format('YYYY/MM/DD hh:mm:ss')}] ${msg.author.tag} (${msg.author.id}): ${msg.cleanContent ? msg.cleanContent.replace(/\n/g, '\r\n') : ''}${attachment ? `\r\n${attachment.url}` : ''}\r\n`;
				return out;
			}, '');
			const embed = new MessageEmbed()
				.setColor(0x824aee)
				.setAuthor(`${messages.first()!.author.tag} (${messages.first()!.author.id})`, messages.first()!.author.displayAvatarURL())
				.addField('❯ Logs', 'See attachment file for full logs (possibly above this embed)')
				.setTimestamp(new Date())
				.setFooter('Bulk Deleted');

			return webhook.send({
				embeds: [embed],
				files: [{ attachment: Buffer.from(output, 'utf8'), name: 'logs.txt' }],
				username: 'Logs: MESSAGE DELETED BULK',
				avatarURL: 'https://i.imgur.com/EUGvQJJ.png'
			});
		}
	}
}
