const { Listener } = require('discord-akairo');

class GuildMemberAddListener extends Listener {
	constructor() {
		super('guildMemberAdd', {
			emitter: 'client',
			event: 'guildMemberAdd',
			category: 'client'
		});
	}

	async exec(member) {
		const roleState = this.client.settings.get(member.guild, 'roleState');
		if (roleState) {
			const user = await this.client.db.models.role_states.findOne({ where: { guild: member.guild.id, user: member.id } });
			try {
				if (user && member.roles) await member.roles.add(user.roles, 'Automatic role state');
			} catch {}
		}
	}
}

module.exports = GuildMemberAddListener;
