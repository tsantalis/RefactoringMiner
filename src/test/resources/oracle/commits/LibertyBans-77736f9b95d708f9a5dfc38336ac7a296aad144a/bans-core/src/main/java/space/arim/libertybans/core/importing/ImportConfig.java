/*
 * LibertyBans
 * Copyright © 2020 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.core.importing;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.IntegerRange;
import space.arim.dazzleconf.annote.SubSection;

@ConfHeader({
		"Settings for importing from other plugins",
		"Most users should not have to touch this.",
		"",
		"In order to perform an import, run /libertybans import <source>.",
		"Available sources are 'advancedban', 'litebans', and 'vanilla'.",
		"",
		"Importing from vanilla is only possible on Bukkit.",
		"Essentials users: Note that Essentials uses the vanilla ban system.",
		"",
		"--- NOTICE ---",
		"You MUST backup your data before performing the import.",
		"LibertyBans will never delete your data, but taking a backup is the best practice",
		"whenever you are performing any large transfer of data. The possibility of failure",
		"will never be zero.",
		"",
		"--- WARNING when importing from AdvancedBan and Vanilla ---",
		"AdvancedBan/Vanilla does not store the UUID of the operator who made a punishment.",
		"To work around this, LibertyBans will attempt to lookup the operator UUID. However,",
		"if you have the Mojang API configured as the sole web api resolver in the config.yml,",
		"you may spam the Mojang API with requests, and your server might be rate limited.",
		"If you are rate limited, your server cannot lookup the UUIDs of new players joining.",
		"To prevent this, add another service to your web api resolvers before importing."})
public interface ImportConfig {

	@ConfKey("retrieval-size")
	@ConfComments({
			"How many punishments to retrieve at once from the import source.",
			"You may be surprised to find out your server is capable of retrieving hundreds," +
			"even thousands of punishments, into memory without much trouble. However," +
			"this is set to 250 as a reasonable conservative estimate."})
	@ConfDefault.DefaultInteger(250)
	@IntegerRange(min = 1)
	int retrievalSize();

	@ConfKey("advancedban")
	@SubSection
	AdvancedBanSettings advancedBan();

	@ConfHeader({
			"Importing from AdvancedBan",
			"",
			"The defaults here match the default settings in AdvancedBan.",
			"If you use AdvancedBan's default storage, you do not need to change anything here.",
			"",
			"--- Using AdvancedBan's MySQL/MariaDB Storage ---",
			"However, if you use MySQL with AdvancedBan, you will need to change a few things.",
			"The jdbc-url should be set to 'jdbc:mariadb://<host>:<port>/<database>' with <host>,",
			"<port>, and <database> replaced with the correct values. The username and password",
			"ought to be the same username and password you used with AdvancedBan.",
			"",
			"For example: You used AdvancedBan with MySQL. Your host is localhost, your port",
			"is 3306, and your database name is 'bans'. You use the username 'advancedban' and",
			"password 'password'. You should set the jdbc-url option to",
			"'jdbc:mysql://localhost:3306/bans', the username to 'advancedban', and the password to 'password'"})
	interface AdvancedBanSettings {

		@ConfKey("jdbc-url")
		@ConfDefault.DefaultString("jdbc:hsqldb:file:plugins/AdvancedBan/data/storage;hsqldb.lock_file=false")
		String jdbcUrl();

		@ConfDefault.DefaultString("SA")
		String username();

		@ConfDefault.DefaultString("")
		String password();

		default ConnectionSource toConnectionSource() {
			return new JdbcDetails(jdbcUrl(), username(), password());
		}
	}

	@ConfKey("litebans")
	@SubSection
	LiteBansSettings litebans();

	@ConfHeader({
			"Importing from LiteBans",
			"",
			"This is no easy task. The main problem is that LiteBans is a closed-source black box,",
			"such that no one except its author knows how the plugin works internally.",
			"",
			"However, it is still possible to import from LiteBans to LibertyBans, and this is achieved",
			"primarily from inspecting the generated SQL schema.",
			"",
			"--- Config Options Explained ---",
			"The jdbc-url depends on the storage mode you are using LiteBans with.",
			"- Using H2, it should be 'jdbc:h2:plugins/LiteBans/litebans.mv.db'",
			"- Using MySQL/MariaDB, it should be 'jdbc:mariadb://<host>:<port>/<database>' with <host>,",
			"<port>, and <database> replaced with the correct values.",
			"- Using PostgreSQL, it should be 'jdbc:postgresql://<host>:<port>/<database>' with <host>,",
			"<port>, and <database> replaced with the correct values.",
			"",
			"If you configured a username and password for LiteBans, you should enter the same",
			"username and password in this section.",
			"",
			"--- JDBC Driver Availability: H2 and Postgres Only ---",
			"One issue you may run into is the availability of your JDBC driver. LiteBans supports",
			"H2, MySQL/MariaDB, and Postgres. While LibertyBans can import from all of these sources,",
			"LibertyBans only includes the MySQL/MariaDB driver for its own purposes.",
			"",
			"Therefore, if using H2 or Postgres, you will need to attach the relevant JDBC driver to the",
			"classpath of your server. This is done by downloading the driver jar, and starting",
			"your server with the -cp option. For more information, join the discord for support",
			"or see https://stackoverflow.com/questions/8084926/including-jar-files-in-class-path",})
	interface LiteBansSettings {

		@ConfKey("jdbc-url")
		@ConfComments("The default value here is set for H2.")
		@ConfDefault.DefaultString("jdbc:h2:plugins/LiteBans/litebans.mv.db")
		String jdbcUrl();

		@ConfDefault.DefaultString("sa")
		String username();

		@ConfDefault.DefaultString("")
		String password();

		@ConfKey("table-prefix")
		@ConfComments("The same table prefix you used with LiteBans")
		@ConfDefault.DefaultString("litebans_")
		String tablePrefix();

		default ConnectionSource toConnectionSource() {
			return new JdbcDetails(jdbcUrl(), username(), password());
		}
	}

}
