const { Listener } = require('discord-akairo');

class ResumeListener extends Listener {
	constructor() {
		super('resumed', {
			emitter: 'client',
			event: 'resumed',
			category: 'client'
		});
	}

	exec(events) {
		this.client.logger.info(`You made it out fine thanks to my luck! You ought to be thankful! (replayed ${events} events)`);
	}
}

module.exports = ResumeListener;
