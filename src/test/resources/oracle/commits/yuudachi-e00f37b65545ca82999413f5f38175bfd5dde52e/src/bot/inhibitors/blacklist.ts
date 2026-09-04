import { Inhibitor } from 'discord-akairo';
import { Message } from 'discord.js';

export default class BlacklistInhibitor extends Inhibitor {
	public constructor() {
		super('blacklist', {
			reason: 'blacklist'
		});
	}

	public exec(message: Message) {
		const blacklist = this.client.settings.get('global', 'blacklist', []);
		return blacklist.includes(message.author.id);
	}
}
