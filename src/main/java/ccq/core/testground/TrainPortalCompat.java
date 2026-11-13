package ccq.core.testground;

import com.simibubi.create.api.contraption.train.PortalTrackProvider;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/** Use Create's native interdimensional track graph and carriage transport. */
final class TrainPortalCompat {
    static void register() {
        PortalTrackProvider.REGISTRY.register(TestGroundModule.PORTAL.get(), TrainPortalCompat::findExit);
    }

    static PortalTrackProvider.Exit findExit(ServerLevel level, BlockFace trackFace) {
        var portalPos = trackFace.getConnectedPos();
        var state = level.getBlockState(portalPos);
        if (!state.is(TestGroundModule.PORTAL.get())) return null;
        var axis = state.getValue(EngineeringPortalBlock.AXIS);
        Direction inward = trackFace.getFace();
        if (!inward.getAxis().isHorizontal() || inward.getAxis() == axis) return null;
        var source = PortalFrame.find(level, portalPos, axis);
        var destination = PortalTravel.resolveExit(level, source);
        if (destination == null) return null;
        var target = destination.frame();
        int column = axis == Direction.Axis.X ? portalPos.getX()-source.origin().getX()
                : portalPos.getZ()-source.origin().getZ();
        int height = portalPos.getY()-source.origin().getY();
        if (column < 0 || column >= target.width() || height < 0 || height >= target.height()) return null;
        Direction outward = inward.getAxis() == target.axis() ? inward.getClockWise() : inward;
        var exitPortalPos = target.at(column, height);
        return new PortalTrackProvider.Exit(destination.level(),
                new BlockFace(exitPortalPos.relative(outward), outward.getOpposite()));
    }
}
