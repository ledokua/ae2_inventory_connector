package net.ledok.ae2inventoryconnector.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Loader services the common code needs. One implementation per loader, found with {@link ServiceLoader}. */
public interface Platform {
    Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst()
            .orElseThrow(() -> new IllegalStateException("No " + Platform.class.getName() + " implementation"));

    /** Whether the block exposes an item inventory on {@code side}. */
    boolean hasItemStorage(ServerLevel level, BlockPos pos, BlockState state, Direction side);

    /**
     * Opens the item inventory at {@code pos}. {@code onInvalidated} runs if the loader can tell the inventory went
     * away (Forge capability invalidation); block changes and chunk unloads are reported separately.
     */
    PlatformInventory openItemStorage(ServerLevel level, BlockPos pos, Direction side, Runnable onInvalidated);

    Path configDir();
}
