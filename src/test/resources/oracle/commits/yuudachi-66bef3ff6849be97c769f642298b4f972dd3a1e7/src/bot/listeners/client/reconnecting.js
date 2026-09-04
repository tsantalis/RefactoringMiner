const { Listener } = require('discord-akairo');

class ReconnectListener extends Listener {
	constructor() {
		super('reconnecting', {
			emitter: 'client',
			event: 'reconnecting',
			category: 'client'
		});
	}

	exec() {
		this.client.logger.info("Come at me if you don't value your life!");
	}
}

module.exports = ReconnectListener;
