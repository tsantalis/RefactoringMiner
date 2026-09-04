import { Command } from 'discord-akairo';
import { Message } from 'discord.js';
import { Reminder } from '../../models/Reminders';
const ms = require('@naval-base/ms');

const REMINDER_LIMIT = 15;

export default class ReminderAddCommand extends Command {
	public constructor() {
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
					id: 'time',
					type: (str: string) => {
						const duration = ms(str);
						if (duration && duration >= 300000) return duration;
						return null;
					},
					prompt: {
						start: (message: Message) => `${message.author}, when do you want me to remind you?`,
						retry: (message: Message) => `${message.author}, please use a proper time format.`
					}
				},
				{
					id: 'reason',
					type: 'string',
					prompt: {
						start: (message: Message) => `${message.author}, what do you want me to remind you of?`
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

	public async exec(message: Message, { time, reason, dm }: { time: number, reason: string, dm: boolean }) {
		const remindersRepo = this.client.db.getRepository(Reminder);
		const reminderCount = await remindersRepo.count({ user: message.author.id });
		if (reminderCount > REMINDER_LIMIT) {
			return message.util!.reply(`you already have ${REMINDER_LIMIT} ongoing reminders... do you really need more?`);
		}

		if (reason && reason.length >= 1850) {
			return message.util!.reply('you must still have water behind your ears to not realize that messages have a limit of 2000 characters!');
		}
		if (!time) {
			return message.util!.reply('I can\'t tell what time I\'m supposed to remind you at!');
		}
		if (time < Date.now()) {
			return message.util!.reply('sorry, I don\'t have access to time travel yet!');
		}
		if (time < (Date.now() + 5000)) {
			return message.util!.reply('I\'m sure you have better memory than that.');
		}

		await this.client.remindScheduler.addReminder({
			user: message.author.id,
			channel: message.channel.type === 'dm' || dm ? null : message.channel.id,
			reason,
			trigger: message.url,
			triggers_at: time
		});

		return message.util!.reply(`I'll remind you in ${ms(time)}`);
	}
}
