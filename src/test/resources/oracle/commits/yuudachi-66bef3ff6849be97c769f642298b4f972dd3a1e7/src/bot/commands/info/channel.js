const { Command } = require('discord-akairo');
const { MessageEmbed } = require('discord.js');
const { stripIndents } = require('common-tags');
const moment = require('moment');
require('moment-duration-format');

class ChannelInfoCommand extends Command {
	constructor() {
		super('channel', {
			aliases: ['channel', 'channel-info'],
			description: {
				content: 'Get info about a channel.',
				usage: '[channel]',
				examples: ['#general', 'general', '222197033908436994']
			},
			category: 'info',
			channel: 'guild',
			clientPermissions: ['EMBED_LINKS'],
			ratelimit: 2,
			args: [
				{
					'id': 'channel',
					'match': 'content',
					'type': 'channel',
					'default': message => message.channel
				}
			]
		});
	}

	exec(message, { channel }) {
		const embed = new MessageEmbed()
			.setColor(3447003)
			.setDescription(`Info about **${channel.name}** (ID: ${channel.id})`)
			.addField(
				'❯ Info',
				stripIndents`
				• Type: ${channel.type}
				• Topic ${channel.topic ? channel.topic : 'None'}
				• NSFW: ${Boolean(channel.nsfw)}
				• Creation Date: ${moment.utc(channel.createdAt).format('YYYY/MM/DD hh:mm:ss')}
			`
			)
			.setThumbnail(message.guild.iconURL());

		return message.util.send(embed);
	}
}

module.exports = ChannelInfoCommand;
