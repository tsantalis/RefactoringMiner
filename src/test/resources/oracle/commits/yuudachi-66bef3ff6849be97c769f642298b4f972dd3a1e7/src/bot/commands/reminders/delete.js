const { Command } = require('discord-akairo');
const { reminderEmbed } = require('../../util');

class ReminderDeleteCommand extends Command {
	constructor() {
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

	async exec(message, { all }) {
		if (all) {
			const reminders = await this.client.db.models.reminders.findAll({ where: { user: message.author.id }, attributes: ['id'] });
			for (const reminder of reminders) this.client.remindScheduler.cancelReminder(reminder.id);

			const deleted = await this.client.db.models.reminders.destroy({ where: { user: message.author.id } });
			return message.util.reply(`I deleted ${deleted} reminder${deleted === 1 ? '' : 's'}!`);
		}

		const reminders = await this.client.db.models.reminders.findAll({ where: { user: message.author.id } });
		if (!reminders.length) return message.util.reply('you have no ongoing reminders!');

		while (reminders.length) {
			await message.util.send(reminderEmbed(message, reminders).setFooter(`Send a message with the reminder's number to delete it or \`cancel\` to cancel`));

			const messages = await message.channel.awaitMessages(
				m => m.author.id === message.author.id && ((m.content > 0 && m.content <= reminders.length) || m.content.toLowerCase() === 'cancel'),
				{ max: 1, time: 20000 }
			);
			if (!messages.size) return message.util.send('Looks like you\'ve run out of time!');
			if (messages.first().content.toLowerCase() === 'cancel') return message.util.send('Looks like we\'re all done here!');

			const index = parseInt(messages, 10) - 1;
			const reminder = reminders.splice(index, 1)[0];
			await this.client.remindScheduler.deleteReminder(reminder.id);
		}

		return message.util.send('Welp, looks like all of your reminders are gone!');
	}
}

module.exports = ReminderDeleteCommand;
