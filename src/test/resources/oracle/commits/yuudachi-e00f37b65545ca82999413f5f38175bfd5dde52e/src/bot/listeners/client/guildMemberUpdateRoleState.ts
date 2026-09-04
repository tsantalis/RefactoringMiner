import { Listener } from 'discord-akairo';
import { GuildMember } from 'discord.js';
import { RoleState } from '../../models/RoleStates';

export default class GuildMemberUpdateRoleStateListener extends Listener {
	public constructor() {
		super('guildMemberUpdateRoleState', {
			emitter: 'client',
			event: 'guildMemberUpdate',
			category: 'client'
		});
	}

	public async exec(_: GuildMember, newMember: GuildMember) {
		const roleState = this.client.settings.get(newMember.guild, 'roleState', undefined);
		if (roleState) {
			await newMember.guild.members.fetch(newMember.id);
			if (newMember.roles) {
				const roleStateRepo = this.client.db.getRepository(RoleState);
				const roles = newMember.roles.filter(role => role.id !== newMember.guild.id).map(role => role.id);
				if (roles.length) {
					await roleStateRepo.createQueryBuilder()
						.insert()
						.into(RoleState)
						.values({ guild: newMember.guild.id, user: newMember.id, roles })
						.onConflict(`("guild", "user") DO UPDATE SET "roles" = :roles`)
						.setParameter('roles', roles)
						.execute();
				} else {
					const user = await roleStateRepo.findOne({ guild: newMember.guild.id, user: newMember.id });
					if (user) await roleStateRepo.remove(user);
				}
			}
		}
	}
}
