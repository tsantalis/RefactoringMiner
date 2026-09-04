const { Command } = require('discord-akairo');

class PrefixCommand extends Command {
	constructor() {
		super('prefix', {
			aliases: ['prefix'],
			description: {
				content: 'Displays or changes the prefix of the guild.',
				usage: '[prefix]',
				examples: ['prefix', 'prefix *', 'prefix Yukikaze']
			},
			category: 'util',
			channel: 'guild',
			ratelimit: 2,
			args: [
				{
					id: 'prefix',
					type: 'string'
				}
			]
		});
	}

	exec(message, { prefix }) {
		if (!prefix) return message.util.send(`The current prefix for this guild is: \`${this.handler.prefix(message)}\``);
		this.client.settings.set(message.guild, 'prefix', prefix);
		if (prefix === process.env.COMMAND_PREFIX) {
			return message.util.reply(`the prefix has been reset to \`${prefix}\``);
		}
		return message.util.reply(`the prefix has been set to \`${prefix}\``);
	}
}

module.exports = PrefixCommand;
