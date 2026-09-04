const { MessageEmbed, User } = require('discord.js');
const { stripIndents, oneLine } = require('common-tags');
const ms = require('@naval-base/ms');

const ACTIONS = {
	1: 'ban',
	2: 'unban',
	3: 'kick',
	4: 'kick',
	5: 'mute',
	6: 'restriction',
	7: 'restriction',
	8: 'restriction',
	9: 'warn'
};

module.exports = {
	CONSTANTS: {
		ACTIONS: {
			BAN: 1,
			UNBAN: 2,
			SOFTBAN: 3,
			KICK: 4,
			MUTE: 5,
			EMBED: 6,
			EMOJI: 7,
			REACTION: 8,
			WARN: 9
		},
		COLORS: {
			BAN: 16718080,
			UNBAN: 8450847,
			SOFTBAN: 16745216,
			KICK: 16745216,
			MUTE: 16763904,
			EMBED: 16776960,
			EMOJI: 16776960,
			REACTION: 16776960,
			WARN: 16776960
		}
	},
	reminderEmbed: (message, reminders) => {
		const truncate = (str, len) => str.length > len ? `${str.slice(0, len)}…` : str; // eslint-disable-line
		return new MessageEmbed()
			.setAuthor(`${message.author.tag} (${message.author.id})`, message.author.displayAvatarURL())
			.setColor(0x30A9ED)
			.setDescription(reminders.length
				? reminders.sort((a, b) => a.triggers_at - b.triggers_at).map(
					(reminder, i) => `${i + 1}. ${truncate(reminder.reason || 'reasonless', 30)} \`${reminder.triggers_at.toUTCString()}\`${reminder.channel ? '' : ' (DM)'}`
				).join('\n')
				: 'No reminders');
	},
	logEmbed: ({ message = null, member, action, duration = null, caseNum, reason, ref = null }) => {
		const embed = new MessageEmbed();
		if (message) {
			embed.setAuthor(`${message.author.tag} (${message.author.id})`, message.author.displayAvatarURL());
		}
		embed.setDescription(stripIndents`
				**Member:** ${member instanceof User ? member.tag : member.user.tag} (${member.id})
				**Action:** ${action}${action === 'Mute' && duration ? `\n**Length:** ${ms(duration, { 'long': true })}` : ''}
				**Reason:** ${reason}${ref ? `\n**Ref case:** ${ref}` : ''}
			`)
			.setFooter(`Case ${caseNum}`)
			.setTimestamp(new Date());

		return embed;
	},
	historyEmbed: (member, cases) => {
		const footer = cases.reduce((count, c) => {
			const action = ACTIONS[c.action];
			count[action] = (count[action] || 0) + 1;
			return count;
		}, {});
		const colors = [8450847, 10870283, 13091073, 14917123, 16152591, 16667430, 16462404];
		const values = [footer.warn || 0, footer.restriction || 0, footer.mute || 0, footer.kick || 0, footer.ban || 0];
		const [warn, restriction, mute, kick, ban] = values;
		const colorIndex = Math.min(values.reduce((a, b) => a + b), colors.length - 1);

		return new MessageEmbed()
			.setAuthor(`${member.user.tag} (${member.id})`, member.user.displayAvatarURL())
			.setColor(colors[colorIndex])
			.setFooter(oneLine`${warn} warning${warn > 1 || warn === 0 ? 's' : ''},
				${restriction} restriction${restriction > 1 || restriction === 0 ? 's' : ''},
				${mute} mute${mute > 1 || mute === 0 ? 's' : ''},
				${kick} kick${kick > 1 || kick === 0 ? 's' : ''},
				and ${ban} ban${ban > 1 || ban === 0 ? 's' : ''}.
			`);
	}
};
