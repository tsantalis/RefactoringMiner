import { Listener } from 'discord-akairo';
import { Message, MessageEmbed, Util } from 'discord.js';
const diff = require('diff');

export default class MessageUpdateListener extends Listener {
	public constructor() {
		super('messageUpdate', {
			emitter: 'client',
			event: 'messageUpdate',
			category: 'client'
		});
	}

	public exec(oldMessage: Message, newMessage: Message) {
		if (oldMessage.author.bot || newMessage.author.bot) return;
		if (Util.escapeMarkdown(oldMessage.content) === Util.escapeMarkdown(newMessage.content)) return;
		const guildLogs = this.client.settings.get(newMessage.guild, 'guildLogs', undefined);
		if (guildLogs) {
			const webhook = this.client.webhooks.get(guildLogs);
			if (!webhook) return;
			const embed = new MessageEmbed()
				.setColor(0x306bff)
				.setAuthor(`${newMessage.author.tag} (${newMessage.author.id})`, newMessage.author.displayAvatarURL())
				.addField('❯ Channel', newMessage.channel);
			let msg = '';
			if (/```(.*?)```/s.test(oldMessage.content) && /```(.*?)```/s.test(newMessage.content)) {
				const strippedOldMessage = oldMessage.content.match(/```(?:(\S+)\n)?\s*([^]+?)\s*```/)![2];
				const strippedNewMessage = newMessage.content.match(/```(?:(\S+)\n)?\s*([^]+?)\s*```/)![2];
				if (strippedOldMessage === strippedNewMessage) return;
				const diffMessage = diff.diffLines(strippedOldMessage, strippedNewMessage, { newlineIsToken: true });
				for (const part of diffMessage) {
					if (part.value === '\n') continue;
					const d = part.added ? '+ ' : part.removed ? '- ' : '';
					msg += `${d}${part.value.replace(/\n/g, '')}\n`;
				}
				const prepend = '```diff\n';
				const append = '\n```';
				embed.addField('❯ Message', `${prepend}${msg.substring(0, 1000)}${append}`);
			} else {
				const diffMessage = diff.diffWords(Util.escapeMarkdown(oldMessage.content), Util.escapeMarkdown(newMessage.content));
				for (const part of diffMessage) {
					const markdown = part.added ? '**' : part.removed ? '~~' : '';
					msg += `${markdown}${part.value}${markdown}`;
				}
				embed.addField('❯ Message', `${msg.substring(0, 1020)}` || '\u200b');
			}
			embed.addField('❯ Message', `[Jump To](${newMessage.url})`, true);
			embed.setTimestamp(oldMessage.editedAt || newMessage.editedAt || new Date());
			embed.setFooter('Edited');

			return webhook.send({
				embeds: [embed],
				username: 'Logs: MESSAGE UPDATED',
				avatarURL: 'https://i.imgur.com/wnC4KmC.png'
			});
		}
	}
}
