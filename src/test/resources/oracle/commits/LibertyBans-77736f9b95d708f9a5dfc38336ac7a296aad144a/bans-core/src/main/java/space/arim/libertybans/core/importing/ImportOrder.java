/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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

import space.arim.libertybans.core.punish.Enaction;

import java.util.Objects;
import java.util.Set;

public class ImportOrder {

	private final Enaction.OrderDetails enactionOrder;
	private final Set<NameAddressRecord> nameAddressRecords;

	public ImportOrder(Enaction.OrderDetails enactionOrder, Set<NameAddressRecord> nameAddressRecords) {
		this.enactionOrder = Objects.requireNonNull(enactionOrder);
		this.nameAddressRecords = Objects.requireNonNull(nameAddressRecords);
	}

	public Enaction.OrderDetails enactionOrder() {
		return enactionOrder;
	}

	public Set<NameAddressRecord> nameAddressRecords() {
		return nameAddressRecords;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ImportOrder that = (ImportOrder) o;
		return enactionOrder.equals(that.enactionOrder) && nameAddressRecords.equals(that.nameAddressRecords);
	}

	@Override
	public int hashCode() {
		int result = enactionOrder.hashCode();
		result = 31 * result + nameAddressRecords.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "ImportOrder{" +
				"enactionOrder=" + enactionOrder +
				", nameAddressRecords=" + nameAddressRecords +
				'}';
	}
}
