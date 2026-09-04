const Scheduler = require('./Scheduler');
const { Op } = require('sequelize');

class MuteScheduler extends Scheduler {
	constructor(client, model, { checkRate = 5 * 60 * 1000 } = {}) {
		super(client, model, { checkRate });
	}

	async addMute(mute, reschedule = false) {
		this.client.logger.info('Muted');
		if (reschedule) this.client.logger.info('Rescheduled mute');
		if (!reschedule) mute = await this.model.create(mute, { returning: true });
		if (mute.action_duration.getTime() < (Date.now() + this.checkRate)) {
			this.queueMute(mute);
		}
	}

	async cancelMute(mute) {
		this.client.logger.info('Unmuted');
		const guild = this.client.guilds.get(mute.guild);
		const muteRole = this.client.settings.get(guild, 'muteRole');
		let member;
		try {
			member = await guild.members.fetch(mute.target_id);
		} catch {}
		mute.update({ action_processed: true });
		if (member) {
			try {
				await member.roles.remove(muteRole, `Unmuted automatically based on duration.`);
			} catch {}
		}
		super.cancelSchedule(mute.id);
	}

	deleteMute(id) {
		super.deleteSchedule(id);
	}

	queueMute(mute) {
		this.queuedSchedules.set(mute.id, setTimeout(() => {
			this.cancelMute(mute);
		}, mute.action_duration.getTime() - Date.now()));
	}

	rescheduleMute(mute) {
		this.client.logger.info('Rescheduling mute');
		super.cancelSchedule(mute.id);
		this.addMute(mute, true);
	}

	async init() {
		await this.check();
		this.checkInterval = setInterval(this.check.bind(this), this.checkRate);
	}

	async check() {
		const mutes = await this.model.findAll({
			where: {
				action_duration: { [Op.lt]: new Date(Date.now() + this.checkRate) },
				action_processed: false
			}
		});
		const now = new Date();

		for (const mute of mutes) {
			if (this.queuedSchedules.has(mute.id)) continue;

			if (mute.action_duration < now) {
				this.cancelMute(mute);
			} else {
				this.queueMute(mute);
			}
		}
	}
}

module.exports = MuteScheduler;
