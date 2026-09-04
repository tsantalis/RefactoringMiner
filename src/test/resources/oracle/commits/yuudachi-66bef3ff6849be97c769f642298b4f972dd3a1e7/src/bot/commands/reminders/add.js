const { Command } = require('discord-akairo');
const sherlock = require('sherlockjs');
const REMINDER_LIMIT = 15;

class ReminderAddCommand extends Command {
	constructor() {
		super('reminder-add', {
			aliases: ['remind-me'],
			category: 'reminders',
			description: {
				content: 'Adds a reminder that triggers at the given time and tells you the given reason.',
				usage: '[--dm/--pm] <reason> <time>',
				examples: ['leave in 5 minutes', 'ban Dim in 6 months --dm']
			},
			ratelimit: 2,
			args: [
				{
					id: 'timeReason',
					match: 'rest',
					prompt: {
						start: message => `${message.author}, what do you want me to remind you of and when?`
					}
				},
				{
					id: 'dm',
					match: 'flag',
					flag: ['--dm', '--pm']
				}
			]
		});
	}

	async exec(message, { timeReason, dm }) {
		const reminderCount = await this.client.db.models.reminders.count({ where: { user: message.author.id } });
		if (reminderCount > REMINDER_LIMIT) {
			return message.util.reply(`you already have ${REMINDER_LIMIT} ongoing reminders... do you really need more?`);
		}

		const { eventTitle, startDate } = sherlock.parse(timeReason);

		if (eventTitle && eventTitle.length >= 1850) {
			return message.util.reply('you must still have water behind your ears to not realize that messages have a limit of 2000 characters!');
		}
		if (!startDate) {
			return message.util.reply('I can\'t tell what time I\'m supposed to remind you at!');
		}
		if (startDate < new Date()) {
			return message.util.reply('sorry, I don\'t have access to time travel yet!');
		}
		if (startDate.getTime() < (Date.now() + 5000)) {
			return message.util.reply('I\'m sure you have better memory than that.');
		}

		await this.client.remindScheduler.addReminder({
			user: message.author.id,
			channel: message.channel.type === 'dm' || dm ? null : message.channel.id,
			reason: eventTitle,
			trigger: message.url,
			triggers_at: startDate
		});

		return message.util.reply(`I'll remind you at ${startDate.toUTCString()}.`);
	}
}

module.exports = ReminderAddCommand;
