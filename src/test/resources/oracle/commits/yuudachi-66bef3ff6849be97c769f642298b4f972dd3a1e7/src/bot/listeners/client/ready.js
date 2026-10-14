const { Listener } = require('discord-akairo');

class ReadyListener extends Listener {
	constructor() {
		super('ready', {
			emitter: 'client',
			event: 'ready',
			category: 'client'
		});
	}

	async exec() {
		this.client.logger.info(`Yawn... Hmph, ${this.client.user.tag} (${this.client.user.id}) is only with you because she's in a good mood!`);
		this.client.user.setActivity(`@${this.client.user.username} help 💖`);
		for (const guild of this.client.guilds.values()) {
			const logs = this.client.settings.get(guild, 'guildLogs');
			if (!logs) continue;
			const webhook = (await guild.fetchWebhooks()).get(logs);
			if (!webhook) continue;
			this.client.webhooks.set(webhook.id, webhook);
		}
	}
}

module.exports = ReadyListener;
