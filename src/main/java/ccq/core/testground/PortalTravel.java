package ccq.core.testground;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

final class PortalTravel {

    private static final ResourceLocation WRENCH = ResourceLocation.parse("create:wrench");

    static void tickPlayer(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre event) {
        tickContact(event.getEntity());
    }

    static void tickEntity(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer)) tickContact(event.getEntity());
    }

    private static void tickContact(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()
                || entity instanceof com.simibubi.create.content.contraptions.AbstractContraptionEntity) return;
        entity.getPersistentData().remove("ccq_testground_travel"); // Retire the old custom timer.
        var box = entity.getBoundingBox().deflate(1.0E-7);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (level.getBlockState(pos).is(TestGroundModule.PORTAL.get())) {
                TestGroundModule.PORTAL.get().contact(entity, pos.immutable());
                return;
            }
        }
    }

    static void activate(PlayerInteractEvent.RightClickBlock event) {
        var stack = event.getItemStack();
        boolean wrench = BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(WRENCH)
                || stack.is(ItemTags.create(ResourceLocation.parse("c:tools/wrench")));
        if (!wrench || !PortalFrame.isFrame(event.getLevel().getBlockState(event.getPos()))) return;
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            PortalFrame frame = PortalFrame.find(event.getLevel(), event.getPos().relative(event.getFace()), axis);
            if (frame != null) {
                if (!event.getLevel().isClientSide) frame.fill(event.getLevel());
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }
        }
    }

    static DimensionTransition destination(ServerLevel level, BlockPos pos, Entity entity) {
        if (!level.getBlockState(pos).is(TestGroundModule.PORTAL.get())) return null;
        PortalFrame source = PortalFrame.find(level, pos, level.getBlockState(pos).getValue(EngineeringPortalBlock.AXIS));
        if (source == null) return null;
        Destination resolved = resolveExit(level, source);
        if (resolved == null) {
            if (entity instanceof ServerPlayer player) player.displayClientMessage(Component.translatable("message.ccq_testground.no_exit"), false);
            return null;
        }
        ServerLevel destination = resolved.level();
        PortalFrame target = resolved.frame();
        Vec3 arrival = Vec3.atBottomCenterOf(target.origin()).add(
                target.right().getStepX() * (target.width() - 1) / 2.0, .05,
                target.right().getStepZ() * (target.width() - 1) / 2.0);
        if (!destination.getWorldBorder().isWithinBounds(BlockPos.containing(arrival))) return null;
        if (!destination.noCollision(entity, entity.getBoundingBox().move(arrival.subtract(entity.position())))) {
            if (entity instanceof ServerPlayer player) player.displayClientMessage(Component.translatable("message.ccq_testground.blocked_exit"), false);
            return null;
        }
        return new DimensionTransition(destination, arrival, Vec3.ZERO, entity.getYRot(), entity.getXRot(),
                DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
    }
    record Destination(ServerLevel level, PortalFrame frame) {}

    static Destination resolveExit(ServerLevel level, PortalFrame source) {
        if (source == null || !source.isComplete(level)) return null;
        PortalLinks links = PortalLinks.get(level.getServer());
        PortalLinks.Endpoint from = new PortalLinks.Endpoint(level.dimension(), source.origin());
        PortalLinks.Endpoint to = links.target(from);
        ServerLevel destination = to == null ? null : level.getServer().getLevel(to.dimension());
        PortalFrame target = null;
        if (destination != null) {
            destination.getChunkAt(to.pos());
            for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
                target = PortalFrame.find(destination, to.pos(), axis);
                if (target != null) break;
            }
            if (target != null) target.fill(destination); // Relight an intact, paired frame.
        }
        if (target == null) {
            var targetDimension = to != null ? to.dimension()
                    : level.dimension().equals(TestGroundModule.DIMENSION) ? Level.OVERWORLD : TestGroundModule.DIMENSION;
            destination = level.getServer().getLevel(targetDimension);
            if (destination != null) target = createExit(destination,
                    to == null ? source.origin() : to.pos(), source);
            if (target == null) return null;
            to = new PortalLinks.Endpoint(destination.dimension(), target.origin());
            links.pair(from, to);
        }
        return new Destination(destination, target);
    }

    private static PortalFrame createExit(ServerLevel level, BlockPos preferred, PortalFrame source) {
        return PortalExitBuilder.create(level, preferred, source);
    }

    static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ccqtest").then(Commands.literal("info").executes(context -> {
            var version=net.neoforged.fml.ModList.get().getModContainerById("ccq_core").orElseThrow().getModInfo().getVersion();
            context.getSource().sendSuccess(() -> Component.translatable("message.ccq_testground.info",
                    version.toString(), TestGroundModule.DIMENSION.location().toString()), false);
            return Command.SINGLE_SUCCESS;
        })).then(Commands.literal("repairbase").requires(source->source.hasPermission(2)).executes(context->{
            var source=context.getSource();
            try {
                var result=EngineeringBaseRepair.repair(source.getLevel(),BlockPos.containing(source.getPosition()));
                if(result.isEmpty()) {
                    source.sendFailure(Component.translatable("message.ccq_testground.base_repair_not_found"));return 0;
                }
                source.sendSuccess(()->Component.translatable("message.ccq_testground.base_repaired",result.getAsInt()),true);
                return Command.SINGLE_SUCCESS;
            } catch(java.io.IOException exception) {
                org.slf4j.LoggerFactory.getLogger("ccq_testground").error("Cannot read engineering base recovery template",exception);
                source.sendFailure(Component.translatable("message.ccq_testground.base_repair_failed"));return 0;
            }
        })));
    }
}
