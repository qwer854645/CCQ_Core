package ccq.core.testground.mixin;

import ccq.core.testground.EngineeringBaseCommands;
import ccq.core.testground.EngineeringStructures;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlaceCommand.class)
public abstract class PlaceCommandMixin {
    @Inject(method="placeStructure",at=@At("HEAD"),cancellable=true)
    private static void ccq$placeWholeBase(CommandSourceStack source,Holder.Reference<Structure> structure,BlockPos pos,CallbackInfoReturnable<Integer> result)throws CommandSyntaxException {
        if(structure.value() instanceof EngineeringStructures.Facility facility&&EngineeringBaseCommands.isBunkerTemplate(facility.templateId())) {
            int placed=EngineeringBaseCommands.placeBunker(source,pos,Rotation.NONE,Mirror.NONE,1,0);
            source.sendSuccess(()->Component.translatable("commands.place.structure.success",structure.key().location().toString(),pos.getX(),pos.getY(),pos.getZ()),true);
            result.setReturnValue(placed);return;
        }
        if(!(structure.value() instanceof EngineeringStructures.Base base))return;
        int placed=EngineeringBaseCommands.place(source,pos,base.ruined(),Rotation.NONE,Mirror.NONE,1,0);
        source.sendSuccess(()->Component.translatable("commands.place.structure.success",structure.key().location().toString(),pos.getX(),pos.getY(),pos.getZ()),true);
        result.setReturnValue(placed);
    }

    @Inject(method="placeTemplate",at=@At("HEAD"),cancellable=true)
    private static void ccq$placeBaseTemplate(CommandSourceStack source,ResourceLocation template,BlockPos pos,Rotation rotation,Mirror mirror,float integrity,int seed,CallbackInfoReturnable<Integer> result)throws CommandSyntaxException {
        if(EngineeringBaseCommands.isBunkerTemplate(template)) {
            int placed=EngineeringBaseCommands.placeBunker(source,pos,rotation,mirror,integrity,seed);
            source.sendSuccess(()->Component.translatable("commands.place.template.success",Component.translationArg(template),pos.getX(),pos.getY(),pos.getZ()),true);
            result.setReturnValue(placed);return;
        }
        if(!EngineeringBaseCommands.isBaseTemplate(template))return;
        int placed=EngineeringBaseCommands.place(source,pos,template.getPath().endsWith("_ruined"),rotation,mirror,integrity,seed);
        source.sendSuccess(()->Component.translatable("commands.place.template.success",Component.translationArg(template),pos.getX(),pos.getY(),pos.getZ()),true);
        result.setReturnValue(placed);
    }
}
