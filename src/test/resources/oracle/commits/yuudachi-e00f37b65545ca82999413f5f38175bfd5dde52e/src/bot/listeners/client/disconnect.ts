import { Listener } from 'discord-akairo';

export default class DisconnectListener extends Listener {
	public constructor() {
		super('disconnect', {
			emitter: 'client',
			event: 'disconnect',
			category: 'client'
		});
	}

	public exec(event: any) {
		this.client.logger.warn(`Hmm, I have to hide the fact I was defeated... I'll let you go this time! (${event.code})`);
	}
}
