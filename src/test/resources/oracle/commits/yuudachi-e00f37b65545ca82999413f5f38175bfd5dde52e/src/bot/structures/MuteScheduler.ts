import YukikazeClient from '../client/YukikazeClient';
import { LessThan, Repository } from 'typeorm';
import { Case } from '../models/Cases';

export default class MuteScheduler {
	protected client: YukikazeClient;

	protected repo: Repository<any>;

	protected checkRate: number;

	protected checkInterval!: NodeJS.Timeout;

	protected queuedSchedules = new Map();

	public constructor(client: YukikazeClient, repository: Repository<any>, { checkRate = 5 * 60 * 1000 } = {}) {
		this.client = client;
		this.repo = repository;
		this.checkRate = checkRate;
	}

	public async addMute(mute: any, reschedule = false) {
		this.client.logger.info('Muted');
		if (reschedule) this.client.logger.info('Rescheduled mute');
		if (!reschedule) {
			const casesRepo = this.client.db.getRepository(Case);
			const cs = new Case();
			cs.guild = mute.guild;
			if (mute.message) cs.message = mute.message;
			cs.case_id = mute.case_id;
			cs.target_id = mute.target_id;
			cs.target_tag = mute.target_tag;
			cs.mod_id = mute.mod_id;
			cs.mod_tag = mute.mod_tag;
			cs.action = mute.action;
			cs.action_duration = mute.action_duration;
			cs.action_processed = mute.action_processed;
			cs.reason = mute.reason;
			mute = await casesRepo.save(cs);
		}
		if (mute.action_duration.getTime() < (Date.now() + this.checkRate)) {
			this.queueMute(mute);
		}
	}

	public async cancelMute(mute: any) {
		this.client.logger.info('Unmuted');
		const guild = this.client.guilds.get(mute.guild);
		const muteRole = this.client.settings.get(guild!, 'muteRole', undefined);
		let member;
		try {
			member = await guild!.members.fetch(mute.target_id);
		} catch {}
		const casesRepo = this.client.db.getRepository(Case);
		mute.action_processed = true;
		await casesRepo.save(mute);
		if (member) {
			try {
				await member.roles.remove(muteRole, `Unmuted automatically based on duration.`);
			} catch {}
		}
		const schedule = this.queuedSchedules.get(mute.id);
		if (schedule) clearTimeout(schedule);
		return this.queuedSchedules.delete(mute.id);
	}

	public async deleteMute(mute: any) {
		const schedule = this.queuedSchedules.get(mute.id);
		if (schedule) clearTimeout(schedule);
		this.queuedSchedules.delete(mute.id);
		const casesRepo = this.client.db.getRepository(Case);
		const deleted = await casesRepo.remove(mute);
		return deleted;
	}

	public queueMute(mute: any) {
		this.queuedSchedules.set(mute.id, setTimeout(() => {
			this.cancelMute(mute);
		}, mute.action_duration.getTime() - Date.now()));
	}

	public rescheduleMute(mute: any) {
		this.client.logger.info('Rescheduling mute');
		const schedule = this.queuedSchedules.get(mute.id);
		if (schedule) clearTimeout(schedule);
		this.queuedSchedules.delete(mute.id);
		this.addMute(mute, true);
	}

	public async init() {
		await this._check();
		this.checkInterval = setInterval(this._check.bind(this), this.checkRate);
	}

	private async _check() {
		const casesRepo = this.client.db.getRepository(Case);
		const mutes = await casesRepo.find({ action_duration: LessThan(new Date(Date.now() + this.checkRate)), action_processed: false })
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
