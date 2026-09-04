const { Command } = require('discord-akairo');
const { MessageEmbed } = require('discord.js');
const { stripIndents } = require('common-tags');
const moment = require('moment');
require('moment-duration-format');

class UserInfoCommand extends Command {
	constructor() {
		super('user', {
			aliases: ['user', 'member', 'user-info'],
			description: {
				content: 'Get info about a member.',
				usage: '[member]',
				examples: ['Crawl', '@Crawl', '81440962496172032']
			},
			category: 'info',
			channel: 'guild',
			clientPermissions: ['EMBED_LINKS'],
			ratelimit: 2,
			args: [
				{
					'id': 'member',
					'match': 'content',
					'type': 'member',
					'default': message => message.member
				}
			]
		});
	}

	exec(message, { member }) {
		const { user } = member;
		const embed = new MessageEmbed()
			.setColor(3447003)
			.setDescription(`Info about **${user.tag}** (ID: ${member.id})`)
			.addField(
				'❯ Member Details',
				/* eslint-disable no-undefined, eqeqeq */
				stripIndents`
				${member.nickname == undefined ? '• No nickname' : ` • Nickname: ${member.nickname}`}
				• Roles: ${member.roles.map(roles => `\`${roles.name}\``).join(' ')}
				• Joined at: ${moment.utc(member.joinedAt).format('YYYY/MM/DD hh:mm:ss')}
			`
			/* eslint-enable no-undefined, eqeqeq */
			)
			/* eslint-disable max-len */
			.addField(
				'❯ User Details',
				stripIndents`
				• ID: ${member.id}
				• Username: ${member.user.tag}
				• Created at: ${moment.utc(user.createdAt).format('YYYY/MM/DD hh:mm:ss')}${user.bot ? '\n• Is a bot account' : ''}
				• Status: ${user.presence.status.toUpperCase()}
				• Activity: ${user.presence.activity ? user.presence.activity.name : 'None'}
			`
			)
			/* eslint-enable max-len */
			.setThumbnail(user.displayAvatarURL());

		return message.util.send(embed);
	}
}

module.exports = UserInfoCommand;
