import { Entity, Column, PrimaryColumn } from 'typeorm';

@Entity('settings')
export class Setting {
	@PrimaryColumn({ type: 'bigint' })
	guild!: string;

	@Column({ type: 'jsonb', default: () => "'{}'::jsonb" })
	settings: any;
}
