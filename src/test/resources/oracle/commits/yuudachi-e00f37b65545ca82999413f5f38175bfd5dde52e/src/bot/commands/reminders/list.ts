import { Command } from 'discord-akairo';
import { Message } from 'discord.js';
import Util from '../../util';
import { Reminder } from '../../models/Reminders';

export default class ReminderListCommand extends Command {
	public constructor() {
		super('reminder-list', {
			aliases: ['reminders'],
			category: 'reminders',
			description: {
				content: 'Lists all of your ongoing reminders.'
			},
			clientPermissions: ['EMBED_LINKS'],
			ratelimit: 2
		});
	}

	public async exec(message: Message) {
		const remindersRepo = this.client.db.getRepository(Reminder);
		const reminders = await remindersRepo.find({ user: message.author.id });

		return message.util!.send(Util.reminderEmbed(message, reminders));
	}
}
