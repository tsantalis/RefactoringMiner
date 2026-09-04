const { Listener } = require('discord-akairo');

class DisconnectListener extends Listener {
	constructor() {
		super('disconnect', {
			emitter: 'client',
			event: 'disconnect',
			category: 'client'
		});
	}

	exec(event) {
		this.client.logger.warn(`Hmm, I have to hide the fact I was defeated... I'll let you go this time! (${event.code})`);
	}
}

module.exports = DisconnectListener;
