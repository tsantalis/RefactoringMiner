import { ConnectionManager } from 'typeorm';
import { Setting } from '../models/Settings';
import { Tag } from '../models/Tags';
import { RoleState } from '../models/RoleStates';
import { Case } from '../models/Cases';
import { Reminder } from '../models/Reminders';

const connectionManager = new ConnectionManager();
connectionManager.create({
	name: 'yukikaze',
	type: 'postgres',
	url: process.env.DB,
	synchronize: true,
	entities: [Setting, Tag, RoleState, Case, Reminder]
});

export default connectionManager;
