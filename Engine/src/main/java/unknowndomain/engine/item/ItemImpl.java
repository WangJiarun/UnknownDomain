package unknowndomain.engine.item;

import unknowndomain.engine.block.RayTraceBlockHit;
import unknowndomain.engine.component.Component;
import unknowndomain.engine.entity.Entity;
import unknowndomain.engine.player.Player;
import unknowndomain.engine.registry.RegistryEntry;
import unknowndomain.engine.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class ItemImpl extends RegistryEntry.Impl<Item> implements Item {
    private ItemPrototype.UseBlockBehavior useBlockBehavior;
    private ItemPrototype.HitBlockBehavior hitBlockBehavior;
    private ItemPrototype.UseBehavior useBehavior;

    ItemImpl(ItemPrototype.UseBlockBehavior useBlockBehavior, ItemPrototype.HitBlockBehavior hitBlockBehavior, ItemPrototype.UseBehavior useBehavior) {
        this.useBlockBehavior = useBlockBehavior;
        this.hitBlockBehavior = hitBlockBehavior;
        this.useBehavior = useBehavior;
    }

    @Override
    public void onUseStart(World world, Entity entity, Item item) {
        useBehavior.onUseStart(world, entity, item);
    }

    @Override
    public boolean onUsing(World world, Player player, Item item, int tickElapsed) {
        return useBehavior.onUsing(world, player, item, tickElapsed);
    }

    @Override
    public void onUseStop(World world, Player player, Item item, int tickElapsed) {
        useBehavior.onUseStop(world, player, item, tickElapsed);
    }

    @Override
    public void onHit(World world, Player player, Item item, RayTraceBlockHit hit) {
        hitBlockBehavior.onHit(world, player, item, hit);
    }


    @Override
    public void onUseBlockStart(World world, Entity entity, Item item, RayTraceBlockHit hit) {
        useBlockBehavior.onUseBlockStart(world, entity, item, hit);
    }

    @Override
    public boolean onUsingBlock(Player player, Item item, RayTraceBlockHit hit, int tickElapsed) {
        return useBlockBehavior.onUsingBlock(player, item, hit, tickElapsed);
    }

    @Override
    public void onUseBlockStop(Player player, Item item, RayTraceBlockHit hit, int tickElapsed) {
        useBlockBehavior.onUseBlockStop(player, item, hit, tickElapsed);
    }


    @Nullable
    @Override
    public <T extends Component> T getComponent(@Nonnull Class<T> type) {
        return null;
    }

    @Override
    public <T extends Component> boolean hasComponent(@Nonnull Class<T> type) {
        return false;
    }
}
