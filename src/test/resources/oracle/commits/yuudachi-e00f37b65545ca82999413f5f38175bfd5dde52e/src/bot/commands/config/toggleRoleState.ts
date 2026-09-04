import { Command } from 'discord-akairo';
import { Message } from 'discord.js';
import { RoleState } from '../../models/RoleStates';

export default class ToggleRoleStateCommand extends Command {
	public constructor() {
		super('toggle-role-state', {
			aliases: ['role-state'],
			description: {
				content: 'Toggle role state on the server.'
			},
			category: 'config',
			channel: 'guild',
			userPermissions: ['MANAGE_GUILD'],
			ratelimit: 2
		});
	}

	public async exec(message: Message) {
		const roleState = this.client.settings.get(message.guild, 'roleState', undefined);
		if (roleState) {
			this.client.settings.set(message.guild, 'roleState', false);
			const userRepo = this.client.db.getRepository(RoleState);
			const users = await userRepo.find({ guild: message.guild.id });
			for (const user of users) userRepo.remove(user);

			return message.util!.reply('successfully removed all records!');
		}
		this.client.settings.set(message.guild, 'roleState', true);
		const members = await message.guild.members.fetch();
		const records: RoleState[] = [];
		for (const member of members.values()) {
			const rs = new RoleState();
			rs.guild = message.guild.id;
			rs.user = member.id;
			rs.roles = member.roles.filter(role => role.id !== message.guild.id).map(role => role.id);
			records.push(rs);
		}
		const userRepo = this.client.db.getRepository(RoleState);
		await userRepo.save(records);

		return message.util!.reply('successfully inserted all the records!');
	}
}
