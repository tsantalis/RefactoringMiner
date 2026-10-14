const { Command } = require('discord-akairo');
const { MessageEmbed } = require('discord.js');
const { stripIndents } = require('common-tags');
const moment = require('moment');
require('moment-duration-format');

const { version } = require('../../../../package.json');

class StatsCommand extends Command {
	constructor() {
		super('stats', {
			aliases: ['stats'],
			description: {
				content: 'Displays statistics about the bot.'
			},
			category: 'util',
			clientPermissions: ['EMBED_LINKS'],
			ratelimit: 2
		});
	}

	exec(message) {
		const embed = new MessageEmbed()
			.setColor(3447003)
			.setDescription(`**${this.client.user.username} Statistics**`)
			.addField('❯ Uptime', moment.duration(this.client.uptime).format('d[d ]h[h ]m[m ]s[s]'), true)
			.addField('❯ Memory Usage', `${Math.round(process.memoryUsage().heapUsed / 1024 / 1024)}MB`, true)
			.addField(
				'❯ General Stats',
				stripIndents`
				• Guilds: ${this.client.guilds.size}
				• Channels: ${this.client.channels.size}
			`,
				true
			)
			.addField('❯ Version', `v${version}`, true)
			.addField('❯ Source Code', '[View Here](https://github.com/Naval-Base/yukikaze)', true)
			.addField(
				'❯ Library',
				'[discord.js](https://discord.js.org)[-akairo](https://github.com/1Computer1/discord-akairo)',
				true
			)
			.setThumbnail(this.client.user.displayAvatarURL())
			.setFooter(`© 2018 ${this.client.users.get(this.client.config.owner).tag}`);

		return message.util.send(embed);
	}
}

module.exports = StatsCommand;
