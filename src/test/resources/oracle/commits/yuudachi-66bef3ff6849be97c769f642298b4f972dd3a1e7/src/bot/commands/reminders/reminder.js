const { Command } = require('discord-akairo');
const { stripIndents } = require('common-tags');

class ReminderCommand extends Command {
	constructor() {
		super('reminder', {
			aliases: ['remind', 'reminder'],
			description: {
				content: stripIndents`Available methods:
					 • add \`[--hoist/--pin] <tag> <content>\`
					 • del \`[--all]\`
					 • list

					Required: \`<>\` | Optional: \`[]\`

					For additional \`<...arguments>\` usage refer to the examples below.
				`,
				usage: '<method> <...arguments>',
				examples: [
					'add leave in 5 minutes',
					'add --dm ban Dim in 6 months',
					'delete',
					'delete --all',
					'list'
				]
			},
			category: 'reminders',
			ratelimit: 2,
			args: [
				{
					id: 'method',
					type: ['add', 'del', 'delete', 'list']
				},
				{
					'id': 'name',
					'match': 'rest',
					'default': ''
				}
			]
		});
	}

	exec(message, { method, name }) {
		if (!method) {
			const prefix = this.handler.prefix(message);
			return message.util.send(stripIndents`
				When you beg me so much I just can't not help you~
				Check \`${prefix}help reminder\` for more information.
			`);
		}
		const command = {
			'add': this.handler.modules.get('reminder-add'),
			'cancel': this.handler.modules.get('reminder-delete'),
			'del': this.handler.modules.get('reminder-delete'),
			'delete': this.handler.modules.get('reminder-delete'),
			'list': this.handler.modules.get('reminder-list')
		}[method];

		return this.handler.handleDirectCommand(message, name, command, true);
	}
}

module.exports = ReminderCommand;
