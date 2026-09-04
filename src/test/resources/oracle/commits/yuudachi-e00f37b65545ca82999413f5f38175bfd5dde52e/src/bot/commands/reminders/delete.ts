import { Command } from 'discord-akairo';
import { Message } from 'discord.js';
import Util from '../../util';
import { Reminder } from '../../models/Reminders';

export default class ReminderDeleteCommand extends Command {
	public constructor() {
		super('reminder-delete', {
			category: 'reminders',
			description: {
				content: 'Deletes/cancels an ongoing reminder.',
				usage: '[--all/-a]',
				examples: ['--all']
			},
			ratelimit: 2,
			args: [
				{
					id: 'all',
					match: 'flag',
					flag: ['--all', '-a']
				}
			]
		});
	}

	public async exec(message: Message, { all }: { all: boolean }) {
		const remindersRepo = this.client.db.getRepository(Reminder);
		if (all) {
			const reminders = await remindersRepo.find({ user: message.author.id });
			for (const reminder of reminders) this.client.remindScheduler.cancelReminder(reminder.id);

			const deleted = await remindersRepo.remove(reminders);
			return message.util!.reply(`I deleted ${deleted.length} reminder${deleted.length === 1 ? '' : 's'}!`);
		}

		const reminders = await remindersRepo.find({ user: message.author.id });
		if (!reminders.length) return message.util!.reply('you have no ongoing reminders!');

		while (reminders.length) {
			await message.util!.send(Util.reminderEmbed(message, reminders).setFooter(`Send a message with the reminder's number to delete it or \`cancel\` to cancel`));

			const messages = await message.channel.awaitMessages(
				m => m.author.id === message.author.id && ((m.content > 0 && m.content <= reminders.length) || m.content.toLowerCase() === 'cancel'),
				{ max: 1, time: 20000 }
			);
			if (!messages.size) return message.util!.send('Looks like you\'ve run out of time!');
			if (messages.first()!.content.toLowerCase() === 'cancel') return message.util!.send('Looks like we\'re all done here!');

			const index = parseInt(messages.first()!.content, 10) - 1;
			const reminder = reminders.splice(index, 1)[0];
			await this.client.remindScheduler.deleteReminder(reminder.id);
		}

		return message.util!.send('Welp, looks like all of your reminders are gone!');
	}
}
