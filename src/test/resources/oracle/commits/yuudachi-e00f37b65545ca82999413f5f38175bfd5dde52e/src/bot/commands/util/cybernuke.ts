import { Argument, Command } from 'discord-akairo';
import { Message, GuildMember } from 'discord.js';
import { stripIndents } from 'common-tags';

export default class LaunchCybernukeCommand extends Command {
	public constructor() {
		super('cybernuke', {
			aliases: ['cybernuke', 'launch-cybernuke'],
			description: {
				content: 'Bans all members that have joined recently, with new accounts.',
				usage: '<join> <age>',
				examples: ['10 120']
			},
			category: 'util',
			userPermissions: ['MANAGE_GUILD'],
			clientPermissions: ['BAN_MEMBERS'],
			ratelimit: 2,
			args: [
				{
					id: 'join',
					type: Argument.range('number', 0.1, 120, true),
					prompt: {
						start: (message: Message) => `${message.author}, how old (in minutes) should a member be for the cybernuke to ignore them (server join date)?`,
						retry: (message: Message) => `${message.author}, the minimum is \`0.1\` and the maximum \`120\` minutes.`
					}
				},
				{
					id: 'age',
					type: Argument.range('number', 0.1, Infinity, true),
					prompt: {
						start: (message: Message) => `${message.author}, how old (in minutes) should a member's account be for the cybernuke to ignore them (account age)?`,
						retry: (message: Message) => `${message.author}, the minimum is \`0.1\` minutes.`
					}
				}
			]
		});
	}

	public async exec(message: Message, { join, age }: { join: number, age: number }) {
		await message.util!.send('Calculating targeting parameters for cybernuke...');
		await message.guild.members.fetch();

		const memberCutoff = Date.now() - (join * 60000);
		const ageCutoff = Date.now() - (age * 60000);
		const members = message.guild.members.filter(
			member => member.joinedTimestamp > memberCutoff && member.user.createdTimestamp > ageCutoff
		);

		await message.util!.send(`Cybernuke will strike ${members.size} members; proceed?`);
		let statusMessage: Message;

		const responses = await message.channel.awaitMessages(msg => msg.author.id === message.author.id, {
			max: 1,
			time: 10000
		});

		if (!responses || responses.size !== 1) {
			await message.reply('Cybernuke cancelled.');
			return null;
		}
		const response = responses.first();

		if (/^y(?:e(?:a|s)?)?$/i.test(response!.content)) {
			statusMessage = await response!.reply('Launching cybernuke...') as Message;
		} else {
			await response!.reply('Cybernuke cancelled.');
			return null;
		}

		const fatalities: GuildMember[] = [];
		const survivors: { member: GuildMember, error: Error }[] = [];
		const promises: Promise<any>[] = [];

		for (const member of members.values()) {
			promises.push(
				member.send(stripIndents`
					Sorry, but you've been automatically targetted by the cybernuke in the "${message.guild.name}" server.
					This means that you have been banned, likely in the case of a server raid.
					Please contact them if you believe this ban to be in error.
				`).catch(this.client.logger.error)
					.then(() => member.ban())
					.then(() => {
						fatalities.push(member);
					})
					.catch(err => {
						this.client.logger.error(err);
						survivors.push({
							member,
							error: err
						});
					})
					.then(async () => {
						if (members.size <= 5) return;
						if (promises.length % 5 === 0) {
							await statusMessage.edit(`Launching cyber nuke (${Math.round(promises.length / members.size * 100)}%)...`);
						}
					})
			);
		}

		await Promise.all(promises);
		await statusMessage.edit('Cybernuke impact confirmed. Casuality report incoming...');
		await response!.reply(stripIndents`
			__**Fatalities:**__

			${fatalities.length > 0 ? stripIndents`
				${fatalities.length} confirmed KIA.
				${fatalities.map(fat => `**-** ${fat.displayName} (${fat.id})`).join('\n')}
			` : 'None'}
			${survivors.length > 0 ? stripIndents`
				__**Survivors**__
				${survivors.length} left standing.
				${survivors.map(srv => `**-** ${srv.member.displayName} (${srv.member.id}): \`${srv.error}\``).join('\n')}
			` : ''}
		`, { split: true });

		return null;
	}
}
