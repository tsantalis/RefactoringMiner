const { Command } = require('discord-akairo');

class ToggleLogsCommand extends Command {
	constructor() {
		super('toggle-logs', {
			aliases: ['logs', 'toggle-logs'],
			description: {
				content: 'Toggle logs on the server.'
			},
			category: 'config',
			channel: 'guild',
			clientPermissions: ['MANAGE_WEBHOOKS'],
			userPermissions: ['MANAGE_GUILD'],
			ratelimit: 2,
			args: [
				{
					id: 'webhook',
					match: 'content',
					type: 'string',
					prompt: {
						start: message => `${message.author}, what Webhook should send the messages?`
					}
				}
			]
		});
	}

	async exec(message, { webhook }) {
		const guildLogs = this.client.settings.get(message.guild, 'guildLogs');
		if (guildLogs) {
			this.client.settings.delete(message.guild, 'guildLogs');
			this.client.webhooks.delete(webhook);
			return message.util.reply('successfully deactivated logs!');
		}
		this.client.settings.set(message.guild, 'guildLogs', webhook);
		const wh = (await message.guild.fetchWebhooks()).get(webhook);
		if (!wh) return;
		this.client.webhooks.set(wh.id, wh);

		return message.util.reply('successfully activated logs!');
	}
}

module.exports = ToggleLogsCommand;
