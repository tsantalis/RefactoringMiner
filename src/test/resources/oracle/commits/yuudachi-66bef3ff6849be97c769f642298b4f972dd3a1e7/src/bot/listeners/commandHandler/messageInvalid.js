const { Listener } = require('discord-akairo');

class MessageInvalidListener extends Listener {
	constructor() {
		super('messageInvalid', {
			emitter: 'commandHandler',
			event: 'messageInvalid',
			category: 'commandHandler'
		});
	}

	async exec(message) {
		if (message.guild && message.util.parsed.prefix) {
			if (!message.util.parsed.alias || !message.util.parsed.afterPrefix) return;
			const command = this.client.commandHandler.modules.get('tag-show');
			return this.client.commandHandler.runCommand(message, command, await command.parse(message, message.util.parsed.afterPrefix));
		}
	}
}

module.exports = MessageInvalidListener;
