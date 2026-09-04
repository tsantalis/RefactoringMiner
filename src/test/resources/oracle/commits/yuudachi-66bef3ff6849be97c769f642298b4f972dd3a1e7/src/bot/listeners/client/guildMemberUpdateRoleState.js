const { Listener } = require('discord-akairo');

class GuildMemberUpdateRoleStateListener extends Listener {
	constructor() {
		super('guildMemberUpdateRoleState', {
			emitter: 'client',
			event: 'guildMemberUpdate',
			category: 'client'
		});
	}

	async exec(_, newMember) {
		const roleState = this.client.settings.get(newMember.guild, 'roleState');
		if (roleState) {
			await newMember.guild.members.fetch(newMember.id);
			if (newMember.roles) {
				const roles = newMember.roles.filter(role => role.id !== newMember.guild.id).map(role => role.id);
				if (roles.length) {
					await this.client.db.models.role_states.upsert({ guild: newMember.guild.id, user: newMember.id, roles });
				} else {
					await this.client.db.models.role_states.destroy({ where: { guild: newMember.guild.id, user: newMember.id } });
				}
			}
		}
	}
}

module.exports = GuildMemberUpdateRoleStateListener;
