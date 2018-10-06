package assets.recipehandler;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

import java.lang.reflect.Method;

public abstract class ModCompat implements ICompat{
    protected Method invGetter;
    private final int slotIndex;
    private final String name;
    public ModCompat(int out, String path){
        this.slotIndex = out;
        this.name = path;
    }

    @Override
    public final String getContainer() {
        return name;
    }

    @Override
    public Slot getResultSlot(Container container){
        return container.getSlot(slotIndex);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        return o instanceof ICompat && getContainer().equals(((ICompat) o).getContainer());
    }

    @Override
    public final int hashCode() {
        return getContainer().hashCode();
    }
}
