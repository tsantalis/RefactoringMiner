class Scheduler {
	constructor(client, model, { checkRate = 5 * 60 * 1000 } = {}) {
		this.client = client;
		this.model = model;
		this.checkRate = checkRate;
		this.queuedSchedules = new Map();
	}

	addSchedule() {}

	cancelSchedule(id) {
		const schedule = this.queuedSchedules.get(id);
		if (schedule) clearTimeout(schedule);
		return this.queuedSchedules.delete(id);
	}

	deleteSchedule(id) {
		this.cancelSchedule(id);
		return this.model.destroy({ where: { id } });
	}

	queueSchedule() {}

	runSchedule() {}

	init() {}
}

module.exports = Scheduler;
