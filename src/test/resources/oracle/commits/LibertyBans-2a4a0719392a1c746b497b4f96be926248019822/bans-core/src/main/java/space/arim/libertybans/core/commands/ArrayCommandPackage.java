/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.commands;

import java.util.NoSuchElementException;
import java.util.StringJoiner;

public final class ArrayCommandPackage extends CommandPackage {

	private final String[] args;
	
	private transient int position = 0;
	
	public ArrayCommandPackage(String command, String[] args) {
		super(command);
		this.args = args;
	}
	
	@Override
	public String next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		return args[position++];
	}
	
	@Override
	public String peek() {
		return args[position];
	}
	
	@Override
	public boolean hasNext() {
		return args.length > position;
	}
	
	@Override
	public String allRemaining() {
		StringJoiner joiner = new StringJoiner(" ");
		for (int n = position; n < args.length; n++) {
			joiner.add(args[n]);
		}
		position = args.length;
		return joiner.toString();
	}

	@Override
	public CommandPackage copy() {
		ArrayCommandPackage copy = new ArrayCommandPackage(getCommand(), args);
		copy.position = position;
		return copy;
	}

}
