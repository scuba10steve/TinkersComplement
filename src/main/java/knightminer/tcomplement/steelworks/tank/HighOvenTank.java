package knightminer.tcomplement.steelworks.tank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import knightminer.tcomplement.library.TCompRegistry;
import knightminer.tcomplement.library.steelworks.IHighOvenFilter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.tconstruct.library.smeltery.ISmelteryTankHandler;
import slimeknights.tconstruct.library.smeltery.SmelteryTank;

/**
 * Same as {@link SmelteryTank}, but filters allowed inputs based if its a valid input from the registry
 */
public class HighOvenTank extends SmelteryTank {

	private FluidStack filter;
	private IHighOvenFilter cache;
	public HighOvenTank(ISmelteryTankHandler parent) {
		super(parent);
	}

	/**
	 * Sets the filter of this tank to the given fluid stack
	 * @param filter  tank filter
	 */
	public void setFilter(@Nullable FluidStack filter) {
		if (filter != null) {
			filter = filter.copy();
		}
		this.filter = filter;
		this.cache = null;
	}

	/**
	 * Gets the fluid in the tank that matches the current filter
	 * @return fluid matching current filter or null if no match
	 */
	public FluidStack getFilterFluid() {
		if(filter == null) {
			return null;
		}
		for(FluidStack liquid : liquids) {
			if(filter.isFluidEqual(liquid)) {
				return liquid;
			}
		}
		return null;
	}

	/**
	 * Checks if the given fluid matches the filter
	 * @param resource  Resource to check
	 * @param update  If true, sets the filter to this fluid if the filter is null
	 * @return  Method of matching the filter. Input means direct match, output means match through mix recipe
	 */
	public FilterMatchType matchesFilter(FluidStack resource, boolean update) {
		// safety check in case drain failed to update this
		if(liquids.isEmpty()) {
			filter = null;
		}

		// if the filter is null, we need to set the filter
		if(filter == null) {
			if (update) {
				setFilter(resource);
			}
			return FilterMatchType.INPUT;
		} else if(filter.isFluidEqual(resource)) {
			return FilterMatchType.INPUT;
		} else if(cache != null && cache.matches(filter, resource)) {
			return FilterMatchType.OUTPUT;
		}

		// so the fluid is not the input, and does not match our cache, so try another recipe from the registry
		IHighOvenFilter recipe = TCompRegistry.getFilter(filter, resource);
		if(recipe != null) {
			// found a match? cache it
			this.cache = recipe;
			return FilterMatchType.OUTPUT;

		}

		// leave cache alone, it was probably fine
		return FilterMatchType.NONE;
	}

	/**
	 * Same as {@link #fill(FluidStack, boolean)}, but does not apply the filter
	 */
	public int fillInternal(@Nonnull FluidStack resource, boolean doFill) {
		return super.fill(resource, doFill);
	}

	/**
	 * Moves the given fluid to the bottom. Same as {@link #moveFluidToBottom(int)}, except moves by type instead of index
	 * @param fluid  fluid to move to the bottom
	 */
	public void moveFluidToBottom(FluidStack fluid) {
		// find the specified fluid
		int i;
		for(i = 0; i < liquids.size(); i++) {
			if(fluid.isFluidEqual(liquids.get(i))) {
				break;
			}
		}
		if(i != 0 && i < liquids.size()) {
			moveFluidToBottom(i);
		}
	}

	/**
	 * Same as {@link #drain(FluidStack, boolean)}, but does not modify the filter
	 */
	public FluidStack drainInternal(@Nonnull FluidStack resource, boolean doDrain) {
		return super.drain(resource, doDrain);
	}

	@Override
	public int fill(@Nonnull FluidStack resource, boolean doFill) {
		if (matchesFilter(resource, doFill) == FilterMatchType.NONE) {
			return 0;
		}
		return fillInternal(resource, doFill);
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		FluidStack drained = drainInternal(resource, doDrain);
		// if draining and that was the last of it, clear the filter
		if(doDrain && drained != null && liquids.isEmpty()) {
			this.setFilter(null);
		}
		return drained;
	}

	private static final String TAG_FILTER = "filter";

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);

		// write filter
		if(filter != null) {
			NBTTagCompound filterTag = new NBTTagCompound();
			filter.writeToNBT(filterTag);
			tag.setTag(TAG_FILTER, filterTag);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);

		// read filter
		if(tag.hasKey(TAG_FILTER, 10)) {
			NBTTagCompound filterTag = tag.getCompoundTag(TAG_FILTER);
			filter = FluidStack.loadFluidStackFromNBT(filterTag);
		}
	}

	public enum FilterMatchType {
		NONE, INPUT, OUTPUT;
	}
}
