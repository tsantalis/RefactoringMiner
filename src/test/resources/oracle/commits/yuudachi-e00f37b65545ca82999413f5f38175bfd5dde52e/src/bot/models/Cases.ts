import { Entity, Column, PrimaryGeneratedColumn, Index } from 'typeorm';

@Entity('cases')
export class Case {
	@PrimaryGeneratedColumn()
	id!: number;

	@Index()
	@Column({ type: 'bigint' })
	guild!: string;

	@Column({ type: 'bigint', nullable: true })
	message!: string;

	@Index()
	@Column()
	case_id!: number;

	@Column({ nullable: true })
	ref_id!: number;

	@Index()
	@Column({ type: 'bigint' })
	target_id!: string;

	@Column({ type: 'text' })
	target_tag!: string;

	@Column({ type: 'bigint', nullable: true })
	mod_id!: string;

	@Column({ type: 'text', nullable: true })
	mod_tag!: string;

	@Column()
	action!: number;

	@Column({ type: 'text', nullable: true })
	reason!: string;

	@Column({ type: 'timestamptz', nullable: true })
	action_duration!: Date;

	@Index()
	@Column({ default: true })
	action_processed!: boolean;

	@Column({ type: 'timestamptz', default: () => 'NOW()' })
	createdAt!: Date;
}
